package io.deephaven.ui.hook;

import groovy.lang.Closure;
import io.deephaven.ui.element.UiContext;
import io.deephaven.ui.event.EventContext;
import io.deephaven.ui.render.RenderContext;
import io.deephaven.ui.render.UiCallable;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * React-style hooks. Static API; read the current {@link RenderContext} via
 * {@link RenderContext#current()}. Mirrors Python's {@code use_state}, {@code use_effect},
 * {@code use_callback}, {@code use_memo}, {@code use_ref}.
 */
public final class Hooks {

    private static final Object UNSET = new Object();

    private Hooks() {}

    /** Hook to add a state variable. Returns (value, setter). The setter accepts either a new value or an updater function. */
    @SuppressWarnings("unchecked")
    public static <T> StateTuple<T> useState(T initial) {
        final RenderContext ctx = RenderContext.current();
        final int slot = ctx.nextHookIndex();
        if (!ctx.hasState(slot)) {
            Object resolved = initial instanceof Supplier ? ((Supplier<?>) initial).get() : initial;
            ctx.initState(slot, resolved);
        }
        T value = (T) ctx.getState(slot);
        UiCallable setter = args -> {
            Object next = args != null && args.length > 0 ? args[0] : null;
            ctx.queueRender(() -> {
                Object current = ctx.peekState(slot);
                Object resolved = resolveUpdate(next, current);
                // setState (not setStateImmediate) so onChange fires markDirty and the next render
                // actually runs. setStateImmediate alone updates the map but leaves the loop idle.
                ctx.setState(slot, resolved);
            });
            return null;
        };
        return new StateTuple<>(value, setter);
    }

    /** Hook for storing a mutable value that persists across renders. */
    @SuppressWarnings("unchecked")
    public static <T> Ref<T> useRef(T initial) {
        RenderContext ctx = RenderContext.current();
        int slot = ctx.nextHookIndex();
        if (!ctx.hasState(slot)) {
            ctx.initState(slot, new Ref<>(initial));
        }
        return (Ref<T>) ctx.getState(slot);
    }

    /**
     * Memoize a value across renders. The supplier re-runs when {@code dependencies} differs from
     * the previous render's dependencies (by {@link Objects#equals}).
     */
    @SuppressWarnings("unchecked")
    public static <T> T useMemo(Supplier<T> fn, List<?> dependencies) {
        if (dependencies == null) {
            throw new IllegalArgumentException("dependencies must be a list (use [] to run only once)");
        }
        Ref<Object> depsRef = useRef(UNSET);
        Ref<Object> valueRef = useRef(null);
        if (depsRef.current == UNSET || !Objects.equals(depsRef.current, dependencies)) {
            valueRef.current = fn.get();
            depsRef.current = dependencies;
        }
        return (T) valueRef.current;
    }

    /** Memoize a callback. Identity is stable until {@code dependencies} change. */
    public static <T> T useCallback(T callback, List<?> dependencies) {
        return useMemo(() -> callback, dependencies);
    }

    /**
     * Call {@code effect} after this component renders if {@code dependencies} changed (or every
     * render when {@code null}). The effect may return a cleanup {@link Runnable} that runs before
     * the next effect invocation and on unmount.
     */
    public static void useEffect(Supplier<Runnable> effect, List<?> dependencies) {
        Ref<Object> depsRef = useRef(UNSET);
        Ref<Runnable> cleanupRef = useRef(null);
        Ref<Boolean> mountedRef = useRef(false);

        final boolean isDirty = !mountedRef.current
                || dependencies == null
                || !Objects.equals(depsRef.current, dependencies);

        Runnable cleanup = () -> {
            if (isDirty && cleanupRef.current != null) {
                try {
                    cleanupRef.current.run();
                } finally {
                    cleanupRef.current = null;
                }
            }
        };
        Runnable runEffect = () -> {
            mountedRef.current = true;
            if (isDirty) {
                cleanupRef.current = effect.get();
                depsRef.current = dependencies;
            }
        };
        // Stabilize the unmount listener across renders. RenderContext fires any unmount listener
        // that was registered last render but is missing this render — if we constructed a fresh
        // lambda each render they'd never match, and the effect would tear itself down on every
        // render. Python achieves the same with use_callback(unmount, []).
        Runnable unmount = useCallback((Runnable) () -> {
            mountedRef.current = false;
            if (cleanupRef.current != null) {
                try {
                    cleanupRef.current.run();
                } finally {
                    cleanupRef.current = null;
                }
            }
        }, List.of());

        RenderContext ctx = RenderContext.current();
        ctx.addEffect(cleanup, runEffect);
        ctx.addUnmountListener(unmount);
    }

    /** Convenience overload: effect without a cleanup. */
    public static void useEffect(Runnable effect, List<?> dependencies) {
        useEffect(() -> {
            effect.run();
            return null;
        }, dependencies);
    }

    /**
     * Returns a function for sending client-side events to the JS plugin. Equivalent to Python's
     * {@code use_send_event}. The returned callable forwards to the active {@link EventContext}.
     */
    public static BiConsumer<String, Map<String, Object>> useSendEvent() {
        EventContext ctx = EventContext.current();
        return ctx::sendEvent;
    }

    /**
     * Convenience hook for boolean state. Equivalent to Python's {@code use_boolean}. The returned
     * {@link BooleanState} is Groovy-destructurable ({@code def (val, set) = Ui.useBoolean()});
     * the setter acts like a normal setter and also exposes {@code on()}, {@code off()}, and
     * {@code toggle()} for cleaner callsites.
     */
    public static BooleanState useBoolean(boolean initial) {
        StateTuple<Boolean> state = useState(initial);
        UiCallable setter = state.setter();
        Runnable on = useCallback((Runnable) () -> setter.call(Boolean.TRUE), List.of(setter));
        Runnable off = useCallback((Runnable) () -> setter.call(Boolean.FALSE), List.of(setter));
        Runnable toggle = useCallback(
                (Runnable) () -> setter.call((Function<Object, Object>) old -> !((Boolean) old)),
                List.of(setter));
        BooleanSetter bool = useMemo(() -> new BooleanSetter(setter, on, off, toggle),
                List.of(setter, on, off, toggle));
        return new BooleanState(state.value(), bool);
    }

    /** @see #useBoolean(boolean) */
    public static BooleanState useBoolean() {
        return useBoolean(false);
    }

    /**
     * Returns a callable for queueing work onto the render thread. Equivalent to Python's
     * {@code use_render_queue}. Useful for state updates that originate from background threads.
     */
    public static Consumer<Runnable> useRenderQueue() {
        RenderContext ctx = RenderContext.current();
        return ctx::queueRender;
    }

    /**
     * Read the current value of a {@link UiContext}. The active provider's value if any, otherwise
     * the context's default value. Must be called inside a render. Equivalent to Python's
     * {@code use_context}.
     */
    public static <T> T useContext(UiContext<T> context) {
        RenderContext.current(); // ensure we're rendering
        return context.currentValue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object resolveUpdate(Object next, Object current) {
        if (next instanceof Closure) {
            return ((Closure) next).call(current);
        }
        if (next instanceof Function) {
            return ((Function) next).apply(current);
        }
        return next;
    }
}
