package io.deephaven.ui.element;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * React-style context for sharing values down the component tree without prop drilling. Mirrors
 * Python's {@code Context}.
 *
 * <p>Create one with {@link io.deephaven.ui.Ui#createContext(Object)}. Provide a value with
 * {@link #provider(Object, Object...)}. Read the active value inside a component with
 * {@link io.deephaven.ui.hook.Hooks#useContext(UiContext)} (or {@code Ui.useContext(ctx)}).
 *
 * @param <T> the type of value carried by this context
 */
public final class UiContext<T> {

    /**
     * Per-thread stacks for active context values. Keyed by the {@link UiContext} instance itself
     * via {@link IdentityHashMap}, so two contexts of the same value type don't share storage.
     */
    private static final ThreadLocal<Map<UiContext<?>, Deque<Object>>> STACKS =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private final T defaultValue;

    public UiContext(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    /** The current value seen by a consumer: nearest provider on the stack, else the default. */
    @SuppressWarnings("unchecked")
    public T currentValue() {
        Deque<Object> stack = STACKS.get().get(this);
        if (stack == null || stack.isEmpty()) {
            return defaultValue;
        }
        return (T) stack.peek();
    }

    /** Wrap children to expose {@code value} to anything beneath them that calls {@link #currentValue()}. */
    public Element provider(T value, Object... children) {
        return new ContextProviderElement<>(this, value, children);
    }

    void push(T value) {
        STACKS.get().computeIfAbsent(this, k -> new ArrayDeque<>()).push(value);
    }

    void pop() {
        Deque<Object> stack = STACKS.get().get(this);
        if (stack != null && !stack.isEmpty()) {
            stack.pop();
        }
    }
}
