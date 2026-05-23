# Deephaven JVM plugins

JVM-backed (Java + Groovy) plugins for the Deephaven server. This is a
multi-project Gradle build; one subdirectory per plugin module. Today the
modules are `ui-groovy/` (the JVM port of `deephaven.ui`) and
`plotly-express/` (the JVM port of `deephaven.plot.express`), but the wrapper,
toolchain pin, and common test infrastructure all live at this level so future
plugins can be added by dropping in a new subdirectory and a tiny `build.gradle`.

## Layout

```
groovy-plugins/
├── build.gradle           — shared subproject config (java 17 toolchain,
│                            groovy version, log backend on test classpath,
│                            spock + JUnit deps, deephavenVersion pin)
├── settings.gradle        — multi-project root; lists modules
├── gradlew, gradle/       — Gradle wrapper (8.x)
├── ui-groovy/
    ├── build.gradle       — plugin-specific deps and tasks
    ├── src/               — Java + Groovy sources
    ├── run/               — local docker-compose harness for hand-testing
    ├── tests/             — Playwright e2e harness (uses repo-level tests/)
    └── README.md
└── plotly-express/
   ├── build.gradle       — plugin-specific deps and tasks
   ├── src/               — Java + Groovy sources
   ├── run/               — local docker-compose harness for hand-testing
   ├── tests/             — Playwright e2e harness (uses repo-level tests/)
   └── README.md
```

## Build / test

From this directory:

```
./gradlew :ui-groovy:test     # run a module's Spock unit tests
./gradlew :ui-groovy:build    # produce build/libs/<plugin>-<version>.jar
./gradlew :plotly-express:build
./gradlew assembleAll         # copy all plugin jars to groovy-plugins/build/libs
./gradlew build               # build everything
```

## Run a dev server

```
./gradlew assembleAll         # collect all plugin jars into groovy-plugins/build/libs
docker compose up             # Deephaven server on http://localhost:10000
```

The `docker-compose.yml` in this directory loads both Groovy plugins from
`groovy-plugins/build/libs` and exists primarily to keep Compose v2 from walking
up the directory tree and using the repo-root `docker-compose.yml` — that one
builds the Python plugins image.

Each module's output JARs land under `<module>/build/libs/`. The
`bundledRuntime` configuration (when a module uses one — `ui-groovy` does)
also copies any non-server-provided runtime deps next to the main JAR.

If you run only a module build (for example `./gradlew :ui-groovy:build`), the
JAR will be present in that module's `build/libs`, but the combined
`groovy-plugins/docker-compose.yml` mount will not see it until you run
`./gradlew assembleAll`.

## Adding a new plugin

1. Create `groovy-plugins/<plugin-name>/` with a `build.gradle` that declares
   only the deps and tasks specific to that plugin. The shared `subprojects {}`
   block in the root `build.gradle` already applies the `groovy` /
   `java-library` plugins, the Java toolchain, common test wiring, and a logger
   backend on the test classpath.
2. Add `include '<plugin-name>'` to `settings.gradle`.
3. Optionally set `base { archivesName = '<published-artifact-name>' }` in the
   subproject's build.gradle if you want a different artifact name than the
   directory name (see `ui-groovy/build.gradle` for an example).

## Installing on a Deephaven server

Each module produces one or more JARs. Mount them onto the Deephaven server's
classpath — for `server-slim` that's `/apps/libs/*`. ServiceLoader
auto-discovery in `META-INF/services/io.deephaven.plugin.Registration` does
the rest.

## Putting it together — a ticking dashboard

With both plugins installed you can build an interactive `Ui.dashboard` whose
panels are driven by a single ticking source. The example below defines:

- a `prices` ticking table generating four updates per second across three
  tickers (`AAPL`, `GOOG`, `MSFT`);
- a control panel with a **picker** to choose the ticker and a **slider**
  to pick the tail window size;
