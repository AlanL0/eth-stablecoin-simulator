#!/usr/bin/env bash
# Production/local smoke test for ETH Stablecoin Simulator MVP.
# Status: planning artifact — requires WP-10 services running.
#
# Usage:
#   ./scripts/smoke-test.sh <JAVA_BASE> <AGENT_BASE> <FRONTEND_BASE>
#
# Example:
#   ./scripts/smoke-test.sh http://localhost:8080 http://localhost:8000 http://localhost:3000

set -euo pipefail

JAVA_BASE="${1:?java base url required}"
AGENT_BASE="${2:?agent base url required}"
FRONTEND_BASE="${3:?frontend base url required}"

strip_trailing_slash() {
  echo "${1%/}"
}

JAVA_BASE="$(strip_trailing_slash "$JAVA_BASE")"
AGENT_BASE="$(strip_trailing_slash "$AGENT_BASE")"
FRONTEND_BASE="$(strip_trailing_slash "$FRONTEND_BASE")"

pass=0
fail=0

check() {
  local name="$1"
  local url="$2"
  local expected="${3:-200}"
  local code
  code="$(curl -s -o /tmp/smoke-body.json -w "%{http_code}" "$url")"
  if [[ "$code" == "$expected" ]]; then
    echo "PASS  $name ($code)"
    pass=$((pass + 1))
  else
    echo "FAIL  $name (expected $expected, got $code)"
    cat /tmp/smoke-body.json 2>/dev/null || true
    fail=$((fail + 1))
  fi
}

check_post() {
  local name="$1"
  local url="$2"
  local body="$3"
  local expected="${4:-200}"
  local code
  code="$(curl -s -o /tmp/smoke-body.json -w "%{http_code}" \
    -X POST -H "Content-Type: application/json" -d "$body" "$url")"
  if [[ "$code" == "$expected" ]]; then
    echo "PASS  $name ($code)"
    pass=$((pass + 1))
  else
    echo "FAIL  $name (expected $expected, got $code)"
    cat /tmp/smoke-body.json 2>/dev/null || true
    fail=$((fail + 1))
  fi
}

echo "Smoke test"
echo "  Java:     $JAVA_BASE"
echo "  Agent:    $AGENT_BASE"
echo "  Frontend: $FRONTEND_BASE"
echo

check "java health" "$JAVA_BASE/health"
check "java eth price" "$JAVA_BASE/api/price/eth"
check "java yields" "$JAVA_BASE/api/yields?asset=USDC"

SIM_BODY='{"ethAmount":2,"protocol":"maker_sky","deployYieldPct":5,"years":1,"compoundsPerYear":12}'
check_post "java simulation" "$JAVA_BASE/api/simulations" "$SIM_BODY"

if command -v jq >/dev/null 2>&1; then
  charts_len="$(jq -r '.charts | length' /tmp/smoke-body.json 2>/dev/null || echo 0)"
  liq_price="$(jq -r '.liquidationPriceUsd' /tmp/smoke-body.json 2>/dev/null || echo 0)"
  schema_ver="$(jq -r '.charts[0].schemaVersion // empty' /tmp/smoke-body.json 2>/dev/null)"
  if [[ "$charts_len" -ge 3 && "$schema_ver" == "1.0" ]]; then
    echo "PASS  java simulation charts (count=$charts_len, schemaVersion=$schema_ver)"
    pass=$((pass + 1))
  else
    echo "FAIL  java simulation charts (count=$charts_len, schemaVersion=$schema_ver)"
    fail=$((fail + 1))
  fi
  if python3 -c "import sys; v=float(sys.argv[1]); sys.exit(0 if 3000 < v < 3300 else 1)" "$liq_price" 2>/dev/null; then
    echo "PASS  java liquidation price sane ($liq_price)"
    pass=$((pass + 1))
  else
    echo "FAIL  java liquidation price unexpected ($liq_price, expected ~3166.67)"
    fail=$((fail + 1))
  fi
else
  echo "SKIP  chart assertions (jq not installed)"
fi

check "java health chart" \
  "$JAVA_BASE/api/charts/health-ratio?ethAmount=2&protocol=maker_sky&deployYieldPct=5&years=1"

check "agent health" "$AGENT_BASE/health"

REC_BODY='{"simulationResult":{"healthRatio":1.2,"riskTier":"MEDIUM","stablecoinDebtUsd":4222.22},"message":"How risky is this?","riskPreference":"conservative"}'
check_post "agent recommend-yield" "$AGENT_BASE/agent/recommend-yield" "$REC_BODY"

PARSE_BODY='{"message":"Show me bear and bull ETH scenarios as a chart","sessionId":"smoke-test"}'
check_post "agent parse-goal" "$AGENT_BASE/agent/parse-goal" "$PARSE_BODY"

check "frontend home" "$FRONTEND_BASE/"

echo
echo "Results: $pass passed, $fail failed"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
echo "smoke test OK"