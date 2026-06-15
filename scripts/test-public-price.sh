#!/usr/bin/env bash
# Step 2 check: PUBLIC_PRICE_API_URL responds with an ETH/USD price.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# shellcheck source=scripts/load-env.sh
source "$ROOT/scripts/load-env.sh"

URL="${PUBLIC_PRICE_API_URL:-}"
if [[ -z "$URL" ]]; then
  echo "test-public-price: FAILED — PUBLIC_PRICE_API_URL is not set in .env" >&2
  exit 1
fi

if [[ "$URL" != *"ids=ethereum"* && "$URL" != *"ids%3Dethereum"* ]]; then
  echo "test-public-price: FAILED — URL must request ids=ethereum (ETH/USD), not another asset" >&2
  echo "  Use: https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd" >&2
  echo "  Wrap the URL in double quotes in .env (the & character breaks plain source .env)." >&2
  exit 1
fi

BODY="$(curl -fsS --max-time 15 -A "ethStableCoin-simulator/1.0" "$URL")"
PRICE="$(python3 -c "
import json, sys
d = json.load(sys.stdin)
if 'ethereum' in d and 'usd' in d['ethereum']:
    print(d['ethereum']['usd'])
    raise SystemExit
for v in d.values():
    if isinstance(v, dict) and 'usd' in v:
        print(v['usd'])
        raise SystemExit
if 'priceUsd' in d:
    print(d['priceUsd'])
" <<<"$BODY")"

if [[ -z "$PRICE" ]]; then
  echo "test-public-price: FAILED — could not parse price from response" >&2
  exit 1
fi

echo "test-public-price: OK (eth/usd=${PRICE})"