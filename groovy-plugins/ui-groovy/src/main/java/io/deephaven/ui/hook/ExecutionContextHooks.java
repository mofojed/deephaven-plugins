package io.deephaven.ui.hook;

import io.deephaven.engine.context.ExecutionContext;
import io.deephaven.util.SafeCloseable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Lives in its own file (separate from {@link Hooks}) so a runtime without the deephaven
 * execution-context classes on the classpath can still load the rest of the hook surface.
 *
 * <p>Equivalent to Python's {@code use_execution_context}: returns a function that, given a
 * {@link Runnable}, runs it inside a captured {@link ExecutionContext}. Handy when you need to
 * invoke Deephaven engine APIs from a thread that doesn't have the right context attached.
 */
public final class ExecutionContextHooks {

    private ExecutionContextHooks() {}

    /** Capture the current context (or the provided one) and return a wrapper. */
    public static Consumer<Runnable> useExecutionContext() {
        return useExecutionContext(null);
    }

    public static Consumer<Runnable> useExecutionContext(ExecutionContext explicit) {
        ExecutionContext ctx = Hooks.useMemo(
                () -> explicit != null ? explicit : ExecutionContext.getContext(),
                List.of(explicit == null ? "auto" : explicit));
        return r -> {
            try (SafeCloseable ignored = ctx.open()) {
                r.run();
            }
        };
    }
}
