# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Environment setup

**Always activate the Python venv before running anything:**

```bash
source .venv/bin/activate
```

First-time setup (creates the venv, installs deps, configures pre-commit, tox):

```bash
python -m venv .venv
source .venv/bin/activate
pip install --upgrade -r requirements.txt
pre-commit install
python tools/plugin_builder.py --configure=full   # or =min for the minimum
```

`npm install` from the root installs JS deps (npm workspaces are rooted at `./plugins/*/src/js/`).

## Common commands

| Task | Command |
| ---- | ------- |
| Build & install a plugin (Python) | `python tools/plugin_builder.py --reinstall <plugin>` |
| Build & install with JS bundle | `python tools/plugin_builder.py -jr <plugin>` |
| Build, install, run server, watch | `python tools/plugin_builder.py -jrsw <plugin>` |
| Build docs for a plugin | `python tools/plugin_builder.py --docs <plugin>` |
| Python tests (single plugin) | `cd plugins/<plugin> && tox -e py3.12` |
| Single Python test | `tox -e py3.12 -- test.deephaven.ui.test_utils.UtilsTest.test_create_props -v` |
| All JS unit tests | `npm run test:unit` |
| JS unit tests for one plugin | `npm run test:unit -- --testPathPattern="plugins/<plugin>"` |
| JS lint (jest-runner-eslint) | `npm run test:lint` |
| Type check (whole repo) | `npm run types` |
| E2E tests (matches CI) | `npm run e2e:docker -- ./tests/<file>.spec.ts --reporter=list` |
| Update E2E snapshots | `npm run e2e:update-snapshots -- ./tests/<file>.spec.ts` |
| Local E2E UI mode | `npm run e2e:ui` |
| All plugins via Docker | `npm run docker` |
| Watch all JS plugins + dev server | `npm start` (optional `-- --scope *theme*`) |
| Preview docs locally | `npm run docs` (built docs: `BUILT=true npm run docs`) |

`plugin_builder.py` flag shorthands: `-i` install, `-r` reinstall, `-j` build JS, `-s` start server, `-w` watch, `-d` docs, `-sa` server-arg (e.g. `-sa --port=9999`).

E2E permission gotcha: if Playwright complains after a Docker run, `sudo rm -rf test-results`.

## Repository layout

This is a monorepo with a Python plugin and a JS plugin co-located per feature under `plugins/<name>/`:

- Python source: `plugins/<name>/src/deephaven/...` (e.g. `plotly-express` lives at `src/deephaven/plot/express` so it mirrors Plotly Express).
- JS source: `plugins/<name>/src/js/` (an npm workspace; package name is `@deephaven/js-plugin-<name>`).
- Each plugin has its own `pyproject.toml`, `setup.cfg`, `tox.ini`, `package.json`, and `CHANGELOG.md`. Plugins are independently versioned and published.
- Top-level `tests/` holds Playwright E2E specs; per-plugin Python tests live in `plugins/<name>/test/`.
- `tools/plugin_builder.py` is the primary developer entrypoint — it wraps build, install, docs, server start, and watch into one CLI.

## Architecture

See `ARCHITECTURE.md` for the canonical write-up with sequence diagrams. Key model:

- **Object**: any Python global created by user code.
- **Server plugin**: registered at server startup. Server calls `is_type(obj)` on globals; the first matching plugin owns the object.
- **JS plugin**: registered at web IDE page load; renders the widget and talks to the server plugin.

Two server-plugin shapes:

- `FetchOnlyObjectType` — one-shot. Server calls `to_bytes(obj)` and ships the payload to the JS plugin's `fetch` prop. Use for static/read-only widgets.
- `BidirectionalObjectType` — `create_client_connection(obj, message_stream)` returns a `MessageStream` for inbound client→server messages and uses the supplied stream for server→client. Plugin is responsible for multi-client safety: copy state or enforce immutability, since the same object can have many concurrent client connections.

JS side: server-pushed messages arrive via `widget.addEventListener('message', fn)` (event `detail` carries payload + exported references). Client-to-server: `widget.sendMessage(payload, [references])` where references are tickets to other exported objects (e.g. tables).

Server plugins may export auxiliary widgets (typically tables) alongside the main payload — the JS plugin receives those references and fetches them as needed.

## JS plugin packaging conventions

- Built as a CJS bundle via Vite.
- Externalize `react`, `react-dom`, `redux`, `react-redux`, and any `@deephaven/*` packages you use (declare in `vite.config.ts` `rollupOptions.external`).
- Repo pins React 18 and uses `npm` `overrides` to force a single React copy across workspaces.

## Pre-commit / formatting

`pre-commit install` wires up Black, blacken-docs, pyright, and ruff for Python; ESLint/Prettier for JS run via Jest's lint config (`npm run test:lint`). Ruff config is in `ruff.toml`. If you must skip a hook, `git commit --no-verify` exists — but only when justified.

## Releases (cocogitto)

Releases use [`cog`](https://github.com/cocogitto/cocogitto) driven by Conventional Commits. Cuts happen weekly on Wednesday from `main`.

```bash
tools/check_changes.sh                  # what has changed since last release per plugin
tools/release.sh <pluginName>           # validates env, runs cog bump --auto --package <name>
tools/release.sh -r upstream <plugin>   # publish via a non-origin remote
cog bump --patch --package <plugin>     # force a patch bump
```

`cog bump` invokes `tools/update_version.sh` to rewrite the source-of-truth version files. **If you move where a plugin's version lives, update `tools/update_version.sh` or releases will break.**

When adding a new plugin: register it in `cog.toml`, `tools/update_version.sh`, and `.github/workflows/modified-plugin.yml`; add a PyPI pending publisher; ensure the `package.json` `repository` field is set. See `README.md` "Publishing a New Plugin" for the full checklist.

## Running against deephaven-core

Two paths:

1. **Docker (simplest):** `npm run docker` builds plugins and serves a deephaven-core instance at `http://localhost:10000`. JS plugins enabled via `docker/config/deephaven.prop`. Override port with `DEEPHAVEN_PORT=...`.
2. **Local source:** build wheels (`python -m build --wheel plugins/<name>`), `pip install --force-reinstall --no-deps <wheel>` into deephaven-core's venv, then start core with `-Ddeephaven.jsPlugins.@deephaven/js-plugin-<name>=<path>/plugins/<name>/src/js`. `npm start` serves JS plugins from Vite at `http://localhost:4100` so DHC/DHE can point at them and skip core restarts on JS changes.
