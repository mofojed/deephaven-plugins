package io.deephaven.ui.render;

/**
 * Marker interface for any callable that should be encoded as {@code __dhCbid} on the wire.
 *
 * <p>The encoder also recognizes Groovy closures and the common {@code java.util.function.*}
 * functional interfaces; this interface is the explicit way for framework code to opt in (e.g.,
 * the setter returned from {@link io.deephaven.ui.hook.Hooks#useState}).
 */
public interface UiCallable {
    Object call(Object... args);
}
