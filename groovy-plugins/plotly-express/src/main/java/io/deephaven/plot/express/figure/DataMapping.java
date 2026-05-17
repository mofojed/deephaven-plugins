package io.deephaven.plot.express.figure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Deephaven table column names to JSON-pointer paths inside the plotly figure tree.
 *
 * <p>Mirrors Python's {@code plot/express/data_mapping/DataMapping.py} +
 * {@code data_mapping/json_conversion.py:json_link_mapping}. For a single trace built from a
 * single table, the wire shape is:
 *
 * <pre>{@code
 * { "table": 0,
 *   "data_columns": {
 *     "Categories": ["/plotly/data/0/x"],
 *     "Values":     ["/plotly/data/0/y"]
 *   } }
 * }</pre>
 *
 * <p>Lists of strings (not single strings) are the values because plotly.express's cartesian-
 * product fan-out can map the same column to multiple trace fields. This milestone always emits
 * single-element lists, but we keep the list shape for byte-compat with the Python plugin.
 */
public final class DataMapping {

    private final Object table;
    /** Insertion-ordered column → [pointers]. Keys are user-visible Deephaven column names. */
    private final LinkedHashMap<String, List<String>> columnToPointers = new LinkedHashMap<>();

    public DataMapping(Object table) {
        this.table = table;
    }

    /**
     * Add a trace-field binding: {@code varName} is the plotly trace key (e.g. "x", "y", "open"),
     * {@code column} is the Deephaven column name, and {@code traceIndex} is the position in the
     * {@code plotly.data} array that this binding refers to.
     */
    public void bind(int traceIndex, String varName, String column) {
        String pointer = "/plotly/data/" + traceIndex + "/" + varName;
        columnToPointers.computeIfAbsent(column, k -> new ArrayList<>()).add(pointer);
    }

    /** Convert to the wire-format dict. {@link Exporter} hands out the integer table ID. */
    public Map<String, Object> toWireDict(Exporter exporter) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("table", exporter.reference(table));
        out.put("data_columns", new LinkedHashMap<>(columnToPointers));
        return out;
    }
}
