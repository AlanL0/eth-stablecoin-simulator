#!/usr/bin/env bash
# Regenerate python-agent/requirements.lock (third-party prod deps only).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
AGENT="$ROOT/python-agent"
TMP="$(mktemp -d)"

python3 -m venv "$TMP/venv"
# shellcheck disable=SC1091
source "$TMP/venv/bin/activate"
pip install -q --upgrade pip
cd "$AGENT"
pip install -q -r <(python3 - <<'PY'
import pathlib
import tomllib

data = tomllib.loads(pathlib.Path("pyproject.toml").read_text())
for dep in data["project"]["dependencies"]:
    print(dep)
PY
)

pip freeze | grep -Ev '^(eth-stablecoin-python-agent|pip|setuptools|wheel)==?' \
  >"$AGENT/requirements.lock"

deactivate
rm -rf "$TMP"
echo "Wrote $AGENT/requirements.lock"