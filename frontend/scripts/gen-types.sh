#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_SNAPSHOT="$ROOT/openapi/java-api.json"
AGENT_SNAPSHOT="$ROOT/openapi/agent-api.json"
OUT_JAVA="$ROOT/lib/generated/java-api.d.ts"
OUT_AGENT="$ROOT/lib/generated/agent-api.d.ts"

mkdir -p "$ROOT/lib/generated"

if [[ "${REFRESH_OPENAPI:-}" == "1" ]]; then
  JAVA_URL="${JAVA_API_URL:-http://localhost:8080}/v3/api-docs"
  AGENT_URL="${AGENT_API_URL:-http://localhost:8000}/openapi.json"
  echo "Refreshing OpenAPI snapshots from live services..."
  curl -fsSL "$JAVA_URL" -o "$JAVA_SNAPSHOT"
  curl -fsSL "$AGENT_URL" -o "$AGENT_SNAPSHOT"
fi

if [[ ! -f "$JAVA_SNAPSHOT" || ! -f "$AGENT_SNAPSHOT" ]]; then
  echo "Missing OpenAPI snapshots in frontend/openapi/. Commit snapshots or set REFRESH_OPENAPI=1." >&2
  exit 1
fi

npx openapi-typescript "$JAVA_SNAPSHOT" -o "$OUT_JAVA"
npx openapi-typescript "$AGENT_SNAPSHOT" -o "$OUT_AGENT"
echo "Generated $OUT_JAVA and $OUT_AGENT"