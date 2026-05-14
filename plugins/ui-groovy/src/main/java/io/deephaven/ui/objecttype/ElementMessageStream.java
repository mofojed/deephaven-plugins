package io.deephaven.ui.objecttype;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import groovy.lang.Closure;
import io.deephaven.engine.context.ExecutionContext;
import io.deephaven.ui.element.Element;
import io.deephaven.ui.element.RenderedNode;
import io.deephaven.ui.event.EventContext;
import io.deephaven.ui.jsonrpc.JsonRpcDispatcher;
import io.deephaven.ui.render.ExportedRenderState;
import io.deephaven.ui.render.NodeEncoder;
import io.deephaven.ui.render.RenderContext;
import io.deephaven.ui.render.Renderer;
import io.deephaven.ui.render.RootRenderContext;
import io.deephaven.ui.render.UiCallable;
import io.deephaven.util.SafeCloseable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * The heart of the Groovy plugin. Direct port of Python's {@code ElementMessageStream}:
 * <ul>
 *   <li>Owns a per-stream render loop on a single-thread executor (matches Python's
 *       per-stream {@code _render_lock} serialization).</li>
 *   <li>Dispatches inbound JSON-RPC (setState / setUrlState / callCallable / closeCallable) onto
 *       the render thread.</li>
 *   <li>Emits {@code documentPatched} notifications with RFC 6902 patches diffed against the
 *       previous document, byte-compatible with the Python plugin so the existing JS plugin is
 *       unchanged.</li>
 * </ul>
 *
 * <p>Implements {@link ClientConnection} on the inbound side so an SPI adapter can hand us bytes;
 * dispatches to an outbound {@link ClientConnection} on the way out.
 */
public final class ElementMessageStream implements ClientConnection, RootRenderContext {

    private final Element element;
    private final ClientConnection connection;
    private final ObjectMapper mapper;
    private final JsonRpcDispatcher dispatcher;
    private final NodeEncoder encoder;

    private final ConcurrentLinkedQueue<Runnable> updateQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> callableQueue = new ConcurrentLinkedQueue<>();

    private final Map<String, Object> renderCallables = new HashMap<>();
    private final Map<String, Object> tempCallables = new HashMap<>();
    private int nextTempCallableId;

    private final ExecutorService executor;
    private final Object renderLock = new Object();
    private final AtomicReference<RenderState> renderState = new AtomicReference<>(RenderState.IDLE);
    private volatile Thread renderThread;
    private volatile boolean dirty;
    private volatile boolean closed;

    private final RenderContext rootContext;
    private final Renderer renderer;
    private final EventContext eventContext;
    /**
     * Captured at construction (when the server calls into us with a proper context attached) and
     * re-opened on the render thread so live-data hooks see a real UpdateGraph, not the poisoned
     * default attached to executor threads. Mirrors Python's {@code self._exec_context}.
     */
    private final ExecutionContext capturedExecutionContext;
    private JsonNode lastDocument;
    private Map<String, List<String>> queryParams = new HashMap<>();

    private enum RenderState { IDLE, RENDERING, QUEUED }

