package io.deephaven.ui.hook;

import io.deephaven.ui.render.UiCallable;

import java.util.Arrays;
import java.util.Iterator;

/**
 * The (value, setter) pair returned from {@link Hooks#useState}. Implements {@link Iterable} so
 * Groovy multi-assignment ({@code def (count, setCount) = Ui.useState(0)}) destructures it.
 */
public final class StateTuple<T> implements Iterable<Object> {

    private final T value;
    private final UiCallable setter;

    public StateTuple(T value, UiCallable setter) {
        this.value = value;
        this.setter = setter;
    }

    public T value() {
        return value;
    }

    public UiCallable setter() {
        return setter;
    }

    @Override
    public Iterator<Object> iterator() {
        return Arrays.<Object>asList(value, setter).iterator();
    }
}
