// Groovy port of tests/app.d/express.py — same exported variable names so the existing
// tests/express.spec.ts can target the Groovy backend unchanged.
//
// Covered this milestone:
//   express_fig, scatter_fig, title_fig, line_plot — basic single-trace x/y plots
//   bar_x_fig, bar_y_fig                            — bar with count_by aggregation
//   ohlc_fig, candlestick_fig                       — financial single-trace plots
//   express_indicator                               — single-value indicator widget
//   ticking_fig                                     — bar over a refreshing Table (no special
//                                                     server-side handling: the JS plugin
//                                                     subscribes to the Table reference)
//   partitioned_fig                                 — bar over a PartitionedTable, one trace per
//                                                     constituent table, colorway-rotated
//
// Still deferred: histogram, indicator_by, timeline, marginal, subplots, calendar,
// refreshing-partitioned (the partition meta listener loop is not yet wired), line_plot_img
// (depends on .to_image()/ui.image()).

import io.deephaven.appmode.ApplicationContext
import io.deephaven.engine.context.QueryScope
import io.deephaven.engine.util.TableTools
import io.deephaven.plot.express.Express as Dx
import io.deephaven.time.DateTimeUtils

// Build a source table matching express_source in tests/app.d/express.py.
def express_source = TableTools.emptyTable(3).update(
        "Categories = (i == 0) ? `A` : (i == 1) ? `B` : `C`",
        "Values = (int)(i * 2 + 1)",
        "Price = (double)(i * 2 + 1)",
        "Reference = 3.0d",
        "Values2 = (int)(i * 2 + 2)",
)

// Match the ohlc_source in the Python fixture: three rows at 8/9/10 AM ET on 2021-07-04 with
// Open/High/Low/Close columns. We compute the base instant in Groovy (the query-language parser
// in formulas can't resolve overloaded DateTimeUtils methods) and pass it through QueryScope.
QueryScope.addParam("_baseInstant", DateTimeUtils.parseInstant("2021-07-04T08:00:00 ET"))
def ohlc_source = TableTools.emptyTable(3).update(
        "Timestamp = _baseInstant + (long)(i * 3_600_000_000_000L)",
        "Open = (double)(i + 1)",
        "High = (double)(i + 2)",
        "Low = (double)(i) + 0.5d",
        "Close = (double)(i + 1) + 0.5d",
)

def express_fig = Dx.bar(express_source, x: "Categories", y: "Values")
def title_fig = Dx.scatter(express_source, x: "Values", y: "Values2", title: "Test Title")
def scatter_fig = Dx.scatter(express_source, x: "Values", y: "Values2")
def line_plot = Dx.line(express_source, x: "Values", y: "Values2")
def bar_x_fig = Dx.bar(express_source, x: "Values")
def bar_y_fig = Dx.bar(express_source, y: "Values2")
def ohlc_fig = Dx.ohlc(ohlc_source, x: "Timestamp", open: "Open", high: "High", low: "Low", close: "Close")
def candlestick_fig = Dx.candlestick(ohlc_source, x: "Timestamp", open: "Open", high: "High", low: "Low", close: "Close")
def express_indicator = Dx.indicator(express_source, value: "Values", title: "Indicator")

// Ticking source: merge static rows with a 1-second ticking table, take head(3) so the chart
// keeps refreshing as new ticks arrive. The wire format for a ticking Table is identical to a
// static Table — the JS plugin subscribes to the Table reference and updates the chart in place.
def express_view = express_source.view("Categories", "Values", "Values2")
def ticking_head = TableTools.timeTable("PT1s").view(
        "Categories = `A`",
        "Values = (int)10",
        "Values2 = (int)20",
)
def ticking_source = TableTools.merge(express_view, ticking_head).head(3)
def ticking_fig = Dx.bar(ticking_source, x: "Categories", y: "Values")

// Partitioned source: one constituent table per Categories value. The plugin fans out into one
// trace per partition, colors them from the colorway, and emits a separate Table reference for
// each constituent so the JS plugin can subscribe to each independently.
def partitioned_source = express_source.partitionBy("Categories")
def partitioned_fig = Dx.bar(partitioned_source, x: "Values", y: "Values2", by: "Categories")

// Groovy app mode doesn't auto-export top-level vars (Python does); each fixture needs an
// explicit setField call.
def app = ApplicationContext.get()
app.setField("express_fig", express_fig, "Basic bar chart")
app.setField("title_fig", title_fig, "Scatter with title")
app.setField("scatter_fig", scatter_fig, "Basic scatter chart")
app.setField("line_plot", line_plot, "Basic line chart")
app.setField("bar_x_fig", bar_x_fig, "Bar chart on x (count by Values)")
app.setField("bar_y_fig", bar_y_fig, "Bar chart on y (count by Values2)")
app.setField("ohlc_fig", ohlc_fig, "OHLC financial chart")
app.setField("candlestick_fig", candlestick_fig, "Candlestick financial chart")
app.setField("express_indicator", express_indicator, "Single-value indicator widget")
app.setField("ticking_source", ticking_source, "Underlying ticking table")
app.setField("ticking_fig", ticking_fig, "Bar chart on ticking table")
app.setField("partitioned_fig", partitioned_fig, "Bar chart fanned out per partition")
