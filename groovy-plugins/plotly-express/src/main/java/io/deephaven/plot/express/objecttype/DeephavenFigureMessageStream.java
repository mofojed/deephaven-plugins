package io.deephaven.plot.express.objecttype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.deephaven.plot.express.figure.DeephavenFigure;
import io.deephaven.plot.express.figure.Exporter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-client message stream for one {@link DeephavenFigure}. Mirrors the Python plugin's
 * {@code DeephavenFigureConnection} + {@code DeephavenFigureListener} pair, collapsed into one
 * class since this milestone doesn't yet support partitioned/ticking tables (no listener loop).
 *
 * <p>Protocol (JSON envelopes over the SPI byte stream):
 * <ul>
 *   <li>On {@link #start()}: synthesize a {@code RETRIEVE} request and push the initial
 *       {@code NEW_FIGURE} payload to the client. This matches the Python plugin's bootstrap in
 *       {@code DeephavenFigureType.create_client_connection}.</li>
 *   <li>{@code {"type": "RETRIEVE"}} → respond with {@code NEW_FIGURE}.</li>
 *   <li>{@code {"type": "FILTER", ...}} → ignored this milestone (input-filter support is a
 *       follow-up).</li>
 * </ul>
 */
public final class DeephavenFigureMessageStream implements ClientConnection {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DeephavenFigure figure;
    private final ClientConnection client;
    private final Exporter exporter = new Exporter();

    /** Monotonic counter for {@code NEW_FIGURE.revision}. Bumps would happen on partition/tick updates; not used yet. */
    private int revision = 0;

    private volatile boolean closed;

    public DeephavenFigureMessageStream(DeephavenFigure figure, ClientConnection client) {
        this.figure = figure;
        this.client = client;
    }

    /** Prime the connection: send the initial figure to the client. */
    public void start() {
        sendNewFigure();
    }

    @Override
    public void onData(byte[] payload, Object[] references) {
        if (closed) {
            return;
        }
        Map<?, ?> message;
        try {
            message = MAPPER.readValue(payload, Map.class);
        } catch (Exception e) {
            return;
        }
        Object type = message.get("type");
        if ("RETRIEVE".equals(type)) {
            sendNewFigure();
        }
        // FILTER and any other inbound types are intentionally unhandled this milestone.
    }

    @Override
    public void onClose() {
        closed = true;
    }

    private void sendNewFigure() {
        Map<String, Object> figureDict = figure.toWireDict(exporter);
        Exporter.References refs = exporter.references();

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "NEW_FIGURE");
        message.put("figure", figureDict);
        message.put("revision", revision);
        message.put("new_references", refs.newReferenceIds);
        message.put("removed_references", refs.removedReferenceIds);

        byte[] bytes;
        try {
            bytes = MAPPER.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize NEW_FIGURE payload", e);
        }
        client.onData(bytes, refs.newObjects.toArray(new Object[0]));
    }

    // Visible for testing.
    int currentRevision() {
        return revision;
    }

    static List<Object> emptyList() {
        return Collections.emptyList();
    }
}
