#!/usr/bin/env bash
# Verify python-agent is running and responding (Step 3 / agent-run check).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# shellcheck source=scripts/load-env.sh
source "$ROOT/scripts/load-env.sh"

AGENT_URL="${AGENT_URL:-http://localhost:8000}"
JAVA_URL="${JAVA_API_URL:-http://localhost:8080}"

fail() {
  echo "test-agent: FAILED — $1" >&2
  exit 1
}

curl -fsS --max-time 5 "${AGENT_URL}/health" >/dev/null 2>&1 \
  || fail "agent not reachable at ${AGENT_URL} — run: make agent-run"

curl -fsS --max-time 5 "${JAVA_URL}/health" >/dev/null 2>&1 \
  || fail "Java API not reachable at ${JAVA_URL} — agent tools need Java (make java-run or make all)"

HEALTH="$(curl -fsS --max-time 5 "${AGENT_URL}/health")"
SERVICE="$(python3 -c "import json,sys; print(json.load(sys.stdin)['service'])" <<<"$HEALTH")"
[[ "$SERVICE" == "python-agent" ]] || fail "unexpected health payload"

REC_BODY='{"simulationResult":{"healthRatio":1.2,"riskTier":"HIGH","stablecoinDebtUsd":4222.22,"liquidationPriceUsd":3166.67,"collateralValueUsd":7600},"message":"How risky is this?","riskPreference":"conservative"}'
REC="$(curl -fsS --max-time 60 -X POST "${AGENT_URL}/agent/recommend-yield" \
  -H "Content-Type: application/json" -d "$REC_BODY")"
python3 -c "import json,sys; d=json.load(sys.stdin); assert d.get('summary')" <<<"$REC" \
  || fail "recommend-yield returned invalid JSON"

PARSE_BODY='{"message":"Show me bear and bull ETH scenarios as a chart","sessionId":"test-agent"}'
PARSE="$(curl -fsS --max-time 60 -X POST "${AGENT_URL}/agent/parse-goal" \
  -H "Content-Type: application/json" -d "$PARSE_BODY")"
python3 -c "import json,sys; d=json.load(sys.stdin); assert d.get('intent')=='request_chart'" <<<"$PARSE" \
  || fail "parse-goal returned unexpected intent"

FALLBACK="$(python3 -c "import json,sys; print(str(json.load(sys.stdin).get('fallbackUsed', True)).lower())" <<<"$REC")"
MODEL="$(python3 -c "import json,sys; print(json.load(sys.stdin).get('model',''))" <<<"$REC")"

echo "test-agent: OK (health, recommend-yield, parse-goal)"

if [[ "$FALLBACK" == "true" ]]; then
  echo "  LLM: fallback mode (${MODEL})"
  if [[ -n "${LLM_API_KEY:-}" ]]; then
    python3 - <<'PY' 2>/dev/null || true
import os, json, urllib.request, urllib.error
url = os.environ.get("LLM_BASE_URL", "").rstrip("/") + "/chat/completions"
key = os.environ.get("LLM_API_KEY", "")
if not url or not key:
    raise SystemExit
model = os.environ.get("MODEL_PRIMARY") or "deepseek-chat"
body = json.dumps({"model": model, "messages": [{"role": "user", "content": "hi"}], "max_tokens": 5}).encode()
req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json", "Authorization": f"Bearer {key}"})
try:
    urllib.request.urlopen(req, timeout=15)
except urllib.error.HTTPError as e:
    err = json.loads(e.read().decode())
    msg = err.get("error", {}).get("message", e.reason)
    print(f"  Hint: LLM provider returned HTTP {e.code} — {msg}")
PY
  else
    echo "  Hint: set LLM_API_KEY in .env for live AI responses"
  fi
else
  echo "  LLM: live (${MODEL})"
fi