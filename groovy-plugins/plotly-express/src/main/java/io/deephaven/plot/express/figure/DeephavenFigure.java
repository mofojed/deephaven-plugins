package io.deephaven.plot.express.figure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side representation of a chart, holding the placeholder plotly figure tree plus a list
 * of {@link DataMapping}s that tie the figure's trace fields to live Deephaven columns.
 *
 * <p>Mirrors the wire shape from
 * {@code plot/express/deephaven_figure/DeephavenFigure.py:to_json}:
 *
 * <pre>{@code
 * { "plotly": { "data": [...], "layout": {...} },
 *   "deephaven": {
 *     "mappings": [...],
 *     "is_user_set_template": false,
 *     "is_user_set_color": false
 *   } }
 * }</pre>
 */
public final class DeephavenFigure {

    private final Map<String, Object> plotlyFigure;
    private final List<DataMapping> dataMappings;
    private final boolean isUserSetTemplate;
    private final boolean isUserSetColor;

    public DeephavenFigure(
            Map<String, Object> plotlyFigure,
            List<DataMapping> dataMappings,
            boolean isUserSetTemplate,
            boolean isUserSetColor) {
        this.plotlyFigure = plotlyFigure;
        this.dataMappings = dataMappings;
        this.isUserSetTemplate = isUserSetTemplate;
        this.isUserSetColor = isUserSetColor;
    }

    /** Produce the {@code "figure"} field of a NEW_FIGURE message. Assigns table IDs via the exporter. */
    public Map<String, Object> toWireDict(Exporter exporter) {
        Map<String, Object> deephaven = new LinkedHashMap<>();
        List<Map<String, Object>> mappings = new ArrayList<>(dataMappings.size());
        for (DataMapping m : dataMappings) {
            mappings.add(m.toWireDict(exporter));
        }
        deephaven.put("mappings", mappings);
        deephaven.put("is_user_set_template", isUserSetTemplate);
        deephaven.put("is_user_set_color", isUserSetColor);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("plotly", plotlyFigure);
        payload.put("deephaven", deephaven);
        return payload;
    }

    // Visible for testing.
    public Map<String, Object> plotlyFigure() {
        return plotlyFigure;
    }

    public List<DataMapping> dataMappings() {
        return dataMappings;
    }
}
