package io.deephaven.ui.render;

/**
 * Thrown when a hook (or other context-dependent API) is called outside an open {@link RenderContext}.
 */
public class NoContextException extends RuntimeException {
    public NoContextException(String message) {
        super(message);
    }
}
