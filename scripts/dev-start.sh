#!/usr/bin/env bash
# Start the full local simulator stack (Postgres + Java + agent + frontend).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

RUN_DIR="$ROOT/.run"
JAVA_PORT="${JAVA_PORT:-8080}"
AGENT_PORT="${AGENT_PORT:-8000}"
WEB_PORT="${WEB_PORT:-3000}"
JAVA_URL="http://localhost:${JAVA_PORT}"
AGENT_URL="http://localhost:${AGENT_PORT}"
WEB_URL="http://localhost:${WEB_PORT}"

mkdir -p "$RUN_DIR"

if [[ ! -f "$ROOT/.env" ]]; then
  cp "$ROOT/.env.example" "$ROOT/.env"
fi

# shellcheck source=scripts/load-env.sh
source "$ROOT/scripts/load-env.sh"

start_bg() {
  local name="$1"
  shift
  local log="$RUN_DIR/${name}.log"
  local pid_file="$RUN_DIR/${name}.pid"
  : >"$log"
  ( "$@" ) >>"$log" 2>&1 &
  echo $! >"$pid_file"
}

wait_http() {
  local name="$1"
  local url="$2"
  local attempts="${3:-60}"
  local i
  for ((i = 1; i <= attempts; i++)); do
    if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "${name} failed to become ready (${url})" >&2
  echo "See ${RUN_DIR}/${name}.log" >&2
  return 1
}

if docker compose version >/dev/null 2>&1; then
  docker compose up -d postgres >/dev/null 2>&1 || true
elif command -v docker-compose >/dev/null 2>&1; then
  docker-compose up -d postgres >/dev/null 2>&1 || true
fi

"$ROOT/scripts/sync-fixtures.sh" >/dev/null 2>&1 || true

start_bg java bash -lc "cd '$ROOT/java-service' && mvn -q compile spring-boot:run"
start_bg agent bash -lc "cd '$ROOT/python-agent' && pip install -q -e '.[dev]' && uvicorn app.main:app --host 0.0.0.0 --port ${AGENT_PORT}"
start_bg web bash -lc "cd '$ROOT/frontend' && if [[ ! -d node_modules ]]; then npm ci; fi && npm run dev -- --port ${WEB_PORT}"

wait_http java "${JAVA_URL}/health" 90
wait_http agent "${AGENT_URL}/health" 60
wait_http web "${WEB_URL}/" 90

echo "all: OK"
echo "  Java:     ${JAVA_URL}"
echo "  Agent:    ${AGENT_URL}"
echo "  Frontend: ${WEB_URL}"
echo "  Logs:     ${RUN_DIR}/"