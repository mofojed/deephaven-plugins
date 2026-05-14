# deephaven-plugin-ui-groovy

JVM-native (Java + Groovy) backend for the `deephaven.ui` widget. Produces the
same wire output as the existing Python plugin so the shipped JS plugin
(`@deephaven/js-plugin-ui`) works against it unchanged.

Covers ~70 of the Python plugin's ~85 components plus the raw HTML elements:

- Hooks: `useState`, `useEffect`, `useCallback`, `useMemo`, `useRef`, `useSendEvent`
- Components: nearly all Spectrum components (button, flex, view, picker, combo_box,
  date_picker, dialog, menu, tabs, slider, progress_bar, …)
- `Ui.dashboard(element)` for multi-panel layouts
- `Ui.toast(message, options)` for transient notifications
- `Html.*` for raw HTML elements (`Html.div`, `Html.h1`, `Html.p`, …)

Deferred to a follow-up: `ui.table`, `ui.item_table_source`, live-data hooks
(`use_table_data`, `use_table_listener`, `use_row_data`, `use_cell_data`,
`use_column_data`), and routing hooks.

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

Drop the produced JAR onto the Deephaven server's classpath (typically
`${DEEPHAVEN_HOME}/server/lib/`). `META-INF/services/io.deephaven.plugin.Registration`
auto-discovers the plugin.

**Important: do not install both this plugin and the Python `deephaven-plugin-ui`
on the same server.** Both register the same Deephaven `ObjectType` names
(`deephaven.ui.Element`, `deephaven.ui.Dashboard`); having both on one server
will fail at registration. Install exactly one.

## Build / test

```
./gradlew test     # run the Spock unit tests (framework only, no server needed)
./gradlew build    # full build, produces build/libs/deephaven-plugin-ui-groovy-<version>.jar
```

## Run end-to-end in a local Deephaven server

A docker-compose harness ships in `run/`. From this directory:

```
./gradlew build           # if you haven't already
cd run
docker compose up         # starts server on http://localhost:10000
```

Then open <http://localhost:10000>. In the file panel on the left you'll see a
variable `counter` from `app.d/counter.groovy` — double-click to render the
widget, click "Increment", and watch the count update via the round-trip we
just built. Edit `app.d/counter.groovy` to try other components.

`docker compose down` to stop.

The JS bundle is copied from `../ui/src/deephaven/ui/_js/dist` into the JAR's
resources. If the Python plugin hasn't been built yet, that directory may not
exist; the copy task is tolerant and you can pass `-PjsBundleSource=<dir>` to
override the location.
