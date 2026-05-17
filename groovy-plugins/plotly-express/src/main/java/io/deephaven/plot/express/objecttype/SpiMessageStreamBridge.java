package io.deephaven.plot.express.objecttype;

import io.deephaven.plugin.type.ObjectCommunicationException;
import io.deephaven.plugin.type.ObjectType.MessageStream;

import java.nio.ByteBuffer;

/**
 * Bridges between the framework's {@link ClientConnection} (byte arrays + Object[] references) and
 * the deephaven-core SPI's {@code MessageStream} (ByteBuffer + Object[] references).
 *
 * <p>This is the only file in the module that imports {@code io.deephaven.plugin.type.*}; the rest
 * of the framework is SPI-agnostic and unit-testable without a deephaven-core JAR on the classpath.
 */
public final class SpiMessageStreamBridge implements ClientConnection {

    private final MessageStream upstream;

    public SpiMessageStreamBridge(MessageStream upstream) {
        this.upstream = upstream;
    }

    @Override
    public void onData(byte[] payload, Object[] references) {
        try {
            upstream.onData(ByteBuffer.wrap(payload), references == null ? new Object[0] : references);
        } catch (ObjectCommunicationException e) {
            throw new RuntimeException("Failed to send payload upstream", e);
        }
    }

    @Override
    public void onClose() {
        upstream.onClose();
    }

    /** The inbound-side bridge: wrap our {@link DeephavenFigureMessageStream} so the SPI can deliver bytes to it. */
    public static MessageStream wrap(final DeephavenFigureMessageStream stream) {
        return new MessageStream() {
            @Override
            public void onData(ByteBuffer payload, Object[] references) {
                byte[] bytes = new byte[payload.remaining()];
                payload.get(bytes);
                stream.onData(bytes, references == null ? new Object[0] : references);
            }

            @Override
            public void onClose() {
                stream.onClose();
            }
        };
    }
}
