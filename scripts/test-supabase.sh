#!/usr/bin/env bash
# Verify Supabase env vars and API reachability (ETH-T10 Step 5).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# shellcheck source=scripts/load-env.sh
source "$ROOT/scripts/load-env.sh"

fail() {
  echo "test-supabase: FAILED — $1" >&2
  exit 1
}

SUPABASE_URL="${NEXT_PUBLIC_SUPABASE_URL:-}"
SUPABASE_KEY="${NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY:-${NEXT_PUBLIC_SUPABASE_ANON_KEY:-}}"

if [[ -z "$SUPABASE_URL" || -z "$SUPABASE_KEY" ]]; then
  fail "set NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY (or legacy NEXT_PUBLIC_SUPABASE_ANON_KEY) in .env"
fi

if [[ ! "$SUPABASE_URL" =~ ^https://[a-z0-9-]+\.supabase\.co$ ]]; then
  fail "NEXT_PUBLIC_SUPABASE_URL should look like https://<ref>.supabase.co"
fi

HTTP_CODE="$(curl -sS -o /dev/null -w "%{http_code}" --max-time 15 \
  -H "apikey: ${SUPABASE_KEY}" \
  -H "Authorization: Bearer ${SUPABASE_KEY}" \
  "${SUPABASE_URL}/auth/v1/health")"

[[ "$HTTP_CODE" == "200" ]] || fail "auth health returned HTTP ${HTTP_CODE} — check URL and key"

KEY_KIND="legacy anon"
if [[ "$SUPABASE_KEY" == sb_publishable_* ]]; then
  KEY_KIND="publishable"
fi

echo "test-supabase: OK (auth health, ${KEY_KIND} key)"

if [[ -n "${DATABASE_URL:-}" ]]; then
  if command -v psql >/dev/null 2>&1; then
    if psql "$DATABASE_URL" -c "select 1" >/dev/null 2>&1; then
      echo "  DATABASE_URL: connected"
    else
      echo "  DATABASE_URL: set but psql connection failed — run: make db-verify"
    fi
  else
    echo "  DATABASE_URL: set (install psql to verify migrations)"
  fi
else
  echo "  DATABASE_URL: not set — optional for frontend auth; required for make db-apply"
fi

echo "  Next: make db-apply && psql \"\$DATABASE_URL\" -f db/rls.sql"
echo "  Auth: open http://localhost:3000/login and complete magic link"