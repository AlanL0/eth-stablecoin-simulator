#!/usr/bin/env bash
# Local CI helper: start services and run smoke test.
# Usage: ./scripts/ci-smoke.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

JAVA_URL="${JAVA_URL:-http://localhost:8080}"
AGENT_URL="${AGENT_URL:-http://localhost:8000}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:3000}"

echo "==> Java tests"
make java-test

echo "==> Python tests"
cd python-agent && python -m pytest -q && cd "$ROOT"

echo "==> Frontend gates"
cd frontend && npm run typecheck && npm test && npm run build && cd "$ROOT"

echo "==> Contract typegen drift check"
cd frontend && npm run gen:types
git diff --exit-code lib/generated/ || {
  echo "Generated types drifted. Commit regenerated files." >&2
  exit 1
}
cd "$ROOT"

if curl -fsS "$JAVA_URL/health" >/dev/null 2>&1 \
  && curl -fsS "$AGENT_URL/health" >/dev/null 2>&1 \
  && curl -fsS "$FRONTEND_URL/" >/dev/null 2>&1; then
  echo "==> Smoke test (services detected)"
  ./scripts/smoke-test.sh "$JAVA_URL" "$AGENT_URL" "$FRONTEND_URL"
else
  echo "==> Smoke test skipped (start java, agent, frontend to run end-to-end)"
fi

echo "ci-smoke OK"