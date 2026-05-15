# syntax=docker/dockerfile:1
#
# Image: Deephaven server-slim with the ui-groovy plugin pre-installed. Pre-built JARs from
# {project_root}/groovy-plugins/ui-groovy/build/libs/ are copied into /apps/libs/ which is on
# server-slim's JVM classpath. The bundled JS plugin lives inside the main JAR as resources and
# is extracted at server startup by UiJsPlugin.
#
# Build prerequisites — run BEFORE `docker compose build` from the repo root:
#   1. npm ci && npm run build         (produces plugins/ui/src/deephaven/ui/_js/dist/index.js)
#   2. cd groovy-plugins && ./gradlew :ui-groovy:build  (produces ui-groovy/build/libs/*.jar)

FROM ghcr.io/deephaven/server-slim:edge

# Copy our jars into /apps/libs/ — server-slim's entrypoint adds that to the classpath.
# Context for this build is groovy-plugins/ui-groovy/ (see tests/docker-compose.yml).
COPY build/libs/ /apps/libs/
