package io.deephaven.ui.element;

import java.util.Collections;

/**
 * Marker subclass of {@link BaseElement} for the dashboard root. The Deephaven server's
 * {@code DashboardType} matches this exact subclass (not the broader {@link Element}) so dashboards
 * get their own widget container; {@link io.deephaven.ui.objecttype.ElementType} handles everything
 * else.
 *
 * <p>The wire-format name is {@code "deephaven.ui.components.Dashboard"} and the wrapped element is
 * passed as the sole child — matching the Python plugin exactly.
 */
public class DashboardElement extends BaseElement {

    public static final String NAME = "deephaven.ui.components.Dashboard";

    public DashboardElement(Element element) {
        super(NAME, Collections.singletonList(element), null, null);
    }
}
