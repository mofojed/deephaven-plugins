# deephaven.plot.express — Groovy Backend Handoff

A JVM-native (Java + Groovy) port of the Python `deephaven.plot.express`
plugin. Lives on the `ui-groovy` branch at `groovy-plugins/plotly-express/`.
The JS plugin (`@deephaven/js-plugin-plotly-express` shipped from
`plugins/plotly-express/`) is reused unchanged — the Groovy backend is
wire-compatible.

## Why this exists

`deephaven.plot.express` today is Python-only and leans on the `plotly.express`
package to materialise a `plotly.graph_objs.Figure`, then attaches Deephaven
column mappings. The Deephaven server is JVM-based and is commonly driven by
Groovy users who have no equivalent charting API.

This plugin provides one. Critically, `plotly.express` is **not** available on
a Groovy server's classpath, so the Groovy backend constructs the
`{plotly, deephaven}` wire-format JSON directly from a snapshot of the
`plotly_white` template + per-plot trace builders — no plotly at runtime. The
shared JS plugin then renders identically regardless of console language.

Both backends register the same Deephaven `ObjectType` name
(`deephaven.plot.express.DeephavenFigure`); installs are mutually exclusive
(one or the other on a given server).

## Branch state

Branch `ui-groovy`, latest commit `ba3d08fb`:

| Commit | Summary |
|---|---|
| `ba3d08fb` | feat: add plotly-express Groovy plugin (11 fixtures, 9 spec tests) |

**9 Spock golden-JSON tests pass.** 11 of 20 fixtures from `tests/app.d/express.py`
are ported and visually verified end-to-end against a
`ghcr.io/deephaven/server-slim:edge` container via
`groovy-plugins/plotly-express/tests/docker-compose.yml`.

## What's done

### Coverage

11 fixtures from `tests/app.d/express.py` are working (same variable names so
the existing `tests/express.spec.ts` targets the Groovy backend unchanged):

| Fixture | Builder | Notes |
|---|---|---|
| `express_fig` | `BarBuilder` | basic vertical bar, x + y |
| `scatter_fig` | `ScatterBuilder` | scattergl, mode=markers |
| `title_fig` | `ScatterBuilder` | + `title:` arg |
| `line_plot` | `LineBuilder` | scattergl, mode=lines |
| `bar_x_fig` | `BarBuilder` | only `x` → `count_by` aggregation on y |
| `bar_y_fig` | `BarBuilder` | only `y` → `count_by` on x, orientation=h |
| `ohlc_fig` | `OhlcBuilder` | x + open/high/low/close |
| `candlestick_fig` | `CandlestickBuilder` | shares `OhlcLikeBuilder` parent |
| `express_indicator` | `IndicatorBuilder` | scalar value, no axes |
| `ticking_fig` | `BarBuilder` | refreshing Table — handled JS-side |
| `partitioned_fig` | `BarBuilder` (partitioned path) | multi-trace fan-out, one Table ref per constituent |

### Framework — `groovy-plugins/plotly-express/src/main/java/io/deephaven/plot/express/`
- `registration/ExpressRegistration` — `META-INF/services` entry,
  auto-discovered; registers `DeephavenFigureType` + `ExpressJsPlugin`
- `objecttype/`
  - `DeephavenFigureType` — extends `ObjectTypeBase`, `isType =
    instanceof DeephavenFigure`, type name matches Python exactly
  - `DeephavenFigureMessageStream` — collapsed
    `DeephavenFigureConnection` + `DeephavenFigureListener` into one class
    because static + simple ticking don't yet need the partition-meta
    listener loop. Handles `{"type":"RETRIEVE"}` → `{"type":"NEW_FIGURE",...}`
    and seeds with an initial NEW_FIGURE in `start()`. `FILTER` is a no-op.
  - `ClientConnection` + `SpiMessageStreamBridge` — copied verbatim from
    `ui-groovy` (only file in the module that touches the deephaven-core SPI)
