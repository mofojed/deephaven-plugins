package io.deephaven.plot.express.objecttype;

import io.deephaven.plot.express.figure.DeephavenFigure;
import io.deephaven.plugin.type.ObjectCommunicationException;
import io.deephaven.plugin.type.ObjectType.MessageStream;
import io.deephaven.plugin.type.ObjectTypeBase;

/**
 * Deephaven {@code ObjectType} for {@link DeephavenFigure}. The JS plugin recognizes figures by
 * the type name {@code "deephaven.plot.express.DeephavenFigure"} — must match the Python plugin's
 * name exactly so the same JS bundle handles either backend.
 */
public class DeephavenFigureType extends ObjectTypeBase {

    public static final String NAME = "deephaven.plot.express.DeephavenFigure";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isType(Object obj) {
        return obj instanceof DeephavenFigure;
    }

    @Override
    public MessageStream compatibleClientConnection(Object obj, MessageStream connection)
            throws ObjectCommunicationException {
        if (!(obj instanceof DeephavenFigure)) {
            throw new ObjectCommunicationException(
                    "Expected DeephavenFigure, got " + (obj == null ? "null" : obj.getClass().getName()));
        }
        ClientConnection downstream = new SpiMessageStreamBridge(connection);
        DeephavenFigureMessageStream stream = new DeephavenFigureMessageStream(
                (DeephavenFigure) obj, downstream);
        stream.start();
        return SpiMessageStreamBridge.wrap(stream);
    }
}
