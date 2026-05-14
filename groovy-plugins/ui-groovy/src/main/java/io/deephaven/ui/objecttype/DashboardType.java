package io.deephaven.ui.objecttype;

import io.deephaven.plugin.type.ObjectCommunicationException;
import io.deephaven.plugin.type.ObjectType.MessageStream;
import io.deephaven.plugin.type.ObjectTypeBase;
import io.deephaven.ui.element.DashboardElement;

/**
 * Deephaven {@code ObjectType} for {@link DashboardElement}. Registered before {@link ElementType}
 * in {@link io.deephaven.ui.registration.UiRegistration} so dashboards match the more specific type
 * even though they are also {@link io.deephaven.ui.element.Element}s.
 *
 * <p>Shares the wire protocol with {@code ElementType} — the only difference is the type name on
 * the field metadata; the message stream is identical.
 */
public class DashboardType extends ObjectTypeBase {

    public static final String NAME = "deephaven.ui.Dashboard";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isType(Object obj) {
        return obj instanceof DashboardElement;
    }

    @Override
    public MessageStream compatibleClientConnection(Object obj, MessageStream connection)
            throws ObjectCommunicationException {
        if (!(obj instanceof DashboardElement)) {
            throw new ObjectCommunicationException(
                    "Expected DashboardElement, got " + (obj == null ? "null" : obj.getClass().getName()));
        }
        ClientConnection downstream = new SpiMessageStreamBridge(connection);
        ElementMessageStream stream = new ElementMessageStream((DashboardElement) obj, downstream);
        stream.start();
        return SpiMessageStreamBridge.wrap(stream);
    }
}