- `figure/`
  - `DeephavenFigure` — holds `Map<String,Object>` plotly figure + list of
    `DataMapping` + `is_user_set_template` / `is_user_set_color` flags
  - `DataMapping` — column → `[/plotly/data/N/var]` pointers
  - `Exporter` — assigns int IDs to Table references for the
    `new_references` array
  - `PlotlyTemplate` — lazy-loads the
    `plotly_white_template.json` resource (captured from running Python
    plugin; ~775 lines)
  - `Placeholder` — single-element typed-NULL placeholders for trace data
    fields (`{dtype:i4, bdata:AAAAgA==}` for int, `[Long.MIN_VALUE]` for long,
    `["None"]` for string, NaN-bdata for double)
  - `ColumnTypeResolver` — reflective
    `table.getDefinition().getColumn(name).getDataType()` so unit tests can
    stub a table without engine JARs
  - `CountByHelper` — reflective `table.view([col]).countBy("count", col)`
    for the single-axis bar form; falls back to identity if reflection fails
    (unit tests)
  - `PartitionedTableHelper` — reflective `PartitionedTable` walk: detects
    via interface-name match, uses `table()` meta-table's `columnIterator(col)`
    to drain key + constituent columns
- `jsplugin/ExpressJsPlugin` — extracts the **entire** `bundle/` tree from JAR
  resources to a temp dir (the plotly bundle is a directory, not a single
  index.js); main() points at `bundle/index.js`

### Builders — `groovy-plugins/plotly-express/src/main/java/io/deephaven/plot/express/builders/`
| File | Notes |
|---|---|
| `AbstractFigureBuilder` | Shared scaffolding for the simple x/y single-trace family (scatter, line). Static `axis()` helper reused by Bar/Ohlc. |
| `ScatterBuilder` | type=scattergl, mode=markers, color=#636efa |
| `LineBuilder` | type=scattergl, mode=lines, adds `line: { color, dash, shape }` |
| `BarBuilder` | Single-trace + count_by + partitioned (3 build paths). Colorway constant for partition fan-out. Standalone; doesn't extend `AbstractFigureBuilder`. |
| `OhlcLikeBuilder` / `OhlcBuilder` / `CandlestickBuilder` | x + open/high/low/close, no axis titles, no legend |
| `IndicatorBuilder` | Scalar value placeholder (Integer.MIN_VALUE for int, NaN for double), domain=[0,1]², layout has margin.t=60 and no axes |

### Public DSL — `src/main/groovy/io/deephaven/plot/express/`
- `Express.groovy` — `static` methods: `scatter`, `line`, `bar`, `ohlc`,
  `candlestick`, `indicator`. All accept Groovy named args via the
  `(Map opts = [:], Object table)` convention so
  `Express.scatter(table, x: 'A', y: 'B')` reads naturally.

### Build & run
- Slots into the existing `groovy-plugins/` multi-project Gradle build as
  `plotly-express/build.gradle` + `include 'plotly-express'` in
  `settings.gradle`.
- `compileOnly` deps on deephaven-plugin + deephaven-engine-api +
  deephaven-Util at the version pinned in the root `build.gradle`; engine is
  provided at runtime.
- No `bundledRuntime` — Jackson is already on the server classpath, and the
  plugin doesn't use zjsonpatch.
- `copyJsBundle` task pulls the *entire*
  `plugins/plotly-express/src/deephaven/plot/express/_js/dist/` directory
  tree into JAR resources at `/io/deephaven/plot/express/js/dist/`.
- Root `groovy-plugins/build.gradle` gained an `:assembleAll` task that
  collects every subproject's `build/libs/*.jar` into
  `groovy-plugins/build/libs/`, so the root `docker-compose.yml` can bind
  a single directory and load both `ui-groovy` and `plotly-express` from one
  mount.
- `tests/docker-compose.yml` mirrors `ui-groovy/tests/`: same alias
  (`deephaven-plugins`) so the existing `tests/express.spec.ts` runs
  unchanged. Healthcheck uses `bash /dev/tcp/localhost/10000` because
  `server-slim` doesn't ship curl/wget/nc.
