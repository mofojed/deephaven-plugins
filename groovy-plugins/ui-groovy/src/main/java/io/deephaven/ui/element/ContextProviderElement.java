package io.deephaven.ui.element;

import io.deephaven.ui.render.RenderContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Element produced by {@link UiContext#provider(Object, Object...)}. On render, pushes its value
 * onto the {@link UiContext}'s thread-local stack and registers a cleanup with
 * {@link RenderContext#addOpenCleanup} so the value is popped after children render.
 *
 * <p>The wire name matches Python's {@code "deephaven.ui.elements.ContextProviderElement"} —
 * though in practice the JS plugin treats it as a transparent wrapper and just renders the
 * children.
 */
public final class ContextProviderElement<T> implements Element {

    public static final String NAME = "deephaven.ui.elements.ContextProviderElement";

    private final UiContext<T> context;
    private final T value;
    private final Object[] children;

    public ContextProviderElement(UiContext<T> context, T value, Object[] children) {
        this.context = context;
        this.value = value;
        this.children = children == null ? new Object[0] : children;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Object> render(RenderContext renderContext) {
        context.push(value);
        renderContext.addOpenCleanup(context::pop);

        Map<String, Object> props = new HashMap<>();
        if (children.length == 1) {
            props.put("children", children[0]);
        } else {
            props.put("children", Arrays.asList(children));
        }
        return props;
    }
}
