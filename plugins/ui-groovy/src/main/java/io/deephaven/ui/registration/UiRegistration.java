package io.deephaven.ui.registration;

import io.deephaven.plugin.Registration;
import io.deephaven.ui.jsplugin.UiJsPlugin;
import io.deephaven.ui.objecttype.DashboardType;
import io.deephaven.ui.objecttype.ElementType;

/**
 * Discovered by {@link java.util.ServiceLoader} via
 * {@code META-INF/services/io.deephaven.plugin.Registration}. The Deephaven server calls
 * {@link #registerInto(Callback)} at startup; we register our ObjectTypes and the JS plugin bundle.
 *
 * <p><strong>Note:</strong> {@code "deephaven.ui.Element"} and {@code "deephaven.ui.Dashboard"} are
 * the same names the Python plugin uses. Installing both plugins on one server will fail at
 * registration — install exactly one.
 */
public final class UiRegistration implements Registration {

    @Override
    public void registerInto(Callback callback) {
        callback.register(new ElementType());
        callback.register(new DashboardType());
        callback.register(new UiJsPlugin());
    }
}
