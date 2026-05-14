package io.deephaven.ui.hook;

/**
 * Mutable single-value holder, the Groovy/Java mirror of Python's {@code Ref}. Returned by
 * {@link Hooks#useRef}; persists across renders.
 */
public final class Ref<T> {
    public T current;

    public Ref(T current) {
        this.current = current;
    }
}
