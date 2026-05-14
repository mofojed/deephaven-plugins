package io.deephaven.ui.event;

import io.deephaven.ui.render.NoContextException;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Thread-local context for emitting client-side events ({@code event} JSON-RPC notifications).
 * Mirrors Python's {@code EventContext}.
 *
 * <p>Opened by {@link io.deephaven.ui.objecttype.ElementMessageStream} around the render thread's
 * work so hooks like {@code useSendEvent} (and convenience helpers like {@code Ui.toast}) can fire
 * events implicitly without threading a reference through every component.
 */
public final class EventContext {

    private static final ThreadLocal<EventContext> CURRENT = new ThreadLocal<>();

    private final BiConsumer<String, Map<String, Object>> onSendEvent;

    public EventContext(BiConsumer<String, Map<String, Object>> onSendEvent) {
        this.onSendEvent = onSendEvent;
    }

    public static EventContext current() {
        EventContext c = CURRENT.get();
        if (c == null) {
            throw new NoContextException("No event context set");
        }
        return c;
    }

    /** Open this context for the current thread. The returned closer restores the previous context. */
    public AutoCloseable open() {
        EventContext previous = CURRENT.get();
        CURRENT.set(this);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    public void sendEvent(String name, Map<String, Object> params) {
        onSendEvent.accept(name, params);
    }
}
