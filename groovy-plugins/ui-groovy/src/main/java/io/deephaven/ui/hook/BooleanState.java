package io.deephaven.ui.hook;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Result of {@link Hooks#useBoolean}: a (value, setter) pair where the setter has typed
 * {@code on() / off() / toggle()} affordances. Implements {@link Iterable} so Groovy multi-assign
 * ({@code def (b, set) = Ui.useBoolean()}) works without losing the {@link BooleanSetter} type.
 */
public final class BooleanState implements Iterable<Object> {

    private final boolean value;
    private final BooleanSetter setter;

    BooleanState(boolean value, BooleanSetter setter) {
        this.value = value;
        this.setter = setter;
    }

    public boolean value() {
        return value;
    }

    public BooleanSetter setter() {
        return setter;
    }

    @Override
    public Iterator<Object> iterator() {
        return Arrays.<Object>asList(value, setter).iterator();
    }
}
