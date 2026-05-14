package io.deephaven.ui.render;

import java.util.Map;

/**
 * The serializable shape of a {@link RenderContext}'s state. Mirrors Python's
 * {@code ExportedRenderState} dict layout: {@code {"state": {slot: value, ...}, "children": {key: ...}}}.
 */
public final class ExportedRenderState {

    private final Map<String, Object> data;

    public ExportedRenderState(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> asMap() {
        return data;
    }

    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }
}
