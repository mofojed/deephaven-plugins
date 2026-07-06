# Docker Sandbox (sbx) Development Environment Plan

## Overview

This plan documents the approach for giving developers and their AI coding
agents a secure, isolated environment to build, run, and test the
deephaven-plugins repo. Agents can run with permissions auto-approved so they
can iterate freely, including running the Dockerized end-to-end tests, while the
host machine stays protected.

The environment is delivered with [Docker Sandboxes](https://docs.docker.com/ai/sandboxes/)
(`sbx`) rather than a VS Code Dev Container.

## Requirements

- Isolate the environment (including a Python venv) from the host.
- Let agents auto-approve nearly all commands so they can work unattended, while
  keeping risky remote operations (e.g. `git push`) gated.
- Start a Deephaven server with the plugins built and installed, reachable in the
  user's browser.
- Run Playwright end-to-end tests the same way `npm run e2e:docker` does, so
  snapshots match CI.
- No privileged docker-in-docker container.
- Host setup instructions documented in the README.

## Why Docker Sandboxes instead of a Dev Container

A Dev Container that runs `npm run e2e:docker` needs Docker inside it. The only
in-container options are a privileged docker-in-docker daemon or a shared host
Docker socket — the first is what we want to avoid, the second breaks isolation.

Docker Sandboxes solve this: each sandbox is a **microVM** (hypervisor
isolation, its own kernel) with **its own Docker daemon inside the VM**. Agents
can build and run containers — including the e2e compose stack — with no
privileged host container and no host socket sharing. Docker's own comparison
ranks this above docker-in-docker for isolation.

Trade-off: `sbx` is a CLI workflow (it runs an agent CLI inside the microVM),
not the VS Code IDE-in-container experience. In the default direct mode the
workspace is a filesystem passthrough at the same absolute path, so a normal
host VS Code window still reflects the agent's live edits.

## Key decisions

- **Full pivot to `sbx`**; no `.devcontainer` is added.
- **Mixin kit** (`kind: mixin`) committed to the repo so it layers onto any
  built-in agent — one kit works for both `claude` and `copilot`. No prebuilt
  template image or registry required; teammates only need the `sbx` CLI.
- **Direct mode**, with the venv at the in-repo `.venv`. It is treated as
  ephemeral and owned by the sandbox; host `.venv` protection is intentionally
  not a goal.
- **Both Claude Code and GitHub Copilot CLI** are supported and documented.
- Auto-approval is provided by the sandbox model itself (agents run in
  bypass/YOLO mode); the microVM boundary is the safety mechanism. `git push`
  is naturally gated because it only works if a GitHub token was stored on the
  host with `sbx secret set -g github`.

## Implementation

### Files added

| File                              | Purpose                                                                                                                    |
| --------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `sbx/deephaven-plugins/spec.yaml` | Mixin kit: install command, network allowlist, agent context.                                                              |
| `tools/sbx_setup.sh`              | Idempotent provisioning script (venv, deps, build + install plugins). Also runnable standalone.                            |
| `README.md` (new section)         | "Running in a Docker Sandbox (sbx)": host prerequisites, credentials, running agents, opening the IDE, e2e, VS Code notes. |

### `tools/sbx_setup.sh`

Idempotent. Resolves the repo root from its own location, then:

1. Optionally selects Node from `.nvmrc` via `nvm` (best effort).
2. Creates `.venv` if missing and activates it.
3. `pip install --upgrade -r requirements.txt` (+ optional
   `sphinx_ext/sphinx-requirements.txt`).
4. `pre-commit install` (when in a git checkout).
5. `npm ci`.
6. `python tools/plugin_builder.py --js --install`.

### `sbx/deephaven-plugins/spec.yaml`

- `commands.install`: `bash tools/sbx_setup.sh` as user `1000` (the `agent`
  user) in the mounted workspace.
- `network.allowedDomains`: pypi.org, files.pythonhosted.org,
  registry.npmjs.org, ghcr.io, \*.githubusercontent.com, github.com,
  mcr.microsoft.com, nodejs.org, raw.githubusercontent.com. (The `Balanced`
  policy already permits most; the explicit list keeps `Locked Down` usable.)
- `agentContext`: repo-specific tips (activate `.venv`, `plugin_builder.py`
  usage, start the server bound to `0.0.0.0`, `sbx ports` to open the IDE,
  `npm run e2e:docker`, tests, and the `git push` credential note).

## Usage summary

```shell
# Host prerequisites (Linux)
curl -fsSL https://get.docker.com | sudo REPO_ONLY=1 sh
sudo apt-get install docker-sbx
sudo usermod -aG kvm $USER && newgrp kvm
sbx login

# Optional credentials (stored on host, injected by proxy)
sbx secret set -g github -t "$(gh auth token)"
sbx secret set -g anthropic

# Run an agent with the kit
sbx run claude  --kit ./sbx/deephaven-plugins
sbx run copilot --kit ./sbx/deephaven-plugins

# Inside the sandbox: start the server bound to 0.0.0.0
python tools/plugin_builder.py -r -s -sa --host -sa 0.0.0.0 ui

# From the host: forward the port and open the IDE
sbx ports <sandbox-name> --publish 10000:10000
# http://localhost:10000/ide/

# Inside the sandbox: e2e like CI
npm run e2e:docker
npm run e2e:update-snapshots
```

## Verification

1. `sbx kit validate ./sbx/deephaven-plugins`.
2. `sbx run claude --kit ./sbx/deephaven-plugins` creates the sandbox and the
   install command completes.
3. Start the server bound to `0.0.0.0`, `sbx ports ... --publish 10000:10000`,
   confirm http://localhost:10000/ide/ loads with plugins.
4. `npm run e2e:docker -- ./tests/ui_dialog.spec.ts` passes; a full run shows no
   snapshot diffs (CI parity).
5. Repeat step 2 with `sbx run copilot`.

## Notes / verified facts

- `deephaven server` accepts `--host` (verified via `deephaven server --help`);
  `--host 0.0.0.0` is required for `sbx ports` forwarding.
- `.venv/` is already in the root `.gitignore`.
- The default agent templates are `-docker` variants, so a full Docker Engine
  runs inside the microVM automatically — no extra setup for `npm run e2e:docker`.
- E2E snapshots come from the pinned `mcr.microsoft.com/playwright:v1.44.1-jammy`
  image (plus `fonts-dejavu-core`) used by `tests/docker-compose.yml`, so the
  sandbox's own Node/OS versions do not affect snapshot fidelity.

## Known host / runtime issues (sbx v0.34.0 on Linux)

The kit itself validates cleanly (`sbx kit validate` passes, `sbx kit inspect`
normalizes correctly). Live sandbox creation on this Linux host was blocked by
two environment issues that are independent of the kit:

1. **`docker0` subnet conflict.** The host Docker daemon's default bridge
   (`docker0`) occupies `172.17.0.0/16`. sbx's host-side network proxy tries to
   bind `172.17.0.0`, hits "endpoint is in invalid state", tears the sandbox
   network down, and creation fails with
   `failed to set up container networking: network <name> not found`. This
   blocks every sandbox (docker and non-docker templates alike).

   Fix options (host-level, require sudo + consent):

   - Relocate Docker's bridge subnet in `/etc/docker/daemon.json`, e.g.
     `{"bip": "192.168.100.1/24", "default-address-pools": [{"base": "192.168.101.0/24", "size": 24}]}`,
     then `sudo systemctl restart docker`. Frees `172.17.0.0/16` for sbx.
   - Or stop host Docker (`sudo systemctl stop docker docker.socket` and
     `sudo ip link delete docker0`). sbx does not need the host Docker daemon —
     it runs its own inside the microVM.

2. **`-docker` template "block" volume plugin missing.** The default `-docker`
   template variants (used so `npm run e2e:docker` can run inside the microVM)
   fail with `error looking up volume plugin block: plugin "block" not found`.
   This is upstream bug docker/sbx-releases#256 (open, labeled `os/linux`).
   Non-`-docker` templates are unaffected. Track upstream; a `sbx reset`
   resolved it for one reporter.

Minor: `net.ipv4.ping_group_range="1 0"` disables unprivileged ICMP (a warning
only). Enable with `sudo sysctl -w net.ipv4.ping_group_range="0 2147483647"`.

## Out of scope / possible follow-ups

- A prebuilt, pushed custom template image (pinned Python 3.12 / Node 24.10.0 /
  JDK 21) for faster cold starts.
- Multi-version `tox` interpreters (3.9–3.13).
- Clone mode and organization governance policies.