- a live line chart, a single-value indicator showing the latest price,
  and the windowed source table — all driven from the same component state.

Paste this into a Groovy console against a server with both plugins loaded
(e.g. `docker compose up` from this directory), then open
<http://localhost:10000/iframe/widget/?name=live_dashboard>.

```groovy
import io.deephaven.engine.context.QueryScope
import io.deephaven.engine.util.TableTools
import io.deephaven.plot.express.Express as Dx
import io.deephaven.ui.Ui

// Shared ticking source — three tickers, each wandering around a different
// midpoint. The query language can't index a Groovy list directly, so we hand
// it a String[] via QueryScope.
QueryScope.addParam("_tickers", ["AAPL", "GOOG", "MSFT"] as String[])
prices = TableTools.timeTable("PT0.25s").update(
    "Ticker = _tickers[(int)(i % 3)]",
    "Price = (Ticker == `AAPL` ? 180.0 : Ticker == `GOOG` ? 140.0 : 410.0) " +
        "+ 5.0 * Math.sin(i * 0.05) + (Math.random() - 0.5) * 2.0",
)

live_dashboard = Ui.dashboard(
    Ui.component { ->
        def (ticker, setTicker) = Ui.useState("AAPL")
        def (windowSize, setWindowSize) = Ui.useState(100)

        // Re-filter the ticking source whenever the controls change. useMemo
        // keeps the derived Table stable across renders that don't touch its
        // dependencies — important so the JS plugin doesn't re-subscribe on
        // every keystroke.
        def filtered = Ui.useMemo({
            prices.where("Ticker = `" + ticker + "`").tail(windowSize as int)
        }, [ticker, windowSize])

        def lineFig = Ui.useMemo({
            Dx.line(filtered, x: "Timestamp", y: "Price", title: ticker + " price")
        }, [filtered, ticker])

        def latest = Ui.useMemo({ filtered.tail(1) }, [filtered])
        def indicatorFig = Ui.useMemo({
            Dx.indicator(latest, value: "Price", title: ticker + " last")
        }, [latest, ticker])

        Ui.row(
            Ui.panel(title: "Controls", width: 25,
                Ui.flex(direction: "column", gap: "size-200",
                    Ui.picker(label: "Ticker", selectedKey: ticker,
                        onSelectionChange: { k -> setTicker(k as String) },
                        Ui.item("AAPL", key: "AAPL"),
                        Ui.item("GOOG", key: "GOOG"),
                        Ui.item("MSFT", key: "MSFT")),
                    Ui.slider(label: "Window size", value: windowSize,
                        minValue: 20, maxValue: 500, step: 20,
                        onChange: { v -> setWindowSize(v as int) }),
                    Ui.text("Showing last " + windowSize + " ticks of " + ticker + "."),
                ),
            ),
            Ui.column(width: 75,
                Ui.panel(title: "Live price", height: 70, lineFig),
                Ui.row(height: 30,
                    Ui.panel(title: "Last", indicatorFig),
                    Ui.panel(title: "Window", filtered),
                ),
            ),
        )
    }
)
```

A few things worth noting:

- The component is wrapped in `Ui.dashboard(...)`, so it opens as a
  multi-panel layout instead of inside a single tab. `Ui.row` / `Ui.column`
  / `Ui.panel` map to Golden Layout rows, columns, and resizable panels.
- The same `filtered` table is passed both to `Dx.line(...)` and rendered
  directly inside a panel. The plotly-express JS plugin and the table
  widget each subscribe to it independently and update in place as new
  ticks arrive.
- Avoid Groovy `GString` interpolation (`"Ticker = \`${ticker}\`"`) inside
query-language formulas and figure titles — the underlying APIs expect
plain `String`. Plain `+` concatenation (as above) is the safest option.
- Multi-series fan-out on a _ticking_ `PartitionedTable` is not yet
  supported (see [`plotly-express/README.md`](plotly-express/README.md));
  filter to a single series in the component as shown above.
