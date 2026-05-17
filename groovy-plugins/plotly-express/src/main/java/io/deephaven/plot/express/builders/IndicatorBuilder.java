package io.deephaven.plot.express.builders;

import io.deephaven.plot.express.figure.ColumnTypeResolver;
import io.deephaven.plot.express.figure.DataMapping;
import io.deephaven.plot.express.figure.DeephavenFigure;
import io.deephaven.plot.express.figure.Placeholder;
import io.deephaven.plot.express.figure.PlotlyTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a single-value {@code indicator} trace for the in-scope {@code express_indicator}
 * fixture. The shape is unlike scatter/line/bar — no axes, scalar value placeholder, fixed
 * delta arrows, and a {@code margin.t=60} so the title doesn't collide with the value display.
 *
 * <p>The grouped variant ({@code express_indicator_by} with {@code by="Categories"}) needs trace
 * fan-out via partitioning and is deferred.
 */
public final class IndicatorBuilder {

    private final Object table;
    private final String value;
    private final String title;

    public IndicatorBuilder(Object table, Map<String, Object> opts) {
        this.table = table;
        this.value = (String) opts.get("value");
        this.title = (String) opts.get("title");
        if (value == null) {
            throw new IllegalArgumentException("indicator requires a value= column");
        }
    }

    public DeephavenFigure build() {
        Map<String, Object> trace = new LinkedHashMap<>();
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("decreasing", Collections.singletonMap("symbol", "▼"));
        delta.put("increasing", Collections.singletonMap("symbol", "▲"));
        trace.put("delta", delta);
        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("x", Arrays.asList(0.0, 1.0));
        domain.put("y", Arrays.asList(0.0, 1.0));
        trace.put("domain", domain);
        trace.put("mode", "number");
        if (title != null) {
            trace.put("title", Collections.singletonMap("text", title));
        }
        // Scalar value placeholder. The JS plugin replaces it via the mapping with one
        // real number from the value column.
        trace.put("value", scalarPlaceholder(ColumnTypeResolver.kindOf(table, value)));
        trace.put("type", "indicator");

        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("legend", Collections.singletonMap("tracegroupgap", 0));
        layout.put("template", PlotlyTemplate.get());
        // No axes — indicators are just a number.
        layout.put("margin", Collections.singletonMap("t", 60));

        Map<String, Object> plotly = new LinkedHashMap<>();
        plotly.put("data", Collections.singletonList(trace));
        plotly.put("layout", layout);

        DataMapping mapping = new DataMapping(table);
        mapping.bind(0, "value", value);
        List<DataMapping> mappings = new ArrayList<>(1);
        mappings.add(mapping);

        return new DeephavenFigure(plotly, mappings, false, false);
    }

    /**
     * Scalar (not array) NULL sentinel for the indicator's {@code value} field. Python plotly
     * emits a plain numeric literal here (not bdata), so we mirror that.
     */
    private static Object scalarPlaceholder(Placeholder.Kind kind) {
        switch (kind) {
            case INT:
                return Integer.MIN_VALUE;
            case LONG:
                return Long.MIN_VALUE;
            case DOUBLE:
                return Double.NaN;
            default:
                return 0;
        }
    }
}
