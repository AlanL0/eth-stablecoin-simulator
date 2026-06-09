#!/usr/bin/env bash
# WP-1 acceptance gate — list expected tables/views/functions.
set -euo pipefail

URL="${1:-${DATABASE_URL:-}}"

if [[ -z "$URL" ]]; then
  echo "Usage: DATABASE_URL=... ./db/verify.sh"
  exit 1
fi

psql "$URL" -v ON_ERROR_STOP=1 <<'SQL'
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

SELECT table_name AS view_name
FROM information_schema.views
WHERE table_schema = 'public'
ORDER BY table_name;

SELECT proname AS function_name
FROM pg_proc
JOIN pg_namespace n ON n.oid = pg_proc.pronamespace
WHERE n.nspname = 'public'
  AND proname = 'resolve_chart_feedback';
SQL

expected_tables=(
  agent_runs
  chart_waitlist
  feedback_entries
  home_layouts
  product_events
  profiles
  protocol_presets
  simulations
  transfer_events
  wallet_snapshots
  yield_snapshots
)

missing=0
for t in "${expected_tables[@]}"; do
  if ! psql "$URL" -tAc "SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='$t'" | grep -q 1; then
    echo "MISSING table: $t"
    missing=$((missing + 1))
  fi
done

if psql "$URL" -tAc "SELECT 1 FROM information_schema.views WHERE table_schema='public' AND table_name='feedback_chart_backlog'" | grep -q 1; then
  echo "OK view: feedback_chart_backlog"
else
  echo "MISSING view: feedback_chart_backlog"
  missing=$((missing + 1))
fi

if [[ "$missing" -eq 0 ]]; then
  echo "WP-1 verify: PASS"
else
  echo "WP-1 verify: FAIL ($missing missing)"
  exit 1
fi