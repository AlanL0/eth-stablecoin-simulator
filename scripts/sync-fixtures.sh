#!/usr/bin/env bash
# Copy chart fixtures from docs/ to service test directories.
# Status: planning artifact — run after WP-0 scaffold creates target dirs.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/docs/fixtures/charts"

if [[ ! -d "$SRC" ]]; then
  echo "Planning fixtures not found at $SRC (gitignored local docs/). Copy fixtures manually or keep docs/ locally."
  exit 1
fi

copy() {
  local dest="$1"
  mkdir -p "$dest"
  cp "$SRC"/*.json "$dest/"
  echo "synced → $dest"
}

copy "$ROOT/java-service/src/test/resources/fixtures/charts"
copy "$ROOT/python-agent/tests/fixtures/charts"
copy "$ROOT/frontend/test/fixtures/charts"

echo "done"