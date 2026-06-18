#!/usr/bin/env bash
# Stop all local ethStableCoin processes (Docker + host dev servers).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

STOP_PORTS=(8080 3000)
FAILED=0

stop_pid_file() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  local pid
  pid="$(cat "$file" 2>/dev/null || true)"
  rm -f "$file"
  [[ -n "$pid" ]] || return 0
  kill "$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null || FAILED=1
}

if docker compose version >/dev/null 2>&1; then
  docker compose down --remove-orphans >/dev/null 2>&1 || true
elif command -v docker-compose >/dev/null 2>&1; then
  docker-compose down --remove-orphans >/dev/null 2>&1 || true
fi

if [[ -d "$ROOT/.run" ]]; then
  for pid_file in "$ROOT"/.run/*.pid; do
    [[ -e "$pid_file" ]] || continue
    stop_pid_file "$pid_file"
  done
fi

for port in "${STOP_PORTS[@]}"; do
  pids="$(lsof -ti:"$port" 2>/dev/null || true)"
  [[ -z "$pids" ]] && continue
  while read -r pid; do
    [[ -n "$pid" ]] || continue
    kill "$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null || FAILED=1
  done <<< "$pids"
done

for port in "${STOP_PORTS[@]}"; do
  if lsof -ti:"$port" >/dev/null 2>&1; then
    FAILED=1
  fi
done

exit "$FAILED"