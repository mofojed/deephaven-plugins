package io.deephaven.plot.express.figure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One-element placeholder values that plotly.express bakes into a freshly-built figure's trace
 * data fields. The JS client overwrites these with real Deephaven column data once it follows
 * the {@link DataMapping} pointers — so the placeholder only matters for byte-level wire-format
 * parity, not for what the user sees.
 *
 * <p>Shapes captured from plotly 6.5.2 + the Python plugin's {@code construct_min_dataframe}:
 * <ul>
 *   <li>int32 columns → {@code {"dtype":"i4","bdata":"AAAAgA=="}} (the 4-byte little-endian
 *       Deephaven NULL_INT sentinel).</li>
 *   <li>int64 columns → plain list {@code [Long.MIN_VALUE]} (plotly's to_json bypasses bdata
 *       for small integer arrays of this size).</li>
 *   <li>String/categorical columns → plain list {@code ["None"]}.</li>
 *   <li>Double columns → {@code {"dtype":"f8","bdata":"<8 little-endian NaN bytes>"}}.</li>
 * </ul>
 */
public final class Placeholder {

    public enum Kind { INT, LONG, DOUBLE, STRING }

    private Placeholder() {}

    /** Produce a single-element placeholder of the given kind, byte-compatible with plotly's to_json. */
    public static Object of(Kind kind) {
        switch (kind) {
            case INT:
                return bdataMap("i4", "AAAAgA==");
            case LONG:
                return Collections.singletonList(Long.MIN_VALUE);
            case DOUBLE:
                // 8-byte little-endian NaN bytes — plotly emits f8 bdata for double columns.
                return bdataMap("f8", "AAAAAAAA+H8=");
            case STRING:
            default:
                return Collections.singletonList("None");
        }
    }

    private static Map<String, Object> bdataMap(String dtype, String bdata) {
        // Insertion order matters for byte-level diffs against the Python output.
        Map<String, Object> out = new LinkedHashMap<>(2);
        out.put("dtype", dtype);
        out.put("bdata", bdata);
        return out;
    }
}
