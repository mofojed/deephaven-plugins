package io.deephaven.ui.objecttype;

/**
 * Outbound channel from the framework to the Deephaven server / client. A thin abstraction over
 * the deephaven-core {@code io.deephaven.plugin.type.MessageStream} so the framework can be
 * unit-tested without the SPI on the classpath.
 *
 * <p>The SPI binding lives in {@link io.deephaven.ui.objecttype.SpiMessageStreamBridge}.
 */
public interface ClientConnection {

    /** Send a payload (typically a JSON-RPC envelope) plus any exported object references. */
    void onData(byte[] payload, Object[] references);

    /** Signal the framework is shutting this stream down. */
    void onClose();
}