    public ElementMessageStream(Element element, ClientConnection connection) {
        this(element, connection, new ObjectMapper(), Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "deephaven-ui-render");
            t.setDaemon(true);
            return t;
        }));
    }

    public ElementMessageStream(Element element, ClientConnection connection,
                                ObjectMapper mapper, ExecutorService executor) {
        this.element = element;
        this.connection = connection;
        this.mapper = mapper;
        this.executor = executor;
        this.encoder = new NodeEncoder();
        this.dispatcher = new JsonRpcDispatcher(mapper);
        this.rootContext = new RenderContext(this);
        this.renderer = new Renderer(rootContext);
        this.eventContext = new EventContext(this::sendEvent);
        this.lastDocument = mapper.createObjectNode();
        ExecutionContext captured;
        try {
            captured = ExecutionContext.getContext();
        } catch (Throwable t) {
            captured = null;
        }
        this.capturedExecutionContext = captured;
        registerDispatcherMethods();
    }

    /** Kick off the stream. The Python side sends an empty payload first so the client posts its initial state. */
    public void start() {
        connection.onData(new byte[0], new Object[0]);
    }

    // ─── Inbound: from the client ────────────────────────────────────────────────────────────

    @Override
    public void onData(byte[] payload, Object[] references) {
        final String decoded = new String(payload, StandardCharsets.UTF_8);
        onQueueRender(() -> {
            String response = dispatcher.handle(decoded);
            if (response != null) {
                connection.onData(response.getBytes(StandardCharsets.UTF_8), new Object[0]);
            }
        });
    }

    @Override
    public void onClose() {
        if (closed) {
            return;
        }
        closed = true;
        if (rootContext != null) {
            try {
                rootContext.unmount();
            } catch (RuntimeException ignored) {
            }
        }
        executor.shutdown();
    }

    // ─── RootRenderContext ───────────────────────────────────────────────────────────────────

    @Override
    public void onChange(Runnable stateUpdate) {
        Thread current = Thread.currentThread();
        if (current != renderThread) {
            throw new IllegalStateException(
                    "State update called from non-render thread '" + current.getName() +
                            "'. Use the render queue to schedule updates from background threads.");
        }
        updateQueue.add(stateUpdate);
        markDirty();
    }

    @Override
    public void onQueueRender(Runnable callable) {
        callableQueue.add(callable);
        queueRender();
    }

    @Override
    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }

    @Override
    public void setQueryParams(Map<String, List<String>> queryParams) {
        this.queryParams = queryParams == null ? new HashMap<>() : queryParams;
    }

    // ─── Render loop ─────────────────────────────────────────────────────────────────────────

    private void markDirty() {
        if (dirty) {
            return;
        }
        dirty = true;
        queueRender();
    }

    private void queueRender() {
        synchronized (renderLock) {
            if (renderState.get() == RenderState.IDLE) {
                renderState.set(RenderState.QUEUED);
                executor.submit(this::processCallableQueue);
            }
        }
    }

    private void processCallableQueue() {
        try (SafeCloseable execScope = capturedExecutionContext == null
                ? () -> {}
                : capturedExecutionContext.open();
             AutoCloseable eventScope = eventContext.open()) {
            synchronized (renderLock) {
                renderThread = Thread.currentThread();
                renderState.set(RenderState.RENDERING);
            }

            Runnable item;
            while ((item = callableQueue.poll()) != null) {
                try {
                    item.run();
                } catch (RuntimeException e) {
                    // Continue draining; failures inside individual callables should not stop the loop.
                }
            }

            if (dirty) {
                render();
            }

            synchronized (renderLock) {
                renderThread = null;
                if (!callableQueue.isEmpty() || dirty) {
                    renderState.set(RenderState.QUEUED);
                    executor.submit(this::processCallableQueue);
                } else {
                    renderState.set(RenderState.IDLE);
                }
            }
        } catch (RuntimeException e) {
            // Catastrophic failure — close the connection so the client doesn't hang.
            try {
                connection.onClose();
            } catch (RuntimeException ignored) {
            }
        } catch (Exception e) {
            // AutoCloseable.close() may throw checked Exception; treat the same way.
            try {
                connection.onClose();
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void render() {
        Runnable update;
        while ((update = updateQueue.poll()) != null) {
            try {
                update.run();
            } catch (RuntimeException ignored) {
            }
        }
        dirty = false;

        try {
            RenderedNode node = renderer.render(element);
            ExportedRenderState state = rootContext.exportState();
            sendDocumentPatch(node, state);
        } catch (RuntimeException e) {
            sendDocumentError(e);
        }
    }

    // ─── Outbound: to the client ─────────────────────────────────────────────────────────────

    private void sendDocumentPatch(RenderedNode root, ExportedRenderState state) {
        if (closed) {
            return;
        }
        NodeEncoder.Result result = encoder.encodeNode(root);
        JsonNode newDocument = mapper.valueToTree(result.encodedNode);
        JsonNode patch = JsonDiff.asJson(lastDocument, newDocument);
        lastDocument = newDocument;

        // Refresh the rendered callables map: only callbacks present in this render are dispatchable.
        renderCallables.clear();
        renderCallables.putAll(result.liveCallables);

        String stateJson;
        try {
            stateJson = mapper.writeValueAsString(state.asMap());
        } catch (Exception e) {
            stateJson = "{}";
        }
        String payload = dispatcher.notification("documentPatched", patch, stateJson);
        Object[] newRefs = result.newObjects.toArray(new Object[0]);
        connection.onData(payload.getBytes(StandardCharsets.UTF_8), newRefs);
    }

    /** Emit a client-side event ({@code event} notification). Called by the {@link EventContext}. */
    private void sendEvent(String name, Map<String, Object> params) {
        if (closed) {
            return;
        }
        Object encodedParams = serializeResultCallables(params == null ? Collections.emptyMap() : params);
        String encodedJson;
        try {
            encodedJson = mapper.writeValueAsString(encodedParams);
        } catch (Exception e) {
            encodedJson = "{}";
        }
        String payload = dispatcher.notification("event", name, encodedJson);
        connection.onData(payload.getBytes(StandardCharsets.UTF_8), new Object[0]);
    }

    private void sendDocumentError(Throwable error) {
        if (closed) {
            return;
        }
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("message", error.getMessage() == null ? "" : error.getMessage());
        errorBody.put("name", error.getClass().getSimpleName());
        errorBody.put("stack", stackToString(error));
        errorBody.put("code", ErrorCode.DOCUMENT_ERROR.value());
        String errorJson;
        try {
            errorJson = mapper.writeValueAsString(errorBody);
        } catch (Exception e) {
            errorJson = "{\"message\":\"\"}";
        }
        String payload = dispatcher.notification("documentError", errorJson);
        connection.onData(payload.getBytes(StandardCharsets.UTF_8), new Object[0]);
    }

    private static String stackToString(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    // ─── Dispatcher ──────────────────────────────────────────────────────────────────────────

    private void registerDispatcherMethods() {
        dispatcher.register("setState", params -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> state = params.isEmpty() ? Collections.emptyMap()
                    : (Map<String, Object>) params.get(0);
            rootContext.importState(state);
            markDirty();
            return null;
        });
        dispatcher.register("setUrlState", params -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> urlState = params.isEmpty() ? Collections.emptyMap()
                    : (Map<String, Object>) params.get(0);
            Object qp = urlState.get("__queryParams");
            if (qp instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> casted = (Map<String, List<String>>) qp;
                setQueryParams(casted);
            }
            markDirty();
            return null;
        });
        dispatcher.register("callCallable", params -> callCallable(params));
        dispatcher.register("closeCallable", params -> {
            if (!params.isEmpty()) {
                tempCallables.remove(String.valueOf(params.get(0)));
            }
            return null;
        });
    }

    private Object callCallable(List<Object> params) {
        if (params.size() < 1) {
            return null;
        }
        String id = String.valueOf(params.get(0));
        @SuppressWarnings("unchecked")
        List<Object> args = params.size() > 1 && params.get(1) instanceof List
                ? (List<Object>) params.get(1) : Collections.emptyList();

        Object fn = renderCallables.get(id);
        if (fn == null) {
            fn = tempCallables.get(id);
        }
        if (fn == null) {
            return null;
        }
        Object result;
        try {
            result = invokeCallable(fn, args);
        } catch (RuntimeException e) {
            return null;
        }
        try {
            return mapper.writeValueAsString(serializeResultCallables(result));
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("serialization_error", "Cannot serialize callable " + id + " result");
            return err;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object invokeCallable(Object fn, List<Object> args) {
        if (fn instanceof UiCallable) {
            return ((UiCallable) fn).call(args.toArray());
        }
        if (fn instanceof Closure) {
            return ((Closure) fn).call(args.toArray());
        }
        if (fn instanceof Runnable && args.isEmpty()) {
            ((Runnable) fn).run();
            return null;
        }
        if (fn instanceof java.util.function.Consumer && args.size() == 1) {
            ((java.util.function.Consumer) fn).accept(args.get(0));
            return null;
        }
        if (fn instanceof Function && args.size() == 1) {
            return ((Function) fn).apply(args.get(0));
        }
        if (fn instanceof java.util.function.Supplier && args.isEmpty()) {
            return ((java.util.function.Supplier) fn).get();
        }
        if (fn instanceof java.util.function.BiConsumer && args.size() == 2) {
            ((java.util.function.BiConsumer) fn).accept(args.get(0), args.get(1));
            return null;
        }
        if (fn instanceof java.util.function.BiFunction && args.size() == 2) {
            return ((java.util.function.BiFunction) fn).apply(args.get(0), args.get(1));
        }
        return null;
    }

    private Object serializeResultCallables(Object value) {
        if (NodeEncoder.isCallable(value)) {
            String id = "tempCb" + nextTempCallableId++;
            tempCallables.put(id, value);
            Map<String, Object> out = new HashMap<>();
            out.put(NodeEncoder.CALLABLE_KEY, id);
            return out;
        }
        if (value instanceof Map) {
            Map<String, Object> in = (Map<String, Object>) value;
            Map<String, Object> out = new LinkedHashMap<>(in.size());
            for (Map.Entry<String, Object> e : in.entrySet()) {
                out.put(e.getKey(), serializeResultCallables(e.getValue()));
            }
            return out;
        }
        if (value instanceof List) {
            List<Object> in = (List<Object>) value;
            List<Object> out = new ArrayList<>(in.size());
            for (Object v : in) {
                out.add(serializeResultCallables(v));
            }
            return out;
        }
        return value;
    }
}
