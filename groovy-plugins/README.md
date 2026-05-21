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
