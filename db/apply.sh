#!/usr/bin/env bash
# Apply all WP-1 SQL in order. Requires DATABASE_URL or first argument.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
URL="${1:-${DATABASE_URL:-}}"

if [[ -z "$URL" ]]; then
  echo "Usage: DATABASE_URL=... ./db/apply.sh"
  echo "   or: ./db/apply.sh 'postgresql://user:pass@host:5432/dbname'"
  exit 1
fi

for f in schema.sql product_events.sql feedback_backlog.sql feedback_resolve.sql; do
  echo "Applying db/$f ..."
  psql "$URL" -v ON_ERROR_STOP=1 -f "$ROOT/db/$f"
done

echo "Done. Run ./db/verify.sh to check tables and views."