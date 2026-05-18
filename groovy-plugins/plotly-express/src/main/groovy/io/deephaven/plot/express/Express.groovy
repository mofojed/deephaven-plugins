package io.deephaven.plot.express

import io.deephaven.plot.express.builders.BarBuilder
import io.deephaven.plot.express.builders.CandlestickBuilder
import io.deephaven.plot.express.builders.IndicatorBuilder
import io.deephaven.plot.express.builders.LineBuilder
import io.deephaven.plot.express.builders.OhlcBuilder
import io.deephaven.plot.express.builders.ScatterBuilder
import io.deephaven.plot.express.figure.DeephavenFigure

/**
 * User-facing API for the Groovy port of the deephaven.plot.express plugin. Mirrors the Python
 * {@code dx.<fn>} signatures. Usage from a Groovy console:
 *
 * <pre>
 * import io.deephaven.plot.express.Express as Dx
 * def fig = Dx.scatter(myTable, x: 'Values', y: 'Values2', title: 'My plot')
 * </pre>
 *
 * <p>The {@code as Dx} alias mirrors Python's {@code import deephaven.plot.express as dx}
 * so call sites read identically across the two backends.
 *
 * <p>Each method accepts Groovy named-arg syntax: positional table first, named opts after.
 * Internally the named opts become the {@code Map} first parameter per Groovy convention.
 */
class Express {

    /** Scatter plot — single trace, mode=markers, type=scattergl. */
    static DeephavenFigure scatter(Map opts = [:], Object table) {
        new ScatterBuilder(table, opts).build()
    }

    /** Line plot — single trace, mode=lines, type=scattergl. */
    static DeephavenFigure line(Map opts = [:], Object table) {
        new LineBuilder(table, opts).build()
    }

    /** Bar plot. Accepts both x and y, or just one of them (count aggregation). */
    static DeephavenFigure bar(Map opts = [:], Object table) {
        new BarBuilder(table, opts).build()
    }

    /** OHLC financial chart. Requires x, open, high, low, close. */
    static DeephavenFigure ohlc(Map opts = [:], Object table) {
        new OhlcBuilder(table, opts).build()
    }

    /** Candlestick financial chart. Requires x, open, high, low, close. */
    static DeephavenFigure candlestick(Map opts = [:], Object table) {
        new CandlestickBuilder(table, opts).build()
    }

    /** Single-value indicator widget. Requires value=; optional title=. */
    static DeephavenFigure indicator(Map opts = [:], Object table) {
        new IndicatorBuilder(table, opts).build()
    }
}
