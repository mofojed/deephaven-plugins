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
 * Shared scaffolding for the simple x/y single-trace builders (scatter, line, bar). Builders
 * with substantially different shapes — OHLC/candlestick (multiple data fields per trace),
 * indicator (no axes) — don't extend this and build their own {@link DeephavenFigure} directly,
 * but reuse the static helpers here for axes and templates.
 */
abstract class AbstractFigureBuilder {

    protected final Object table;
    protected final String x;
    protected final String y;
    protected final String title;

    AbstractFigureBuilder(Object table, Map<String, Object> opts) {
        this.table = table;
        this.x = (String) opts.get("x");
        this.y = (String) opts.get("y");
        this.title = (String) opts.get("title");
    }

    public DeephavenFigure build() {
        Map<String, Object> data = new LinkedHashMap<>();
        if (x != null) {
            data.put("x", Placeholder.of(ColumnTypeResolver.kindOf(table, x)));
        }
        if (y != null) {
            data.put("y", Placeholder.of(ColumnTypeResolver.kindOf(table, y)));
        }
        Map<String, Object> orderedTrace = new LinkedHashMap<>(data);
        orderedTrace.putAll(buildTrace());

        List<Object> dataList = new ArrayList<>();
        dataList.add(orderedTrace);

        Map<String, Object> layout = new LinkedHashMap<>();
        layout.putAll(extraLayoutFirst());
        layout.put("legend", Collections.singletonMap("tracegroupgap", 0));
        layout.put("template", PlotlyTemplate.get());
        if (title != null) {
            layout.put("title", Collections.singletonMap("text", title));
        }
        layout.put("xaxis", axis("y", "bottom", x));
        layout.put("yaxis", axis("x", "left", y));

        Map<String, Object> plotly = new LinkedHashMap<>();
        plotly.put("data", dataList);
        plotly.put("layout", layout);

        DataMapping mapping = new DataMapping(table);
        if (x != null) {
            mapping.bind(0, "x", x);
        }
        if (y != null) {
            mapping.bind(0, "y", y);
        }
        List<DataMapping> mappings = new ArrayList<>(1);
        mappings.add(mapping);

        return new DeephavenFigure(plotly, mappings, false, false);
    }

    /** Trace fields excluding x/y data slots (the base class puts those first). */
    protected abstract Map<String, Object> buildTrace();

    /** Extra top-level layout fields placed BEFORE legend/template. Bar uses this for barmode. */
    protected Map<String, Object> extraLayoutFirst() {
        return Collections.emptyMap();
    }

    /** Standard hovertemplate. */
    protected String hoverTemplate() {
        String xLabel = x == null ? "" : x;
        String yLabel = y == null ? "" : y;
        return xLabel + "=%{x}<br>" + yLabel + "=%{y}<extra></extra>";
    }

    static Map<String, Object> axis(String anchor, String side, String columnName) {
        return axis(anchor, side, columnName, true);
    }

    /** Build a {@code xaxis} / {@code yaxis} entry. Datatypes are byte-compatible with python output. */
    static Map<String, Object> axis(String anchor, String side, String columnName, boolean withTitle) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("anchor", anchor);
        a.put("domain", Arrays.asList(0.0, 1.0));
        a.put("side", side);
        if (withTitle && columnName != null) {
            a.put("title", Collections.singletonMap("text", columnName));
        }
        return a;
    }
}
