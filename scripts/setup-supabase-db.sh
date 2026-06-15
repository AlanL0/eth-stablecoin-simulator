#!/usr/bin/env bash
# Apply ETH simulator schema + RLS + Supabase grants/trigger to remote Postgres.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export PATH="/opt/homebrew/opt/libpq/bin:${PATH:-}"

# shellcheck source=scripts/load-env.sh
source "$ROOT/scripts/load-env.sh"

PROJECT_REF="${SUPABASE_PROJECT_REF:-kxlbxvpuwvcdtgndphwd}"
DB_PASSWORD="${SUPABASE_DB_PASSWORD:-}"

resolve_database_url() {
  if [[ -n "${DATABASE_URL:-}" ]]; then
    echo "$DATABASE_URL"
    return
  fi
  if [[ -z "$DB_PASSWORD" ]]; then
    echo "setup-supabase-db: FAILED — set DATABASE_URL or SUPABASE_DB_PASSWORD in .env" >&2
    exit 1
  fi
  # Direct connection (migrations). Percent-encode special chars in password if needed.
  echo "postgresql://postgres:${DB_PASSWORD}@db.${PROJECT_REF}.supabase.co:5432/postgres"
}

URL="$(resolve_database_url)"
PSQL=(psql "$URL" -v ON_ERROR_STOP=1)

fail() {
  echo "setup-supabase-db: FAILED — $1" >&2
  exit 1
}

command -v psql >/dev/null 2>&1 || fail "psql not found — brew install libpq"

echo "setup-supabase-db: connecting..."
"${PSQL[@]}" -c "select 1" >/dev/null || fail "could not connect — check DATABASE_URL or SUPABASE_DB_PASSWORD"

echo "setup-supabase-db: applying WP-1 schema..."
"$ROOT/db/apply.sh" "$URL"

echo "setup-supabase-db: verifying tables..."
"$ROOT/db/verify.sh" "$URL"

echo "setup-supabase-db: applying RLS..."
"${PSQL[@]}" -f "$ROOT/db/rls.sql"

echo "setup-supabase-db: applying Data API grants..."
"${PSQL[@]}" -f "$ROOT/db/supabase_grants.sql"

echo "setup-supabase-db: enabling auth user trigger..."
"${PSQL[@]}" -f "$ROOT/db/supabase_auth_trigger.sql"

# Persist DATABASE_URL in .env when built from password (avoid duplicating if already set).
if [[ -z "${DATABASE_URL:-}" && -n "$DB_PASSWORD" ]]; then
  if grep -q '^DATABASE_URL=' "$ROOT/.env"; then
    # macOS sed
    sed -i '' "s|^DATABASE_URL=.*|DATABASE_URL=${URL}|" "$ROOT/.env"
  else
    echo "DATABASE_URL=${URL}" >>"$ROOT/.env"
  fi
  echo "setup-supabase-db: wrote DATABASE_URL to .env"
fi

echo "setup-supabase-db: OK"
echo "  Next: make test-supabase"
echo "  Auth redirect URLs (dashboard): http://localhost:3000/auth/callback , http://localhost:3000/profile"