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
  echo "Refreshing OpenAPI snapshots from live Java service..."
  curl -fsSL "$JAVA_URL" -o "$JAVA_SNAPSHOT"
  # Agent contract is maintained as a committed snapshot until ETH-T22 publishes a dedicated group.
fi

if [[ ! -f "$JAVA_SNAPSHOT" || ! -f "$AGENT_SNAPSHOT" ]]; then
  echo "Missing OpenAPI snapshots in frontend/openapi/. Commit snapshots or set REFRESH_OPENAPI=1." >&2
  exit 1
fi

npx openapi-typescript "$JAVA_SNAPSHOT" -o "$OUT_JAVA"
npx openapi-typescript "$AGENT_SNAPSHOT" -o "$OUT_AGENT"
echo "Generated $OUT_JAVA and $OUT_AGENT"