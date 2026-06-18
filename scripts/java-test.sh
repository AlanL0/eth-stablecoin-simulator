#!/usr/bin/env bash
# Run Java tests from any cwd. Use instead of bare 'mvn test' at repo root.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=scripts/check-java.sh
source "$ROOT/scripts/check-java.sh"
cd "$ROOT/java-service"
exec mvn test "$@"