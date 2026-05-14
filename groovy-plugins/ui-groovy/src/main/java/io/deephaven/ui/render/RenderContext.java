package io.deephaven.ui.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * React-style per-component render state. Direct port of Python's {@code RenderContext}: hook
 * slots, child contexts, effects, unmount listeners. The current context is exposed via a
 * {@link ThreadLocal} so {@link io.deephaven.ui.hook.Hooks} can find it implicitly.
 *
 * <p>Live-data liveness-scope plumbing from the Python version is intentionally omitted in this
 * MVP — see plan {@code Phase 2}.
 */
public final class RenderContext {

    private static final ThreadLocal<RenderContext> CURRENT = new ThreadLocal<>();

    private static final int READY_TO_OPEN = -2;
    private static final int OPENED_AND_UNUSED = -1;

    private final RootRenderContext root;
    private final Map<Integer, Object> state = new LinkedHashMap<>();
    private final Map<String, RenderContext> childrenContext = new LinkedHashMap<>();

    private int hookIndex = READY_TO_OPEN;
    private int hookCount = -1;
    private boolean isMounted = true;

    private final List<Effect> collectedEffects = new ArrayList<>();
    private final List<Runnable> collectedUnmountListeners = new ArrayList<>();
    private final List<String> collectedContexts = new ArrayList<>();
    private final List<Runnable> openCleanups = new ArrayList<>();

    public RenderContext(RootRenderContext root) {
        this.root = root;
    }

    /** Returns the currently active context; throws {@link NoContextException} if none. */
    public static RenderContext current() {
        RenderContext c = CURRENT.get();
        if (c == null) {
            throw new NoContextException("No context set");
        }
        return c;
    }

    /** Open this context for a render pass. Returns an {@link AutoCloseable} that closes it. */
    public OpenScope open() {
        assertMounted();
        if (hookIndex != READY_TO_OPEN) {
            throw new IllegalStateException("RenderContext.open() was already called, and is not reentrant");
        }
        hookIndex = OPENED_AND_UNUSED;

        RenderContext previous = CURRENT.get();
        CURRENT.set(this);

        List<Runnable> oldUnmountListeners = new ArrayList<>(collectedUnmountListeners);
        collectedUnmountListeners.clear();

        List<String> oldContextKeys = new ArrayList<>(collectedContexts);
        collectedContexts.clear();

        collectedEffects.clear();

        return new OpenScope(previous, oldUnmountListeners, oldContextKeys);
    }

    public final class OpenScope implements AutoCloseable {
        private final RenderContext previous;
        private final List<Runnable> oldUnmountListeners;
        private final List<String> oldContextKeys;
        private boolean closed;

        OpenScope(RenderContext previous, List<Runnable> oldUnmountListeners, List<String> oldContextKeys) {
            this.previous = previous;
            this.oldUnmountListeners = oldUnmountListeners;
            this.oldContextKeys = oldContextKeys;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;

            try {
                // Run open-context cleanups in reverse registration order (matches Python). These
                // typically pop values pushed by ContextProviderElement so the next sibling sees
                // the right context value.
                for (int i = openCleanups.size() - 1; i >= 0; i--) {
                    try {
                        openCleanups.get(i).run();
                    } catch (RuntimeException ignored) {
                    }
                }
                openCleanups.clear();

                Set<String> currentKeys = new HashSet<>(collectedContexts);
                for (String key : oldContextKeys) {
                    if (!currentKeys.contains(key)) {
                        deleteChildContext(key);
                    }
                }
                Set<Runnable> currentListeners = new HashSet<>(collectedUnmountListeners);
                for (Runnable listener : oldUnmountListeners) {
                    if (!currentListeners.contains(listener)) {
                        try {
                            listener.run();
                        } catch (RuntimeException e) {
                            // continue running other unmount listeners
                        }
                    }
                }
                for (Effect effect : collectedEffects) {
                    if (effect.cleanup != null) {
                        try {
                            effect.cleanup.run();
                        } catch (RuntimeException ignored) {
                        }
                    }
                }
                for (Effect effect : collectedEffects) {
                    if (effect.effect != null) {
                        try {
                            effect.effect.run();
                        } catch (RuntimeException ignored) {
                        }
                    }
                }

                int seenHooks = hookIndex + 1;
                if (hookCount < 0) {
                    hookCount = seenHooks;
                } else if (hookCount != seenHooks) {
                    throw new IllegalStateException(
                            "Expected to use " + hookCount + " hooks, but used " + seenHooks);
                }
            } finally {
                CURRENT.set(previous);
                if (previous == null) {
                    CURRENT.remove();
                }
                hookIndex = READY_TO_OPEN;
                collectedEffects.clear();
            }
        }
    }

