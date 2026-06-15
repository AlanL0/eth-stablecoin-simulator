#!/usr/bin/env bash
# Verify Java serves public_api when Chainlink/RPC is disabled.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# shellcheck source=scripts/load-env.sh
source "$ROOT/scripts/load-env.sh"

if [[ -z "${PUBLIC_PRICE_API_URL:-}" ]]; then
  echo "test-price-fallback: FAILED — set PUBLIC_PRICE_API_URL in .env first" >&2
  exit 1
fi

"$ROOT/scripts/dev-stop.sh" >/dev/null 2>&1 || true

RUN_DIR="$ROOT/.run"
mkdir -p "$RUN_DIR"
: >"$RUN_DIR/fallback-test.log"

(
  export ETH_RPC_URL=
  cd "$ROOT/java-service"
  mvn -q compile spring-boot:run
) >>"$RUN_DIR/fallback-test.log" 2>&1 &
JAVA_PID=$!
echo "$JAVA_PID" >"$RUN_DIR/fallback-test.pid"

cleanup() {
  kill "$JAVA_PID" 2>/dev/null || true
  rm -f "$RUN_DIR/fallback-test.pid"
}
trap cleanup EXIT

for _ in $(seq 1 90); do
  if curl -fsS --max-time 2 "http://localhost:8080/health" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

RESP="$(curl -fsS --max-time 5 "http://localhost:8080/api/price/eth")"
SOURCE="$(python3 -c "import json,sys; print(json.load(sys.stdin)['source'])" <<<"$RESP")"
DEGRADED="$(python3 -c "import json,sys; print(str(json.load(sys.stdin)['degraded']).lower())" <<<"$RESP")"

if [[ "$SOURCE" == "public_api" && "$DEGRADED" == "false" ]]; then
  echo "test-price-fallback: OK (source=public_api)"
  exit 0
fi

if [[ "$SOURCE" == "static" ]]; then
  echo "test-price-fallback: FAILED — fell through to static (PUBLIC_PRICE_API_URL not reaching Java?)" >&2
  echo "  Check .env quoting: URLs with & must be wrapped in double quotes." >&2
else
  echo "test-price-fallback: FAILED — source=${SOURCE} degraded=${DEGRADED}" >&2
fi
echo "  See $RUN_DIR/fallback-test.log" >&2
exit 1