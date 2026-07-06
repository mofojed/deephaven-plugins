#!/usr/bin/env bash
#
# Provision a development environment for the deephaven-plugins repo.
#
# This script is idempotent and safe to re-run. It is invoked automatically as
# the install command of the Docker Sandbox kit (see sbx/deephaven-plugins/spec.yaml),
# but it can also be run standalone on any machine that has Python 3 and Node.js
# available:
#
#   bash tools/sbx_setup.sh
#
# It creates an in-repo virtual environment at .venv, installs the Python and
# JavaScript dependencies, and builds + installs all plugins into the venv so
# the Deephaven server can serve them.
set -euo pipefail

# Resolve the repo root as the parent of this script's directory so the script
# works regardless of the current working directory.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

echo "==> deephaven-plugins sandbox setup (repo: ${REPO_ROOT})"

# --- Node: match the version pinned in .nvmrc when nvm is available ----------
# The base sandbox image ships a recent Node, but if nvm is present we pin to
# the repo's .nvmrc so local builds line up with CI. Best effort only.
if [ -f .nvmrc ] && [ -s "${NVM_DIR:-$HOME/.nvm}/nvm.sh" ]; then
  echo "==> Selecting Node from .nvmrc via nvm"
  # shellcheck disable=SC1091
  . "${NVM_DIR:-$HOME/.nvm}/nvm.sh"
  nvm install
  nvm use
fi

echo "==> Node: $(node --version 2>/dev/null || echo 'not found')"
echo "==> Python: $(python3 --version 2>/dev/null || echo 'not found')"

# --- Python virtual environment ---------------------------------------------
if [ ! -d .venv ]; then
  echo "==> Creating virtual environment at .venv"
  python3 -m venv .venv
fi

# shellcheck disable=SC1091
. .venv/bin/activate

echo "==> Upgrading pip and installing Python requirements"
python -m pip install --upgrade pip
python -m pip install --upgrade -r requirements.txt

# Optional: documentation build dependencies. Non-fatal if this fails so the
# core dev loop still works without the docs toolchain.
if [ -f sphinx_ext/sphinx-requirements.txt ]; then
  echo "==> Installing docs requirements (optional)"
  python -m pip install -r sphinx_ext/sphinx-requirements.txt || \
    echo "!! Skipping docs requirements (install failed)"
fi

# Pre-commit hooks (formatting, linting, type checking). Non-fatal outside a
# git checkout.
if [ -d .git ]; then
  echo "==> Installing pre-commit hooks"
  pre-commit install || echo "!! Skipping pre-commit install"
fi

# --- JavaScript dependencies -------------------------------------------------
echo "==> Installing JavaScript dependencies (npm ci)"
npm ci

# --- Build + install all plugins into the venv -------------------------------
echo "==> Building JS and installing plugins into the venv"
python tools/plugin_builder.py --js --install

echo "==> Setup complete."
echo "    Activate the environment with: source .venv/bin/activate"
echo "    Start the server with:         python tools/plugin_builder.py -r -s -sa --host -sa 0.0.0.0"