    private void assertActive() {
        if (CURRENT.get() != this) {
            throw new IllegalStateException("RenderContext method called when RenderContext not opened");
        }
    }

    private void assertMounted() {
        if (!isMounted) {
            throw new IllegalStateException("RenderContext method called when RenderContext is unmounted");
        }
    }

    public Map<String, List<String>> getQueryParams() {
        return root.getQueryParams();
    }

    public boolean hasState(int key) {
        return state.containsKey(key);
    }

    public Object getState(int key) {
        assertActive();
        return state.get(key);
    }

    /** Read state without requiring the context to be active; used by deferred setters. */
    public Object peekState(int key) {
        return state.get(key);
    }

    public void initState(int key, Object value) {
        if (state.containsKey(key)) {
            throw new IllegalStateException("Key " + key + " is already initialized");
        }
        state.put(key, value);
    }

    public void setState(int key, Object value) {
        if (!state.containsKey(key)) {
            throw new IllegalStateException("Key " + key + " not initialized");
        }
        // Queue the update on the render loop so it's batched with the next render.
        root.onChange(() -> state.put(key, value));
    }

    /** Set state without queueing — used inside an already-scheduled update. */
    public void setStateImmediate(int key, Object value) {
        state.put(key, value);
    }

    public RenderContext getChildContext(String key) {
        RenderContext child = childrenContext.get(key);
        if (child == null) {
            child = new RenderContext(root);
            childrenContext.put(key, child);
        }
        collectedContexts.add(key);
        return child;
    }

    public void deleteChildContext(String key) {
        RenderContext child = childrenContext.remove(key);
        if (child != null) {
            child.unmount();
        }
    }

    public int nextHookIndex() {
        hookIndex++;
        return hookIndex;
    }

    public void queueRender(Runnable update) {
        root.onQueueRender(update);
    }

    public void addEffect(Runnable cleanup, Runnable effect) {
        assertActive();
        collectedEffects.add(new Effect(cleanup, effect));
    }

    public void addUnmountListener(Runnable listener) {
        assertActive();
        collectedUnmountListeners.add(listener);
    }

    /**
     * Register a callback to fire after children of this context have rendered but before unmount
     * listeners / effects run. Used by {@link io.deephaven.ui.element.ContextProviderElement} to
     * pop its value off the {@link io.deephaven.ui.element.UiContext} stack.
     */
    public void addOpenCleanup(Runnable cleanup) {
        assertActive();
        openCleanups.add(cleanup);
    }

    public ExportedRenderState exportState() {
        Map<String, Object> out = new LinkedHashMap<>();

        Map<String, Object> retainedState = new LinkedHashMap<>();
        for (Map.Entry<Integer, Object> entry : state.entrySet()) {
            Object value = entry.getValue();
            if (isRetainable(value)) {
                retainedState.put(entry.getKey().toString(), value);
            }
        }
        if (!retainedState.isEmpty()) {
            out.put("state", retainedState);
        }

        Map<String, Object> childExports = new LinkedHashMap<>();
        for (Map.Entry<String, RenderContext> entry : childrenContext.entrySet()) {
            ExportedRenderState childState = entry.getValue().exportState();
            if (!childState.isEmpty()) {
                childExports.put(entry.getKey(), childState.asMap());
            }
        }
        if (!childExports.isEmpty()) {
            out.put("children", childExports);
        }
        return new ExportedRenderState(out);
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state) {
        this.state.clear();
        this.childrenContext.clear();

        if (state == null) {
            return;
        }
        Object queryParams = state.remove("__queryParams");
        if (queryParams instanceof Map) {
            root.setQueryParams((Map<String, List<String>>) queryParams);
        }
        Object inner = state.get("state");
        if (inner instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) inner).entrySet()) {
                // Keys arrive as strings after JSON deserialization; convert back to int slots.
                this.state.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }
        Object children = state.get("children");
        if (children instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) children).entrySet()) {
                if (entry.getValue() instanceof Map) {
                    getChildContext(entry.getKey()).importState((Map<String, Object>) entry.getValue());
                }
            }
        }
    }

    public void unmount() {
        assertMounted();
        isMounted = false;
        for (RenderContext child : childrenContext.values()) {
            child.unmount();
        }
        for (Runnable listener : collectedUnmountListeners) {
            try {
                listener.run();
            } catch (RuntimeException ignored) {
            }
        }
        hookIndex = READY_TO_OPEN;
        hookCount = -1;
        state.clear();
        childrenContext.clear();
        collectedEffects.clear();
        collectedUnmountListeners.clear();
        collectedContexts.clear();
    }

    private static boolean isRetainable(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean;
    }

    private static final class Effect {
        final Runnable cleanup;
        final Runnable effect;

        Effect(Runnable cleanup, Runnable effect) {
            this.cleanup = cleanup;
            this.effect = effect;
        }
    }
}
