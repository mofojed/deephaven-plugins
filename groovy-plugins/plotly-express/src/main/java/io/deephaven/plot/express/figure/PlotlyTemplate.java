package io.deephaven.plot.express.figure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * The plotly "plotly_white" theme template — the giant ~700-key Map that
 * {@code plotly.express} bakes into every figure's {@code layout.template}.
 *
 * <p>The Python plugin gets this for free from plotly itself; we ship a snapshot of it as a
 * resource. The snapshot was extracted from a {@code dx.scatter(...).to_dict()} call against the
 * Python plugin running plotly 6.5.2 — see this module's {@code build.gradle} / golden capture
 * script for how to refresh it.
 */
public final class PlotlyTemplate {

    private static final String RESOURCE_PATH = "/io/deephaven/plot/express/plotly_white_template.json";

    private static volatile Map<String, Object> cached;

    private PlotlyTemplate() {}

    public static Map<String, Object> get() {
        Map<String, Object> local = cached;
        if (local != null) {
            return local;
        }
        synchronized (PlotlyTemplate.class) {
            if (cached == null) {
                cached = load();
            }
            return cached;
        }
    }

    private static Map<String, Object> load() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = PlotlyTemplate.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Resource not found: " + RESOURCE_PATH);
            }
            return mapper.readValue(in, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load plotly_white template", e);
        }
    }
}
