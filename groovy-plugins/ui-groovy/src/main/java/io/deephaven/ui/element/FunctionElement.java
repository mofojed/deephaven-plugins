package io.deephaven.ui.element;

import io.deephaven.ui.render.RenderContext;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/**
 * An {@link Element} that defers rendering to a user-supplied function. Equivalent to Python's
 * {@code FunctionElement} — what {@code @ui.component} decorated functions become.
 */
public class FunctionElement implements Element {

    private final String name;
    private final Supplier<Object> renderFn;
    private final String key;

    public FunctionElement(String name, Supplier<Object> renderFn, String key) {
        this.name = name;
        this.renderFn = renderFn;
        this.key = key;
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
        Object children = renderFn.get();
        return Collections.singletonMap("children", children);
    }
}
