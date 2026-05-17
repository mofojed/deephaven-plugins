# syntax=docker/dockerfile:1
#
# Image: Deephaven server-slim with the plotly-express Groovy plugin pre-installed. Pre-built
# JARs from {project_root}/groovy-plugins/plotly-express/build/libs/ are copied into /apps/libs/
# which is on server-slim's JVM classpath. The bundled JS plugin lives inside the main JAR as
# resources and is extracted at server startup by ExpressJsPlugin.
#
# Build prerequisites — run BEFORE `docker compose build`:
#   1. Build the Python plugin's JS bundle (npm ci && npm run build inside plugins/plotly-express/)
#      so plugins/plotly-express/src/deephaven/plot/express/_js/dist/index.js exists.
#   2. cd groovy-plugins && ./gradlew :plotly-express:build
#      (produces plotly-express/build/libs/*.jar with the JS bundle baked in)

FROM ghcr.io/deephaven/server-slim:edge

COPY build/libs/ /apps/libs/
