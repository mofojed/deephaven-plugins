# deephaven-plugin-plotly-express (Groovy)

JVM-native (Java + Groovy) backend for the `deephaven.plot.express` plugin.
Produces the same wire output as the existing Python plugin so the shipped JS
plugin (`@deephaven/js-plugin-plotly-express`) works against it unchanged.

Build / test commands and overall layout live in the parent
[`groovy-plugins/`](../README.md) directory; this README covers what's
specific to this plugin.

## API

A single user-facing class, `io.deephaven.plot.express.Express`, mirrors the
Python `deephaven.plot.express` (`dx`) surface. Import it aliased so call
sites read identically across the two backends:

```groovy
import io.deephaven.plot.express.Express as Dx
```

Currently exported figure constructors:

| Method                                                          | Notes                                                 |
| --------------------------------------------------------------- | ----------------------------------------------------- |
| `Dx.scatter(table, x:, y:, by:, title:)`                        | Single-trace `scattergl` markers.                     |
| `Dx.line(table, x:, y:, by:, title:)`                           | Single-trace `scattergl` lines.                       |
| `Dx.bar(table, x:, y:, by:, title:)`                            | Pass either `x` or `y` alone for a count aggregation. |
| `Dx.ohlc(table, x:, open:, high:, low:, close:, title:)`        | OHLC financial chart.                                 |
| `Dx.candlestick(table, x:, open:, high:, low:, close:, title:)` | Candlestick financial chart.                          |
| `Dx.indicator(table, value:, title:)`                           | Single-value KPI widget.                              |

Each method accepts Groovy named-arg syntax: the positional `table` first,
named options after. Pass a `Table` for a single-trace plot, or a
`PartitionedTable` (with `by:` naming the partition column) to fan out one
trace per constituent — automatically colorway-rotated.

## Examples

The snippets below run in a Groovy console against a Deephaven server with
this plugin installed (see [Local dev](#local-dev--run) for a one-command
harness). Each top-level binding becomes a widget; open it directly at
<http://localhost:10000/iframe/widget/?name=> &lt;binding&gt;.

### Basic scatter

```groovy
import io.deephaven.engine.util.TableTools
import io.deephaven.plot.express.Express as Dx

src = TableTools.emptyTable(20).update("X = (int)i", "Y = (int)(i * i)")
scatter_fig = Dx.scatter(src, x: "X", y: "Y", title: "y = x²")
```

### Bar — count aggregation

Pass only `x` (or only `y`) and the plugin counts rows per category:

```groovy
events = TableTools.emptyTable(30).update(
    "Category = (i % 4 == 0) ? `Alpha` : (i % 4 == 1) ? `Beta` : (i % 4 == 2) ? `Gamma` : `Delta`"
)
bar_count = Dx.bar(events, x: "Category", title: "Events per category")
```

### Candlestick

```groovy
import io.deephaven.engine.context.QueryScope
import io.deephaven.time.DateTimeUtils

QueryScope.addParam("_t0", DateTimeUtils.parseInstant("2024-01-02T09:30:00 ET"))
ohlc_src = TableTools.emptyTable(20).update(
    "Timestamp = _t0 + (long)(i * 60_000_000_000L)",
    "Open = 100.0 + i * 0.5",
    "High = Open + 1.0 + Math.random()",
    "Low = Open - 1.0 - Math.random()",
    "Close = Open + (Math.random() - 0.5)",
)
candlestick_fig = Dx.candlestick(ohlc_src,
    x: "Timestamp", open: "Open", high: "High", low: "Low", close: "Close",
    title: "1-minute bars")
```

### Fan out by partition

`by:` against a `PartitionedTable` emits one trace per constituent, each
colored from the chart's colorway and given a matching legend entry:

```groovy
sales = TableTools.emptyTable(9).update(
    "Region = (i % 3 == 0) ? `North` : (i % 3 == 1) ? `South` : `East`",
    "Quarter = `Q` + ((int)(i / 3) + 1)",
    "Sales = (int)(50 + i * 7 + (i % 3) * 20)",
)
sales_fig = Dx.bar(sales.partitionBy("Region"),
    x: "Quarter", y: "Sales", by: "Region", title: "Sales by region")
```

### Ticking line

`timeTable` produces a refreshing source; the JS plugin subscribes to the
Table reference and the chart updates in place. `tail(n)` keeps the window
bounded:

```groovy
prices = TableTools.timeTable("PT0.5s").update(
    "Price = 100.0 + 10.0 * Math.sin(i * 0.1) + (Math.random() - 0.5) * 5.0",
).tail(150)
ticking_line = Dx.line(prices, x: "Timestamp", y: "Price", title: "Live price")
```

### Ticking bar

A faster-ticking source with a categorical column drives a bar chart that
redraws as new events arrive:

```groovy
ticking_events = TableTools.timeTable("PT0.2s").update(
    "Category = (i % 4 == 0) ? `Alpha` : (i % 4 == 1) ? `Beta` : (i % 4 == 2) ? `Gamma` : `Delta`",
    "Value = (int)(Math.random() * 100)",
).tail(200)
ticking_bar = Dx.bar(ticking_events, x: "Category", y: "Value",
    title: "Live values (last 200 events)")
```

> Multi-series fan-out on a _ticking_ `PartitionedTable` is not yet wired
> (the partition-meta listener loop is deferred). For now, use `by:` only on
> static partitioned tables; ticking sources should be plain `Table`s.

## Install

Drop the produced JAR onto the Deephaven server's classpath (typically
`/apps/libs/` on `server-slim`). `META-INF/services/io.deephaven.plugin.Registration`
auto-discovers the plugin.

**Important: do not install both this plugin and the Python
`deephaven-plugin-plotly-express` on the same server.** Both register the
same Deephaven `ObjectType` name (`deephaven.plot.express.DeephavenFigure`);
having both on one server will fail at registration. Install exactly one.

## Local dev — `run/`

A docker-compose harness ships in `run/` for hand-testing figures. From the
parent `groovy-plugins/` directory:

```
./gradlew :plotly-express:build    # if you haven't already
cd plotly-express/run
docker compose up                   # starts server on http://localhost:10000
```

The harness mounts `tests/app.d/` so the named fixtures from
`tests/app.d/express.groovy` (`scatter_fig`, `candlestick_fig`,
`ticking_fig`, `partitioned_fig`, …) are immediately available. Open any of
them directly with:

```
http://localhost:10000/iframe/widget/?name=<binding_name>
```

`docker compose down` to stop.

## End-to-end tests — `tests/`

A separate docker-compose harness in `tests/` runs the repo's existing
Playwright suite (`tests/express.spec.ts`) against a Groovy-mode server. The
fixtures under `tests/app.d/` are Groovy ports of the Python test scripts in
the repo-level `tests/app.d/`, with the same exported variable names so the
spec files target the Groovy backend unchanged.

## JS bundle

The JS plugin (`@deephaven/js-plugin-plotly-express`) is reused unchanged
from the Python plugin's build output. It's copied from
`../../plugins/plotly-express/src/deephaven/plot/express/_js/dist` into this
JAR's resources at build time. If that directory doesn't exist (because the
Python plugin hasn't been built yet), pass `-PjsBundleSource=<dir>` to
override the source.
