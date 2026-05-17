package io.deephaven.plot.express.registration;

import io.deephaven.plugin.Registration;
import io.deephaven.plot.express.jsplugin.ExpressJsPlugin;
import io.deephaven.plot.express.objecttype.DeephavenFigureType;

/**
 * Discovered by {@link java.util.ServiceLoader} via
 * {@code META-INF/services/io.deephaven.plugin.Registration}. The Deephaven server calls
 * {@link #registerInto(Callback)} at startup; we register the {@link DeephavenFigureType}
 * ObjectType and the JS plugin bundle.
 *
 * <p><strong>Note:</strong> {@code "deephaven.plot.express.DeephavenFigure"} is the same type
 * name the Python plugin uses. Installing both plugins on one server will fail at registration —
 * install exactly one.
 */
public final class ExpressRegistration implements Registration {

    @Override
    public void registerInto(Callback callback) {
        callback.register(new DeephavenFigureType());
        callback.register(new ExpressJsPlugin());
    }
}
