package io.deephaven.plot.express.builders;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a single-trace scattergl figure with {@code mode: "lines"} for the in-scope
 * {@code line_plot} fixture.
 */
public final class LineBuilder extends AbstractFigureBuilder {

    public LineBuilder(Object table, Map<String, Object> opts) {
        super(table, opts);
    }

    @Override
    protected Map<String, Object> buildTrace() {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("hovertemplate", hoverTemplate());
        trace.put("legendgroup", "");
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("color", "#636efa");
        line.put("dash", "solid");
        line.put("shape", "linear");
        trace.put("line", line);
        trace.put("marker", java.util.Collections.singletonMap("symbol", "circle"));
        trace.put("mode", "lines");
        trace.put("name", "");
        trace.put("showlegend", false);
        trace.put("xaxis", "x");
        trace.put("yaxis", "y");
        trace.put("type", "scattergl");
        return trace;
    }
}