- `run/docker-compose.yml` is the focused-iteration dev harness for just
  this plugin; the combined two-plugin server lives at
  `groovy-plugins/docker-compose.yml`.
- `tests/app.d/express.groovy` — the 11 fixtures, mirrors variable names
  from `tests/app.d/express.py`.
- `groovy-plugins/app.d/` — `demo_ui.groovy` + `demo_plotly_express.groovy`
  for the combined dev server.

### Verification approach

Two layers (both fast):

1. **Spock golden-JSON diffing** —
   `src/test/groovy/io/deephaven/plot/express/FigureBuilderGoldenSpec.groovy`.
   Each fixture builds via the Groovy builder, calls `toWireDict(Exporter)`,
   and diffs against the byte-for-byte capture from a Python server in
   `src/test/resources/golden/golden_<name>.json`. The diff strips
   `plotly.layout.template` (huge, byte-identical from the shipped resource)
   and the placeholder x/y/open/high/low/close/value fields (replaced
   client-side via mappings, so don't affect rendering). A duck-typed stub
   table provides the `getDefinition().getColumn().getDataType()` chain to
   `ColumnTypeResolver` so the engine JARs aren't required on the test
   classpath.

2. **Fast iteration via direct iframe URL** —
   `http://localhost:10000/iframe/widget/?name=<fixtureName>` against the
   Groovy server. Skips the Panels-menu dance; one tab refresh per edit.
   This is how every fixture in the current commit was verified visually.

## How goldens were captured

Each `golden_<name>.json` came from running the Python plugin's
`<fixture>.get_figure().to_dict(Exporter())` against an in-process
`deephaven_server.Server()`. The Python server **must** not collide with a
running docker server on port 10000 — `docker compose down` first.

To re-capture (e.g. after a plotly version bump or new fixture):

```bash
cd /Users/bender/dev/deephaven/oss/deephaven-plugins
source .venv/bin/activate
docker compose -f groovy-plugins/plotly-express/tests/docker-compose.yml down  # free port 10000
python3 <<'EOF'
from deephaven_server import Server
Server().start()
from deephaven.column import int_col, string_col, double_col
from deephaven import new_table
import deephaven.plot.express as dx
from deephaven.plot.express.exporter import Exporter
import json
# ... build the source tables that match tests/app.d/express.py ...
# ... call dx.<plot>(...) for each fixture, then ...
inner = fig.get_figure()
with open('groovy-plugins/plotly-express/src/test/resources/golden/golden_<name>.json', 'w') as f:
    json.dump(inner.to_dict(Exporter()), f, indent=2)
EOF
```

`PlotlyTemplate` reads from
`src/main/resources/io/deephaven/plot/express/plotly_white_template.json`;
that file was extracted from one fixture's golden and now serves all of them
(it's the giant ~700-key plotly_white theme).

## What's remaining

### High value

1. **Refreshing-partitioned tables.** The current `BarBuilder.buildPartitioned`
   snapshots constituents once at build time. If the partitioned table is
   refreshing (new partitions added/removed at runtime), the figure goes
   stale. Mirror Python's `DeephavenFigureListener._on_update`:
   subscribe to the partition meta table via
   `Table.addUpdateListener` (needs the UpdateGraph shared lock — see the
   `LiveHooks.useTableListener` pattern in `ui-groovy`); on each tick, rebuild
   the figure via the same `BarBuilder.buildPartitioned` path; emit a fresh
   `NEW_FIGURE` with `revision++`. Should live in
   `DeephavenFigureMessageStream`. Track refresh state via
   `RevisionManager`-equivalent counter (Python has one,
   `plot/express/deephaven_figure/RevisionManager.py`).

2. **`histogram` builder.** Server-side binning via the Deephaven aggregation
   API (`countBy` over a calculated bucket column). `express_hist_by` in the
   fixture file uses `by="Categories"` so it's also a multi-trace fan-out —
   pattern matches `BarBuilder.buildPartitioned` but with binning preprocessing
   instead of a real partitioned table. Python reference:
   `plot/express/preprocess/HistPreprocessor.py`.

3. **Multi-series via list `y` and `by` on non-partitioned tables.** Python
   `dx.scatter(table, x="A", y=["B","C"])` and
   `dx.scatter(table, x="A", y="B", by="Group")` both fan out into multiple
   traces from a single regular Table. `marginal_scatter_fig` also needs this
   (rug + histogram marginals = 3 traces, layout has nested xaxis2/yaxis2).
   Builders would need a trace-generator pattern; mappings get a cartesian
   product. Python reference: `data_mapping/json_conversion.py:convert_to_json_links`.

### Medium value

4. **`make_subplots` / `titles_fig` / `keep_subplot_titles_fig`.** Combines
   multiple existing `DeephavenFigure` instances into one (Python: `subplots.py`,
   `_layer.py`). Layout fields like `grid: { rows, columns, pattern }`,
   `annotations: [...]` for subplot titles. Mappings are concatenated with an
   `axisN` offset per child figure.

5. **`timeline` builder.** Bar variant with `x_start` / `x_end` instead of
   `x`. Single trace, layout has datetime x axis. Mostly mechanical port of the
   existing BarBuilder.

6. **`line_calendar` — NYSE calendar axis.** Adds a top-level
   `deephaven.calendar` block to the wire payload describing trading-hour
   gaps so plotly compresses the time axis. Python reference:
   `plot/express/deephaven_figure/FigureCalendar.py`. The JS plugin reads this
   block and rewrites the x axis ticks accordingly.

7. **`express_indicator_by`.** Indicator + `by:` grouping → one indicator
   trace per partition, arranged in a grid. Layout uses `grid: { rows, cols }`.

### Low value / nice-to-have

8. **Spock test for the partitioned path.** Currently only the single-trace
   builders have golden tests; partitioned was verified via visual smoke.
   Would need either a real engine-table dependency on the test classpath
   (matches the trade-off `ui-groovy` made for live-data hooks) or a duck-
   typed stub that satisfies `PartitionedTableHelper.isPartitioned`'s
   interface-name check (currently strict on
   `io.deephaven.engine.table.PartitionedTable`).

9. **CI integration.** No GitHub Actions workflow exists for this plugin yet.
   When ready, mirror `ui-groovy`'s pattern: a workflow that runs
   `./gradlew :plotly-express:test` + the docker e2e suite against
   `tests/express.spec.ts`.

10. **Documentation pass.** No README in the subproject. A Python → Groovy
    cheatsheet (`dx.scatter(...)` → `Express.scatter(...)`) plus a list of
    supported plot kwargs per builder would help adoption.

11. **`bar` colorway customisation.** Python supports `color_discrete_sequence`,
    `color_discrete_map` for per-partition color overrides;
    `BarBuilder.buildPartitioned` hard-codes the default colorway.

## Gotchas — bugs that took the longest to find

These are the traps most likely to bite the next agent if they touch the same
areas:

1. **plotly-express's `index.js` is an ES module.** The deephaven web client
   loads plugins via CommonJS-style `require`. Pointing `JsPlugin.main()` at
   `index.js` fails in the browser with "Cannot use import statement outside
   a module". The bundled CommonJS entry is at `bundle/index.js`. The
   Python plugin's `package.json` exports map declares the same:
   `"require": "./dist/bundle/index.js"`. See
   `ExpressJsPlugin.MAIN_FILE`.

2. **The JS dist is a directory tree, not a single file.** Unlike
   `ui-groovy`'s `index.js`-only bundle, plotly ships
   `index.js`, `bundle/index.js`, `bundle/style.css`, all the `.d.ts`/`.js.map`
   files. The JsPlugin must extract every entry under the resource prefix —
   walk JarFile entries, don't enumerate a hardcoded short list. See
   `ExpressJsPlugin.extractFromJar`.

3. **`server-slim:edge` has no curl, wget, or nc.** The Compose healthcheck
   must use a binary the image ships. Bash's `/dev/tcp/<host>/<port>`
   pseudo-device works: `bash -c 'echo > /dev/tcp/localhost/10000'`.
   Borrow that pattern when adding new plugins.

4. **`DateTimeUtils.parseInstant` can't be called inside a Deephaven query
   formula.** The query-language parser fails with `Cannot find method
   parseInstant(java.time.Instant) in class DateTimeUtils`. Compute the
   instant in Groovy, push it through `QueryScope.addParam`, then reference
   the param in the formula. See `tests/app.d/express.groovy`'s
   `_baseInstant` setup for `ohlc_source`.

5. **Reflective `RowSet.iterator().nextLong()` silently returns zero rows.**
   The PartitionedTable's meta-table needs `Table.columnIterator(String)`
   (returns a CloseableIterator over the values) rather than RowSet
   iteration + ColumnSource lookups. The latter approach swallowed an
   exception in our first attempt and produced an empty figure. See
   `PartitionedTableHelper.columnValues`. **Don't catch
   ReflectiveOperationException silently** — rethrow so failures surface.

6. **`ObjectTypeBase` vs `ObjectTypeClassBase`.** Same as ui-groovy: use
   `ObjectTypeBase` so `isType` can do `instanceof DeephavenFigure`, not
   exact class equality. See `DeephavenFigureType`.

7. **Plugin name collision.** Both the Python and Groovy plugins register
   `deephaven.plot.express.DeephavenFigure`. Installing both on one server
   fails at registration. Noted in `ExpressRegistration` javadoc.

8. **Refreshing Tables don't need server-side work.** A ticking
   `Table` (non-partitioned) flows through the existing static-figure path
   verbatim. The JS plugin subscribes to the Table reference and updates
   the chart in place. This is **only** true while the column **set** is
   stable — if columns appear/disappear the figure structure goes stale.
   For `PartitionedTable` you have to re-emit (see "remaining" §1).

9. **Bar `orientation` depends on which axis is missing.** With only `y`
   supplied (no `x`), plotly emits `orientation: "h"` and draws horizontal
   bars; with only `x` it stays vertical. See `BarBuilder.buildSingleTrace`.

10. **Indicator's `value` field is a scalar, not an array.** Python plotly
    emits a plain JSON number for it (`-2147483648` for the int NULL),
    *not* a bdata struct. `IndicatorBuilder.scalarPlaceholder` mirrors that.
    Arrays use `{dtype, bdata}` only when long enough; short ones become
    plain lists (`[-9223372036854775808]` for a 1-element long array).

11. **OHLC / candlestick layouts have no `legend` and no axis titles.** The
    base `AbstractFigureBuilder` always adds them; `OhlcLikeBuilder` doesn't
    extend that and builds the layout from scratch. Don't try to retrofit
    OHLC onto `AbstractFigureBuilder` without making axis-titles + legend
    optional.

12. **Placeholder bdata byte-parity isn't load-bearing.** The JS plugin
    replaces every value covered by a `data_columns` mapping. So if you get
    `["None"]` instead of `["2000-01-01"]` for a date placeholder, the
    rendered chart is still pixel-identical — golden tests can strip the
    placeholder fields before diffing. See
    `FigureBuilderGoldenSpec.stripVolatile`.

13. **Spock test doesn't have engine-table on the classpath.** Tests stub
    tables via a duck-typed inline anonymous class implementing
    `getDefinition().getColumn(name).getDataType()`. `ColumnTypeResolver` is
    reflective so this works. `CountByHelper` and `PartitionedTableHelper`
    fall through to identity / empty when reflection fails, so the
    single-trace bar path still works in unit tests. The partitioned path
    isn't unit-tested for the same reason.

14. **Application Mode doesn't auto-export top-level vars in Groovy.** Same
    as ui-groovy: prefix locals with `def`, then call
    `ApplicationContext.get().setField("name", value, "description")` for
    each fixture you want visible. Skipping `def` makes the variable global
    AND show up in the Panels menu twice (once from each path), breaking
    Playwright's `getByRole({ exact: true })`.

15. **Groovy named args collapse to a leading Map.** Builders accept
    `(Map opts = [:], Object table)` so `Express.bar(table, x:'A', y:'B')`
    binds the named args to `opts`. Order matters: positional table comes
    second in the method signature, even though the caller writes it first.

## Key files

- **Gradle root (multi-project):** `groovy-plugins/`
- **Module root:** `groovy-plugins/plotly-express/`
- **MessageStream:**
  `src/main/java/io/deephaven/plot/express/objecttype/DeephavenFigureMessageStream.java`
- **Figure wire model:**
  `src/main/java/io/deephaven/plot/express/figure/DeephavenFigure.java`
- **Bar builder (all three paths):**
  `src/main/java/io/deephaven/plot/express/builders/BarBuilder.java`
- **Partitioned-table reflection:**
  `src/main/java/io/deephaven/plot/express/figure/PartitionedTableHelper.java`
- **Public API:** `src/main/groovy/io/deephaven/plot/express/Express.groovy`
- **Test fixtures (mirrors Python):** `tests/app.d/express.groovy`
- **Goldens:** `src/test/resources/golden/golden_*.json`
- **Embedded plotly_white theme:**
  `src/main/resources/io/deephaven/plot/express/plotly_white_template.json`
- **Python reference (read-only):** `plugins/plotly-express/src/deephaven/plot/express/`
  - Wire shape: `deephaven_figure/DeephavenFigure.py:to_json` (lines ~746-787)
  - How plotly.express output is captured + mappings attached:
    `deephaven_figure/generate.py:generate_figure` (~1042-1119)
  - Mapping shape: `data_mapping/json_conversion.py:json_link_mapping`
  - Request/response envelope + revision protocol:
    `communication/DeephavenFigureListener.py`
- **JS plugin contract (read-only):**
  `plugins/plotly-express/src/js/src/PlotlyExpressChart*.tsx` and
  `PlotlyExpressChartModel.ts`
- **Playwright spec (shared with Python):** `tests/express.spec.ts` and
  `tests/express.spec.ts-snapshots/`

## How to bring it up

```bash
cd groovy-plugins
./gradlew :plotly-express:test           # 9 Spock golden specs
./gradlew assembleAll                    # build/libs/ aggregates both plugins
docker compose up                        # http://localhost:10000, anonymous auth
# (combined server with both ui-groovy and plotly-express)
```

For focused iteration on just this plugin:

```bash
cd groovy-plugins
./gradlew :plotly-express:build          # plotly-express/build/libs/*.jar
cd plotly-express/run && docker compose up
```

For the e2e suite (Playwright against `tests/express.spec.ts`):

```bash
cd groovy-plugins/plotly-express/tests
docker compose run --rm e2e-tests express.spec.ts --grep "Express loads|Figure with title|Figure with scatter"
# Pass the spec file explicitly — without it the runner trips on theme.spec.ts
# which imports from a path that isn't mounted into the e2e container.
```

For fast smoke verification (single fixture without the Panels-menu dance):
hit `http://localhost:10000/iframe/widget/?name=<fixture_var_name>` directly.

Example smoke pattern used throughout this work:

```js
import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const page = await browser.newContext().then(c => c.newPage());
const errors = [];
page.on('pageerror', e => errors.push(e.message));
page.on('console', msg => msg.type() === 'error' && errors.push(msg.text()));
await page.goto('http://localhost:10000/iframe/widget/?name=scatter_fig');
await page.waitForTimeout(6000);
await page.screenshot({ path: '/tmp/smoke.png' });
console.log(errors.length === 0 ? 'OK' : errors.join('\n'));
await browser.close();
```
