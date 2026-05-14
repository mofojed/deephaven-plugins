# deephaven.ui — Groovy Backend Handoff

A JVM-native (Java + Groovy) port of the Python `deephaven.ui` plugin. Lives
on the `ui-groovy` branch at `groovy-plugins/ui-groovy/`. The JS plugin
(`@deephaven/js-plugin-ui` shipped from `plugins/ui/`) is reused unchanged —
the Groovy backend is wire-compatible.

## Why this exists

`deephaven.ui` today is Python-only. The Deephaven server is JVM-based and is
commonly driven by Groovy users who have no equivalent React-style UI API.
This plugin provides one, sharing the JS bundle so users get the identical
widget experience regardless of console language. Both backends register the
same Deephaven `ObjectType` names; installs are mutually exclusive (one or
the other on a given server).

## Branch state

Branch `ui-groovy`, 6 commits ahead of `main`, all green:

| Commit | Summary |
|---|---|
| `99a4a07b` | MVP framework + 10 components (25 tests) |
| `d0f2a44d` | ~60 more components + Html namespace (81 tests) |
| `51447f27` | Dashboard + toast / event channel (88 tests) |
| `f9856124` | `ui.table` + `item_table_source` + live-data hooks (97 tests) |
| `a068213e` | Move `plugins/ui-groovy` → `groovy-plugins/ui-groovy` |
| `5334964e` | Routing / context / util hooks (109 tests) |

**109 Spock unit tests pass.** All 12 demos in `groovy-plugins/ui-groovy/run/app.d/`
verified end-to-end in headless Chrome against a `ghcr.io/deephaven/server-slim:edge`
container brought up via `run/docker-compose.yml`.

## What's done

### Framework — `groovy-plugins/ui-groovy/src/main/java/io/deephaven/ui/`
- `element/` — `Element`, `BaseElement`, `FunctionElement`, `RenderedNode`,
  `DashboardElement`, `ContextProviderElement`, `UiContext`
- `render/` — `RenderContext` (ThreadLocal slot-based hooks, child contexts,
  effects, unmount listeners, open-cleanup callbacks); `Renderer`;
  `NodeEncoder` emitting `__dhElemName` / `__dhCbid` / `__dhObid` sentinels;
  `UiCallable`, `RootRenderContext`, `ExportedRenderState`
- `jsonrpc/` — hand-rolled `JsonRpcDispatcher` over Jackson
- `objecttype/` — `ElementMessageStream` (render loop, executor, JSON Patch
  diffing via `zjsonpatch`, captures `ExecutionContext` at construction and
  reopens on the render thread, opens an `EventContext` during work);
  `ElementType` extends `ObjectTypeBase` with `instanceof Element` matching;
  `DashboardType` matches `instanceof DashboardElement`; registered in that
  order (Dashboard before Element) via ServiceLoader
- `registration/UiRegistration` — `META-INF/services` entry, auto-discovered
- `jsplugin/UiJsPlugin` — extracts `index.js` from JAR resources to a temp dir
- `util/PropCase` — snake_case → camelCase, `UNSAFE_` / `aria_` preserved
- `event/EventContext` — ThreadLocal for `useSendEvent` / `Ui.toast`

### Hooks — `src/main/java/io/deephaven/ui/hook/`
| Hook | File |
|---|---|
| `useState`, `useEffect`, `useCallback`, `useMemo`, `useRef`, `useSendEvent`, `useBoolean`, `useRenderQueue`, `useContext` | `Hooks.java` |
| `useTableListener`, `useTableData`, `useRowData`, `useCellData`, `useColumnData` | `LiveHooks.java` |
| `useExecutionContext` | `ExecutionContextHooks.java` |
| `useQueryParams`, `useQueryParam`, `useSetQueryParam` | `RoutingHooks.java` |

Supporting types: `Ref`, `StateTuple`, `BooleanSetter`, `BooleanState` — all
`Iterable` so Groovy destructuring (`def (val, set) = ...`) works.

### Public DSL — `src/main/groovy/io/deephaven/ui/`
- `Ui.groovy` — `component { ... }`, every hook, ~70 Spectrum components
  (button, flex, view, picker, table, dashboard, …), `toast(opts, msg)`,
  `createContext(default)`, `tableAgg / tableFormat / tableDatabar /
  tableHeatmap`, `itemTableSource`, generic `componentElement`
