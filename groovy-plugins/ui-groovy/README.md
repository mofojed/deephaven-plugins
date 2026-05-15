# deephaven-plugin-ui-groovy

JVM-native (Java + Groovy) backend for the `deephaven.ui` widget. Produces the
same wire output as the existing Python plugin so the shipped JS plugin
(`@deephaven/js-plugin-ui`) works against it unchanged.

Build / test commands and overall layout live in the parent
[`groovy-plugins/`](../README.md) directory; this README covers what's
specific to this plugin.

Functional parity with the Python plugin's full hook + component surface:

- Hooks: `useState`, `useEffect`, `useCallback`, `useMemo`, `useRef`, `useSendEvent`,
  `useTableListener`, `useTableData`, `useRowData`, `useCellData`, `useColumnData`,
  `useBoolean`, `useRenderQueue`, `useExecutionContext`, `useQueryParams`,
  `useQueryParam`, `useSetQueryParam`, `useContext`, `useLivenessScope`
- Context: `Ui.createContext(default)` returns a `UiContext`; provide a value
  with `ctx.provider(value, children...)`; read with `Ui.useContext(ctx)`
- Components: nearly all Spectrum components (button, flex, view, picker, combo_box,
  date_picker, dialog, menu, tabs, slider, progress_bar, table, …)
- `Ui.dashboard(element)` for multi-panel layouts
- `Ui.toast(message, options)` for transient notifications
- `Ui.itemTableSource(table, …)` consumable by picker / comboBox / listView
- `Ui.tableAgg / tableFormat / tableDatabar / tableHeatmap` for table formatting
- `Html.*` for raw HTML elements (`Html.div`, `Html.h1`, `Html.p`, …)

## Example

```groovy
import io.deephaven.ui.Ui

myApp = Ui.component { ->
    def (count, setCount) = Ui.useState(0)
    Ui.flex(direction: 'column',
        Ui.text("Count: $count"),
        Ui.button("Increment", onPress: { setCount(count + 1) })
    )
}
```

## Install

Drop the produced JAR (and `zjsonpatch-*.jar` from the same `build/libs/` dir)
onto the Deephaven server's classpath (typically `/apps/libs/` on
`server-slim`). `META-INF/services/io.deephaven.plugin.Registration`
auto-discovers the plugin.

**Important: do not install both this plugin and the Python `deephaven-plugin-ui`
on the same server.** Both register the same Deephaven `ObjectType` names
(`deephaven.ui.Element`, `deephaven.ui.Dashboard`); having both on one server
will fail at registration. Install exactly one.

## Local dev — `run/`

A docker-compose harness ships in `run/` for hand-testing widgets. From the
parent `groovy-plugins/` directory:

```
./gradlew :ui-groovy:build    # if you haven't already
cd ui-groovy/run
docker compose up             # starts server on http://localhost:10000
```

Then open <http://localhost:10000>. In the file panel on the left you'll see
the demo widgets defined in `app.d/*.groovy` — double-click any of them to
render. Edit the `.groovy` files and restart the container to try changes.

`docker compose down` to stop.

## End-to-end tests — `tests/`

A separate docker-compose harness in `tests/` runs the repo's existing
Playwright suite (`tests/*.spec.ts`) against a Groovy-mode server. See
`tests/docker-compose.yml` for details. The fixtures under `tests/app.d/` are
Groovy ports of the Python test scripts in the repo-level `tests/app.d/`.

## JS bundle

The JS plugin (`@deephaven/js-plugin-ui`) is reused unchanged from the Python
plugin's build output. It's copied from `../../plugins/ui/src/deephaven/ui/_js/dist`
into this JAR's resources at build time. If that directory doesn't exist (because
the Python plugin hasn't been built yet), pass
`-PjsBundleSource=<dir>` to override the source.
