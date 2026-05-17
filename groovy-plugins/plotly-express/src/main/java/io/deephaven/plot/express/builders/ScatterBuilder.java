package io.deephaven.plot.express.builders;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a single-trace scattergl figure for the in-scope fixtures
 * {@code scatter_fig} / {@code title_fig} in {@code tests/app.d/express.py}.
 */
public final class ScatterBuilder extends AbstractFigureBuilder {

    public ScatterBuilder(Object table, Map<String, Object> opts) {
        super(table, opts);
    }

    @Override
    protected Map<String, Object> buildTrace() {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("hovertemplate", hoverTemplate());
        trace.put("legendgroup", "");
        Map<String, Object> marker = new LinkedHashMap<>();
        marker.put("color", "#636efa");
        marker.put("symbol", "circle");
        trace.put("marker", marker);
        trace.put("mode", "markers");
        trace.put("name", "");
        trace.put("showlegend", false);
        // x and y data slots are inserted by the base class; "xaxis" / "yaxis" string IDs go here.
        trace.put("xaxis", "x");
        trace.put("yaxis", "y");
        trace.put("type", "scattergl");
        return trace;
    }
}
