package io.deephaven.ui.element;

import io.deephaven.ui.render.RenderContext;
import io.deephaven.ui.util.PropCase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base implementation for built-in UI elements that have no special render logic. Direct port of
 * the Python {@code BaseElement}: packs {@code children} into props, normalizes prop keys to
 * camelCase / React-style names, drops null entries.
 */
public class BaseElement implements Element {

    private final String name;
    private final String key;
    private final Map<String, Object> props;

    public BaseElement(String name, List<Object> children, String key, Map<String, Object> rawProps) {
        this.name = name;
        this.key = key;

        Map<String, Object> props = new LinkedHashMap<>();
        if (rawProps != null) {
            props.putAll(rawProps);
        }

        Object existingChildren = props.get("children");
        if (children != null && !children.isEmpty() && existingChildren != null) {
            throw new IllegalArgumentException("Cannot provide both children and props.children");
        }

        if (children != null) {
            if (children.size() == 1) {
                // Single child is passed as a scalar to match React expectations.
                props.put("children", children.get(0));
            } else if (children.size() > 1) {
                props.put("children", new ArrayList<>(children));
            }
        }

        // Store the key under "key" so the renderer can pick it up alongside other props.
        props.put("key", key);

        this.props = PropCase.dictToReactProps(props);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Map<String, Object> render(RenderContext context) {
        return props;
    }
}
