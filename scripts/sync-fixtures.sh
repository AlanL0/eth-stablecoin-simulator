#!/usr/bin/env bash
# Sync committed chart fixtures into service test directories.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

copy_tree() {
  local src="$1"
  local dest="$2"
  [[ "$(cd "$src" && pwd)" == "$(cd "$dest" && pwd 2>/dev/null || echo "$dest")" ]] && return 0
  mkdir -p "$dest"
  cp -f "$src"/*.json "$dest/"
}

SRC="$ROOT/frontend/test/fixtures/charts"
if [[ ! -d "$SRC" ]]; then
  SRC="$ROOT/java-service/src/test/resources/fixtures/charts"
fi

if [[ ! -d "$SRC" ]] || ! compgen -G "$SRC/*.json" >/dev/null; then
  exit 0
fi

copy_tree "$SRC" "$ROOT/java-service/src/test/resources/fixtures/charts"
copy_tree "$SRC" "$ROOT/frontend/test/fixtures/charts"