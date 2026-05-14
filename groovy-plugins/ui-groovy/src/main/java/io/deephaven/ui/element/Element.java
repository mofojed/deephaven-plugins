package io.deephaven.ui.element;

import io.deephaven.ui.render.RenderContext;

import java.util.Map;

/**
 * A renderable UI element. Mirrors the Python {@code Element} abstract class.
 * <p>
 * The element {@link #getName() name} is the {@code __dhElemName} sent over the wire and matches the
 * Python qualname conventions (e.g. {@code "deephaven.ui.components.Button"}).
 */
public interface Element {

    String DEFAULT_NAME = "deephaven.ui.Element";

    /** The unique name of this element (the {@code __dhElemName} on the wire). */
    default String getName() {
        return DEFAULT_NAME;
    }

    /** Optional React key for reconciliation. */
    default String getKey() {
        return null;
    }

    /**
     * Render this element within the provided context. Returns the element's props (already with
     * camelCase keys and {@code children} packed) — the {@link io.deephaven.ui.render.Renderer}
     * recurses into them to render any nested elements.
     */
    Map<String, Object> render(RenderContext context);
}
