package io.deephaven.ui.render;

import groovy.lang.Closure;
import io.deephaven.ui.element.RenderedNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encodes a {@link RenderedNode} tree into the wire format the JS plugin expects:
 * <ul>
 *   <li>Elements become {@code {__dhElemName, props}}.</li>
 *   <li>Callables (Java functional interfaces, {@code UiCallable}, Groovy {@code Closure}s) become
 *       {@code {__dhCbid: "cb<n>"}} and are registered for later invocation.</li>
 *   <li>Non-primitive non-iterable objects (Deephaven tables, figures, ...) become
 *       {@code {__dhObid: <index>}} and are appended to {@code newObjects}.</li>
 * </ul>
 *
 * <p>State across renders: callable identity is preserved via an {@link IdentityHashMap} so we
 * don't churn IDs for stable closures. Exported objects survive a render if they're still
 * referenced; otherwise they're dropped from the next render's exports.
 */
public final class NodeEncoder {

    public static final String CALLABLE_KEY = "__dhCbid";
    public static final String OBJECT_KEY = "__dhObid";
    public static final String ELEMENT_KEY = "__dhElemName";
    public static final String DEFAULT_CALLABLE_PREFIX = "cb";

    private final String callablePrefix;
    private int nextCallableId;
    private int nextObjectId;

    private final IdentityHashMap<Object, String> callableIds = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Integer> objectIds = new IdentityHashMap<>();
    private final Map<String, Object> liveCallables = new LinkedHashMap<>();
    private List<Object> newObjects = new ArrayList<>();

    public NodeEncoder() {
        this(DEFAULT_CALLABLE_PREFIX);
    }

    public NodeEncoder(String callablePrefix) {
        this.callablePrefix = callablePrefix;
    }

    public static final class Result {
        public final Map<String, Object> encodedNode;
        public final List<Object> newObjects;
        public final Map<String, Object> liveCallables;

        Result(Map<String, Object> encodedNode, List<Object> newObjects, Map<String, Object> liveCallables) {
            this.encodedNode = encodedNode;
            this.newObjects = newObjects;
            this.liveCallables = liveCallables;
        }
    }

    public Result encodeNode(RenderedNode node) {
        newObjects = new ArrayList<>();

        // Track which exported objects we saw this render so we can drop the stale ones.
        IdentityHashMap<Object, Boolean> seenObjects = new IdentityHashMap<>();
        // Reset live callable view for this render; callable identity preservation persists across.
        liveCallables.clear();

        @SuppressWarnings("unchecked")
        Map<String, Object> encoded = (Map<String, Object>) transform(node, seenObjects);

        // Prune object IDs that no longer appear so future objects with the same identity get fresh exports.
        objectIds.keySet().removeIf(obj -> !seenObjects.containsKey(obj));

        return new Result(encoded, new ArrayList<>(newObjects), new LinkedHashMap<>(liveCallables));
    }

    @SuppressWarnings("unchecked")
    private Object transform(Object value, IdentityHashMap<Object, Boolean> seenObjects) {
        if (value == null) {
            return null;
        }
        if (value instanceof RenderedNode) {
            return convertRenderedNode((RenderedNode) value, seenObjects);
        }
        if (isCallable(value)) {
            return convertCallable(value);
        }
        if (value instanceof Map) {
            Map<String, Object> in = (Map<String, Object>) value;
            Map<String, Object> out = new LinkedHashMap<>(in.size());
            for (Map.Entry<String, Object> e : in.entrySet()) {
                out.put(e.getKey(), transform(e.getValue(), seenObjects));
            }
            return out;
        }
        if (value instanceof List) {
            List<Object> in = (List<Object>) value;
            List<Object> out = new ArrayList<>(in.size());
            for (Object item : in) {
                out.add(transform(item, seenObjects));
            }
            return out;
        }
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            List<Object> out = new ArrayList<>(arr.length);
            for (Object item : arr) {
                out.add(transform(item, seenObjects));
            }
            return out;
        }
        if (value instanceof Collection) {
            List<Object> out = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                out.add(transform(item, seenObjects));
            }
            return out;
        }
        if (value instanceof CharSequence && !(value instanceof String)) {
            // Coerce GStrings / StringBuilders to plain Strings so JSON output is a string, not an object.
            return value.toString();
        }
        if (isPrimitive(value)) {
            return value;
        }
        return convertObject(value, seenObjects);
    }

    private Map<String, Object> convertRenderedNode(RenderedNode node, IdentityHashMap<Object, Boolean> seen) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(ELEMENT_KEY, node.getName());
        Map<String, Object> props = node.getProps();
        if (props != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> transformed = (Map<String, Object>) transform(props, seen);
            out.put("props", transformed);
        }
        return out;
    }

    private Map<String, Object> convertCallable(Object cb) {
        String id = callableIds.get(cb);
        if (id == null) {
            id = callablePrefix + nextCallableId++;
            callableIds.put(cb, id);
        }
        liveCallables.put(id, cb);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(CALLABLE_KEY, id);
        return out;
    }

    private Map<String, Object> convertObject(Object obj, IdentityHashMap<Object, Boolean> seen) {
        Integer id = objectIds.get(obj);
        if (id == null) {
            id = nextObjectId++;
            objectIds.put(obj, id);
            newObjects.add(obj);
        }
        seen.put(obj, Boolean.TRUE);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(OBJECT_KEY, id);
        return out;
    }

    public static boolean isCallable(Object value) {
        return value instanceof Closure
                || value instanceof UiCallable
                || value instanceof Runnable
                || value instanceof java.util.function.Consumer
                || value instanceof java.util.function.BiConsumer
                || value instanceof java.util.function.Function
                || value instanceof java.util.function.BiFunction
                || value instanceof java.util.function.Supplier;
    }

    public static boolean isPrimitive(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character;
    }
}
