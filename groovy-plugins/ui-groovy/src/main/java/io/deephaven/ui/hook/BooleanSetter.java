package io.deephaven.ui.hook;

import io.deephaven.ui.render.UiCallable;

/**
 * Setter returned from {@link Hooks#useBoolean}. Callable like a regular {@code set(value)} but
 * also exposes {@link #on()}, {@link #off()}, and {@link #toggle()} convenience methods.
 * Mirrors Python's {@code BooleanCallable} protocol.
 */
public final class BooleanSetter implements UiCallable {

    private final UiCallable underlying;
    private final Runnable onFn;
    private final Runnable offFn;
    private final Runnable toggleFn;

    BooleanSetter(UiCallable underlying, Runnable on, Runnable off, Runnable toggle) {
        this.underlying = underlying;
        this.onFn = on;
        this.offFn = off;
        this.toggleFn = toggle;
    }

    @Override
    public Object call(Object... args) {
        return underlying.call(args);
    }

    public void on() { onFn.run(); }
    public void off() { offFn.run(); }
    public void toggle() { toggleFn.run(); }
}
