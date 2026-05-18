package io.deephaven.plot.express

import com.fasterxml.jackson.databind.ObjectMapper
import io.deephaven.plot.express.Express as Dx
import io.deephaven.plot.express.figure.DeephavenFigure
import io.deephaven.plot.express.figure.Exporter
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Builds each in-scope fixture from {@code tests/app.d/express.py} and diffs the resulting
 * {@code figure} dict against a golden JSON captured from the running Python plugin
 * (golden_*.json in src/test/resources/golden/).
 *
 * <p>The comparison ignores:
 * <ul>
 *   <li>{@code plotly.layout.template} — we ship that as a static resource, so it's always
 *       byte-identical and would dwarf the meaningful diff.</li>
 *   <li>placeholder values in {@code plotly.data[*].x / .y} — the JS plugin overwrites them
 *       via the data-column mapping anyway, so byte parity here doesn't affect rendering.</li>
 * </ul>
 *
 * <p>Whatever's left — trace structure, layout axes/titles, hovertemplate, mapping shape — IS
 * the contract the JS plugin depends on.
 *
 * <p>To avoid pulling the full engine onto the unit-test classpath, the {@code Table} dependency
 * is satisfied with a duck-typed stub that the reflective {@code ColumnTypeResolver} accepts.
 */
class FigureBuilderGoldenSpec extends Specification {

    @Shared
    ObjectMapper mapper = new ObjectMapper()

    /** Duck-typed table: exposes a getDefinition().getColumn(name).getDataType() chain. */
    @Shared
    Object source = stubTable(
            Categories: String.class,
            Values:     int.class,
            Values2:    int.class,
            Price:      double.class,
            Reference:  double.class,
    )

    @Shared
    Object ohlcSource = stubTable(
            Timestamp: java.time.Instant.class,
            Open:      double.class,
            High:      double.class,
            Low:       double.class,
            Close:     double.class,
    )

    @Unroll
    def "matches golden JSON for #fixtureName"() {
        when:
        DeephavenFigure fig = builder.call()
        Map<String, Object> wire = fig.toWireDict(new Exporter())
        Map<String, Object> golden = mapper.readValue(
                getClass().getResourceAsStream("/golden/${fixtureName}.json"), Map.class)

        then:
        assertFiguresEquivalent(wire, golden, fixtureName)

        where:
        fixtureName               | builder
        "golden_express_fig"      | { -> Dx.bar(source, x: "Categories", y: "Values") }
        "golden_scatter_fig"      | { -> Dx.scatter(source, x: "Values", y: "Values2") }
        "golden_title_fig"        | { -> Dx.scatter(source, x: "Values", y: "Values2", title: "Test Title") }
        "golden_line_plot"        | { -> Dx.line(source, x: "Values", y: "Values2") }
        "golden_ohlc_fig"         | { -> Dx.ohlc(ohlcSource, x: "Timestamp", open: "Open", high: "High", low: "Low", close: "Close") }
        "golden_candlestick_fig"  | { -> Dx.candlestick(ohlcSource, x: "Timestamp", open: "Open", high: "High", low: "Low", close: "Close") }
        "golden_express_indicator"| { -> Dx.indicator(source, value: "Values", title: "Indicator") }
        "golden_bar_x_fig"        | { -> Dx.bar(source, x: "Values") }
        "golden_bar_y_fig"        | { -> Dx.bar(source, y: "Values2") }
    }

    /** Comparison helper. Strips template + placeholder x/y data before diffing. */
    private void assertFiguresEquivalent(Map<String, Object> actual, Map<String, Object> expected, String name) {
        Map<String, Object> a = stripVolatile(actual)
        Map<String, Object> e = stripVolatile(expected)
        assert a == e :
                "Figure for ${name} diverges from golden:\n--- actual ---\n${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(a)}\n--- expected ---\n${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(e)}"
    }

    private Map<String, Object> stripVolatile(Map<String, Object> wire) {
        Map<String, Object> copy = new LinkedHashMap<>(wire)
        Map<String, Object> plotly = new LinkedHashMap<>((Map<String, Object>) copy.get("plotly"))
        Map<String, Object> layout = new LinkedHashMap<>((Map<String, Object>) plotly.get("layout"))
        layout.remove("template")
        plotly.put("layout", layout)

        // Strip placeholder data values from every well-known trace field — the JS plugin
        // replaces them via the mapping anyway, so byte parity here doesn't affect rendering.
        List<String> placeholderFields = ["x", "y", "open", "high", "low", "close", "value"]
        List<Map<String, Object>> data = ((List<Map<String, Object>>) plotly.get("data")).collect { trace ->
            Map<String, Object> t = new LinkedHashMap<>(trace)
            placeholderFields.each { t.remove(it) }
            return t
        }
        plotly.put("data", data)
        copy.put("plotly", plotly)
        return copy
    }

    /** Build a tiny stand-in object exposing {@code getDefinition().getColumn(name).getDataType()}. */
    private static Object stubTable(Map<String, Class<?>> typesByName) {
        def definition = new Object() {
            Object getColumn(String name) {
                if (!typesByName.containsKey(name)) {
                    throw new IllegalArgumentException("no such column: ${name}")
                }
                Class<?> type = typesByName[name]
                return new Object() {
                    Class<?> getDataType() { type }
                }
            }
        }
        return new Object() {
            Object getDefinition() { definition }
        }
    }
}
