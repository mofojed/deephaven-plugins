package io.deephaven.plot.express.builders;

import io.deephaven.plot.express.figure.ColumnTypeResolver;
import io.deephaven.plot.express.figure.DataMapping;
import io.deephaven.plot.express.figure.DeephavenFigure;
import io.deephaven.plot.express.figure.Placeholder;
import io.deephaven.plot.express.figure.PlotlyTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Common base for {@code ohlc} and {@code candlestick} traces. The shape is the same — one trace
 * with x + open/high/low/close data slots and a minimal layout (axes only, no legend, no title,
 * no axis titles). Only the trace {@code type} differs.
 */
abstract class OhlcLikeBuilder {

    protected final Object table;
    protected final String x;
    protected final String open;
    protected final String high;
    protected final String low;
    protected final String close;

    OhlcLikeBuilder(Object table, Map<String, Object> opts) {
        this.table = table;
        this.x = (String) opts.get("x");
        this.open = (String) opts.get("open");
        this.high = (String) opts.get("high");
        this.low = (String) opts.get("low");
        this.close = (String) opts.get("close");
        if (x == null || open == null || high == null || low == null || close == null) {
            throw new IllegalArgumentException("ohlc/candlestick require x, open, high, low, close");
        }
    }

    public DeephavenFigure build() {
        // Trace ordering matches Python: close, high, low, open (alphabetical inside the
        // financial-data dict), then x last, then type. That insertion order keeps a JSON diff
        // against the captured golden meaningful.
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("close", Placeholder.of(ColumnTypeResolver.kindOf(table, close)));
        trace.put("high", Placeholder.of(ColumnTypeResolver.kindOf(table, high)));
        trace.put("low", Placeholder.of(ColumnTypeResolver.kindOf(table, low)));
        trace.put("open", Placeholder.of(ColumnTypeResolver.kindOf(table, open)));
        trace.put("x", Placeholder.of(ColumnTypeResolver.kindOf(table, x)));
        trace.put("type", traceType());

        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("template", PlotlyTemplate.get());
        // No axis titles — plotly omits them because the trace plots 4 columns on the y axis
        // and one wouldn't make sense.
        layout.put("xaxis", AbstractFigureBuilder.axis("y", "bottom", null, false));
        layout.put("yaxis", AbstractFigureBuilder.axis("x", "left", null, false));

        Map<String, Object> plotly = new LinkedHashMap<>();
        plotly.put("data", Collections.singletonList(trace));
        plotly.put("layout", layout);

        DataMapping mapping = new DataMapping(table);
        mapping.bind(0, "x", x);
        mapping.bind(0, "open", open);
        mapping.bind(0, "high", high);
        mapping.bind(0, "low", low);
        mapping.bind(0, "close", close);
        List<DataMapping> mappings = new ArrayList<>(1);
        mappings.add(mapping);

        return new DeephavenFigure(plotly, mappings, false, false);
    }

    /** Either "ohlc" or "candlestick". */
    protected abstract String traceType();
}
