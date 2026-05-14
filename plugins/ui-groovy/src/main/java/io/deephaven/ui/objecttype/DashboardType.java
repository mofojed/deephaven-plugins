package io.deephaven.ui.objecttype;

import io.deephaven.plugin.type.ObjectCommunicationException;
import io.deephaven.plugin.type.ObjectType.MessageStream;
import io.deephaven.plugin.type.ObjectTypeBase;

/**
 * Dashboard type. Registered up front for parity with the Python plugin but treated as a stub in
 * MVP — {@link #isType(Object)} always returns {@code false} since the framework doesn't expose a
 * {@code DashboardElement} yet. See plan Phase 2.
 */
public class DashboardType extends ObjectTypeBase {

    public static final String NAME = "deephaven.ui.Dashboard";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isType(Object obj) {
        return false;
    }

    @Override
    public MessageStream compatibleClientConnection(Object obj, MessageStream connection)
            throws ObjectCommunicationException {
        throw new ObjectCommunicationException(
                "deephaven.ui.Dashboard is not yet supported by the Groovy plugin; see Phase 2.");
    }
}
