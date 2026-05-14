package io.deephaven.ui.objecttype;

import io.deephaven.plugin.type.ObjectCommunicationException;
import io.deephaven.plugin.type.ObjectType.MessageStream;
import io.deephaven.plugin.type.ObjectTypeBase;
import io.deephaven.ui.element.Element;

/**
 * Deephaven {@code ObjectType} for {@link Element}. The JS plugin recognizes widgets by the type
 * name {@code "deephaven.ui.Element"} — must match the Python plugin's name exactly.
 *
 * <p>Extends {@link ObjectTypeBase} (not {@code ObjectTypeClassBase}) so {@link #isType} can use
 * {@code instanceof} semantics. {@code ObjectTypeClassBase.isType} does an exact-class-equality
 * check, which rejects all our subclasses ({@code BaseElement}, {@code FunctionElement}, ...).
 */
public class ElementType extends ObjectTypeBase {

    public static final String NAME = "deephaven.ui.Element";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isType(Object obj) {
        return obj instanceof Element;
    }

    @Override
    public MessageStream compatibleClientConnection(Object obj, MessageStream connection)
            throws ObjectCommunicationException {
        if (!(obj instanceof Element)) {
            throw new ObjectCommunicationException(
                    "Expected Element, got " + (obj == null ? "null" : obj.getClass().getName()));
        }
        ClientConnection downstream = new SpiMessageStreamBridge(connection);
        ElementMessageStream stream = new ElementMessageStream((Element) obj, downstream);
        stream.start();
        return SpiMessageStreamBridge.wrap(stream);
    }
}
