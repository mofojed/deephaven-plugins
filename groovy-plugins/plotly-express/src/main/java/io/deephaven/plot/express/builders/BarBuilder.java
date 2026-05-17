package io.deephaven.plot.express.builders;

import io.deephaven.plot.express.figure.ColumnTypeResolver;
import io.deephaven.plot.express.figure.CountByHelper;
import io.deephaven.plot.express.figure.DataMapping;
import io.deephaven.plot.express.figure.DeephavenFigure;
import io.deephaven.plot.express.figure.PartitionedTableHelper;
import io.deephaven.plot.express.figure.Placeholder;
import io.deephaven.plot.express.figure.PlotlyTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a bar figure. Three call shapes are supported:
 * <ul>
 *   <li>{@code bar(table, x:"…", y:"…")} — single trace, both axes explicit.</li>
 *   <li>{@code bar(table, x:"…")} / {@code bar(table, y:"…")} — single trace; runs
 *       {@code table.view([col]).countBy("count", col)} server-side and binds the synthetic
 *       {@code count} column to the missing axis.</li>
 *   <li>{@code bar(partitionedTable, x:"…", y:"…", by:"<keyCol>")} — one trace per partition,
 *       each backed by a separate Deephaven Table reference, colors rotating through plotly's
 *       default colorway. Mirrors what {@code dx.bar(partition_by(...), by=...)} does.</li>
 * </ul>
 */
public final class BarBuilder {

    // First 10 colors of plotly's default colorway. The partitioned variant uses these in
    // order; if there are more partitions we wrap around (the Python plugin does the same).
    private static final String[] COLORWAY = {
            "#636EFA", "#EF553B", "#00CC96", "#AB63FA", "#FFA15A",
            "#19D3F3", "#FF6692", "#B6E880", "#FF97FF", "#FECB52",
    };

    private final Object tableArg;
    private final String x;
    private final String y;
    private final String by;

    public BarBuilder(Object tableArg, Map<String, Object> opts) {
        this.tableArg = tableArg;
        this.x = (String) opts.get("x");
        this.y = (String) opts.get("y");
        this.by = (String) opts.get("by");
        if (x == null && y == null) {
            throw new IllegalArgumentException("bar requires at least one of x or y");
        }
    }

    public DeephavenFigure build() {
        if (PartitionedTableHelper.isPartitioned(tableArg)) {
            return buildPartitioned();
        }
        return buildSingleTrace();
    }

    // ─── Single-trace path (covers express_fig, bar_x_fig, bar_y_fig, ticking_fig) ──────────

    private DeephavenFigure buildSingleTrace() {
        Object effectiveTable;
        String xColumn;
        String yColumn;
        boolean countByOnX = false;
        boolean countByOnY = false;
        if (y == null) {
            effectiveTable = CountByHelper.countBy(tableArg, x);
            xColumn = x;
            yColumn = "count";
            countByOnY = true;
        } else if (x == null) {
            effectiveTable = CountByHelper.countBy(tableArg, y);
            xColumn = "count";
            yColumn = y;
            countByOnX = true;
        } else {
            effectiveTable = tableArg;
            xColumn = x;
            yColumn = y;
        }

        Map<String, Object> trace = barTrace(
                effectiveTable, xColumn, yColumn,
                /*hover*/ xColumn + "=%{x}<br>" + yColumn + "=%{y}<extra></extra>",
                /*name*/ "", /*legendgroup*/ "", /*color*/ "#636efa",
                /*showlegend*/ false,
                (x == null && y != null) ? "h" : "v",
                countByOnX, countByOnY);

        Map<String, Object> layout = standardLayout(/*legendTitle*/ null, xColumn, yColumn);

        Map<String, Object> plotly = new LinkedHashMap<>();
        plotly.put("data", Collections.singletonList(trace));
        plotly.put("layout", layout);

        DataMapping mapping = new DataMapping(effectiveTable);
        mapping.bind(0, "x", xColumn);
        mapping.bind(0, "y", yColumn);
        List<DataMapping> mappings = new ArrayList<>(1);
        mappings.add(mapping);

        return new DeephavenFigure(plotly, mappings, false, false);
    }

    // ─── Partitioned path (covers partitioned_fig) ──────────────────────────────────────────

    private DeephavenFigure buildPartitioned() {
        if (x == null || y == null) {
            throw new UnsupportedOperationException(
                    "partitioned bar requires both x and y this milestone (count_by + partitions not yet wired)");
        }
        List<PartitionedTableHelper.Partition> partitions = PartitionedTableHelper.snapshot(tableArg);
        if (partitions.isEmpty()) {
            // Reflection failed — degrade to single-trace using the partitioned table as if it
            // were a regular Table. Won't be visually correct but won't crash.
            return buildSingleTrace();
        }

        List<Object> traces = new ArrayList<>(partitions.size());
        List<DataMapping> mappings = new ArrayList<>(partitions.size());
        for (int i = 0; i < partitions.size(); i++) {
            PartitionedTableHelper.Partition p = partitions.get(i);
            String color = COLORWAY[i % COLORWAY.length];
            String hover = (by == null ? "" : by + "=" + p.keyLabel + "<br>")
                    + x + "=%{x}<br>" + y + "=%{y}<extra></extra>";
            traces.add(barTrace(p.table, x, y, hover, p.keyLabel, p.keyLabel, color,
                    /*showlegend*/ true, "v", false, false));

            DataMapping m = new DataMapping(p.table);
            m.bind(i, "x", x);
            m.bind(i, "y", y);
            mappings.add(m);
        }

        Map<String, Object> layout = standardLayout(by, x, y);

        Map<String, Object> plotly = new LinkedHashMap<>();
        plotly.put("data", traces);
        plotly.put("layout", layout);
        return new DeephavenFigure(plotly, mappings, false, false);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────────────────

    private static Map<String, Object> barTrace(
            Object table, String xColumn, String yColumn,
            String hovertemplate, String name, String legendgroup, String color,
            boolean showlegend, String orientation,
            boolean countByOnX, boolean countByOnY) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("x", Placeholder.of(countByOnX ? Placeholder.Kind.LONG
                : ColumnTypeResolver.kindOf(table, xColumn)));
        trace.put("y", Placeholder.of(countByOnY ? Placeholder.Kind.LONG
                : ColumnTypeResolver.kindOf(table, yColumn)));
        trace.put("hovertemplate", hovertemplate);
        trace.put("legendgroup", legendgroup);
        Map<String, Object> marker = new LinkedHashMap<>();
        marker.put("color", color);
        marker.put("pattern", Collections.singletonMap("shape", ""));
        trace.put("marker", marker);
        trace.put("name", name);
        trace.put("orientation", orientation);
        trace.put("showlegend", showlegend);
        trace.put("textposition", "auto");
        trace.put("xaxis", "x");
        trace.put("yaxis", "y");
        trace.put("type", "bar");
        return trace;
    }

    private static Map<String, Object> standardLayout(String legendTitle, String xColumn, String yColumn) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("barmode", "relative");
        Map<String, Object> legend = new LinkedHashMap<>();
        if (legendTitle != null) {
            Map<String, Object> ltitle = new LinkedHashMap<>();
            ltitle.put("side", "top");
            ltitle.put("text", legendTitle);
            legend.put("title", ltitle);
        }
        legend.put("tracegroupgap", 0);
        layout.put("legend", legend);
        layout.put("template", PlotlyTemplate.get());
        layout.put("xaxis", AbstractFigureBuilder.axis("y", "bottom", xColumn));
        layout.put("yaxis", AbstractFigureBuilder.axis("x", "left", yColumn));
        return layout;
    }
}