- `Html.groovy` — 34 raw HTML tags (`Html.div`, `Html.h1`, …)

### Build & run
- Gradle 8.14.3 wrapper at `groovy-plugins/ui-groovy/gradlew`. Standalone,
  not yet integrated with any repo-root build.
- `compileOnly` deps on deephaven-plugin and deephaven-engine modules at
  0.39.6; server provides these at runtime.
- `bundledRuntime` configuration copies `zjsonpatch-0.4.16.jar` into
  `build/libs/` alongside the main JAR — the server classpath has Jackson +
  Groovy but not zjsonpatch.
- `copyJsBundle` task pulls
  `plugins/ui/src/deephaven/ui/_js/dist/index.js` into JAR resources.
- `run/docker-compose.yml` mounts `build/libs/*` → `/apps/libs/`, mounts
  `run/app.d/` → `/app.d/`, sets anonymous auth + `console.type=groovy`.
- `run/app.d/*.groovy` — 6 demo scripts: counter, showcase, dashboard_demo,
  toast_demo, table_demo, hooks_demo (12 `setField`'d widgets total).

## What's remaining

### High value
1. **`useLivenessScope`** — the only Python hook not yet ported. Needs
   `LivenessScope` plumbing in `RenderContext` (Python tracks
   `_collected_scopes`, `_top_level_scope`, exposes `manage(LivenessScope)` so
   derived live tables survive across renders). Without it, a derived live
   table created inside `useMemo`/`useCallback` may leak or be released
   prematurely. The current live-data hooks work because users pass a stable
   table reference; under sustained load with derived-table churn this could
   misbehave.
2. **Wire-format golden tests.** Capture `documentPatched` byte payloads from
   the Python plugin for a fixed scenario set, diff against Groovy output.
   Highest-value safety net for keeping the two backends interoperable as the
   JS plugin evolves.
3. **Liveness scope semantics beyond the hook.** `RenderContext` should retain
   `Table`s produced inside render functions across renders; Python collects
   them and releases stale ones each cycle. Same concern as item 1.

### Medium value
4. **`convert_date_props`.** Python helper that auto-converts `Date` /
   `Instant` / `LocalDate` props on `date_picker` / `date_field` / `calendar`.
   Components work without it but lose some prop-type acceptance.
5. **Hot-reload of `app.d` scripts.** Currently requires
   `docker compose restart` on every script edit. A console command or file
   watcher would tighten the dev loop.
6. **Maven Central publish pipeline.** The Gradle module is standalone with
   no CI yet. Decide coordinates (`io.deephaven:deephaven-plugin-ui-groovy:<ver>`),
   versioning relative to deephaven-core, and wire a publish task.
7. **Coexistence-with-Python flag.** Today both plugins register the same
   ObjectType names, so installs are mutually exclusive. If users need both
   on one server, add the `deephaven.ui.groovy.enabled` system property gate
   originally proposed in the MVP plan.
8. **Playwright integration with `tests/`.** The existing `tests/` harness only
   covers Python widgets. Add a `tests/ui_groovy.spec.ts` (or similar) that
   drives `groovy-plugins/ui-groovy/run/` so regressions land in CI.

### Low value / nice-to-have
9. **Hook-count mismatch handling.** `RenderContext.OpenScope.close` swallows
   `RuntimeException` from effects/cleanups, masking the
   "Expected N hooks, got M" error. Let that one escape.
10. **`useEffect` cleanup ordering edge cases.** Current port is correct for
    the demos but should be stress-tested with multiple effects and
    interleaved dep changes.
11. **Documentation pass.** README is minimal; a Python → Groovy cheatsheet
    plus a "common patterns" doc would help adoption.

## Gotchas — bugs that took the longest to find

These are the traps most likely to bite the next agent if they touch the same
areas:

1. **`ObjectTypeClassBase.isType()` does exact class equality**, not
   `instanceof`. Use `ObjectTypeBase` directly and implement `isType` with
   `instanceof`. See `ElementType.java` comments.
2. **`addUpdateListener` requires the UpdateGraph shared lock.** Without it,
   the call succeeds silently but the listener never fires (an
   `UpdateGraphConflictException` is thrown into a swallowing catch). See
   `LiveHooks.useTableListener` for the `lockCloseable()` pattern.
3. **The render thread needs the right `ExecutionContext` open.** Capture at
   `ElementMessageStream` construction (when the SPI call has a real context
   attached) and reopen on the executor thread inside `processCallableQueue`.
   Otherwise `table.getUpdateGraph()` sees a `PoisonedUpdateGraph`.
4. **`useEffect` unmount listeners must be stabilized.** A plain `() -> ...`
   lambda is a fresh instance per render; `RenderContext.OpenScope.close`
   treats "old listener not present in the new set" as unmount and tears down
   the effect every render. Wrap in `useCallback(..., List.of())`.
5. **zjsonpatch isn't on the server classpath** — it must be bundled
   alongside the main JAR. The `bundledRuntime` configuration handles this;
   don't accidentally drop it.
6. **`UiJsPlugin.main()` must return a relative `Path`**, not absolute.
   Returning an absolute path puts it into the manifest verbatim and breaks
   the URL the web client builds.
7. **JS toast requires `variant`.** Python defaults it to `"neutral"`;
   `Ui.toast` does the same. Don't strip that default.
8. **Groovy named args collapse to a leading Map.** Methods accepting both
   named and positional args need `(Map opts, T positional)` *and* a
   `(T positional)` overload calling `method([:], positional)`. See
   `Ui.toast`, `Ui.table`, `Ui.itemTableSource`.
9. **Groovy multi-assign needs `Iterable`.** `Map.Entry` doesn't destructure;
   `StateTuple` and `BooleanState` implement `Iterable<Object>` to make
   `def (a, b) = ...` work.
10. **`GString` is not a `String`.** `NodeEncoder.isPrimitive` checks
    `CharSequence` (not just `String`) and coerces via `toString()` before
    encoding so `"Count: $count"` user code becomes a JSON string, not an
    exported object reference.
11. **Application Mode doesn't auto-export top-level vars in Groovy.** Each
    widget needs an explicit
    `ApplicationContext.get().setField("name", value, "description")` to show
    up in the file panel. Python's deephaven session has a hook that exposes
    module globals automatically; Groovy doesn't.
12. **`useEffect` deps comparison.** A fresh `ArrayList` is built each render;
    `Objects.equals(prevList, newList)` compares contents via
    `AbstractList.equals`, so reference inequality between list instances is
    fine. But if a dep is a derived `Table` whose `.equals` is identity-only,
    the effect re-runs every render — usually harmless but worth knowing.

## Key files

- **Module root:** `groovy-plugins/ui-groovy/`
- **Render loop:** `src/main/java/io/deephaven/ui/objecttype/ElementMessageStream.java`
- **State plumbing:** `src/main/java/io/deephaven/ui/render/RenderContext.java`
- **Wire format:** `src/main/java/io/deephaven/ui/render/NodeEncoder.java`
- **Public API:** `src/main/groovy/io/deephaven/ui/Ui.groovy`
- **Hooks:** `src/main/java/io/deephaven/ui/hook/{Hooks,LiveHooks,RoutingHooks,ExecutionContextHooks}.java`
- **Python reference (read-only):** `plugins/ui/src/deephaven/ui/`
- **JS plugin contract (read-only):**
  `plugins/ui/src/js/src/widget/{WidgetHandler.tsx, WidgetTypes.ts, WidgetJsonPatch.ts}`
  and `plugins/ui/src/js/src/elements/utils/ElementUtils.tsx`

## How to bring it up

```bash
cd groovy-plugins/ui-groovy
./gradlew test               # 109 Spock specs
./gradlew build              # produces build/libs/{main,zjsonpatch}.jar
cd run && docker compose up  # http://localhost:10000, anonymous auth
```

Then open the IDE and double-click any of the registered widgets: `counter`,
`showcase`, `myDashboard`, `toastDemo`, `tableDemo`, `pickerFromTable`,
`liveDemo`, `booleanDemo`, `backgroundDemo`, `routingDemo`, `contextDemo`,
`sanityTable`.

For headless verification, the pattern used throughout this work is a small
Playwright script driven from the repo root (where `tests/` already has
Playwright installed). Example:

```js
import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const page = await browser.newContext().then(c => c.newPage());
await page.goto('http://localhost:10000/iframe/widget/?name=counter');
await page.waitForSelector('text=Count: 0');
await page.click('button:has-text("Increment")');
// inspect page.locator('body').innerText() etc.
await browser.close();
```
