#!/usr/bin/env bash
# Production/local smoke test for ETH Stablecoin Simulator MVP.
#
# Usage:
#   ./scripts/smoke-test.sh <JAVA_BASE> <FRONTEND_BASE>
#
# Example:
#   ./scripts/smoke-test.sh http://localhost:8080 http://localhost:3000

set -euo pipefail

JAVA_BASE="${1:?java base url required}"
FRONTEND_BASE="${2:?frontend base url required}"

strip_trailing_slash() {
  echo "${1%/}"
}

JAVA_BASE="$(strip_trailing_slash "$JAVA_BASE")"
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
echo "  Frontend: $FRONTEND_BASE"
echo

check "java health" "$JAVA_BASE/health"
check "java eth price" "$JAVA_BASE/api/price/eth"
if command -v jq >/dev/null 2>&1; then
  price_source="$(jq -r '.source // empty' /tmp/smoke-body.json 2>/dev/null)"
  price_degraded="$(jq -r '.degraded // empty' /tmp/smoke-body.json 2>/dev/null)"
  if [[ "$price_source" == "chainlink" || "$price_source" == "public_api" ]]; then
    echo "PASS  java eth price live source ($price_source)"
    pass=$((pass + 1))
  elif [[ "$price_source" == "static" && "$price_degraded" == "true" ]]; then
    echo "WARN  java eth price static/degraded (configure ETH_RPC_URL for live price)"
    pass=$((pass + 1))
  else
    echo "FAIL  java eth price source metadata (source=$price_source, degraded=$price_degraded)"
    fail=$((fail + 1))
  fi
fi
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
  if awk -v v="$liq_price" 'BEGIN { exit !(v > 3000 && v < 3300) }'; then
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

check "java wallet endpoint" "$JAVA_BASE/api/wallet/0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045/stablecoins"
check "java audit endpoint" "$JAVA_BASE/api/audit/0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"

REC_BODY='{"simulationResult":{"healthRatio":1.2,"riskTier":"HIGH","stablecoinDebtUsd":4222.22},"message":"How risky is this?","riskPreference":"conservative"}'
check_post "agent recommend-yield" "$JAVA_BASE/agent/recommend-yield" "$REC_BODY"

PARSE_BODY='{"message":"Show me bear and bull ETH scenarios as a chart","sessionId":"smoke-test"}'
check_post "agent parse-goal" "$JAVA_BASE/agent/parse-goal" "$PARSE_BODY"

check "frontend home" "$FRONTEND_BASE/"

echo
echo "Results: $pass passed, $fail failed"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
echo "smoke test OK"