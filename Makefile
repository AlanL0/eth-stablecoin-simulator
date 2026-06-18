# ETH Stablecoin Simulator — local development
.DEFAULT_GOAL := help

SHELL := /bin/bash
.SHELLFLAGS := -eu -o pipefail -c

ROOT        := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
COMPOSE     := docker compose
ENV_FILE    := .env
RUN_DIR     := .run

JAVA_DIR    := java-service
WEB_DIR     := frontend

JAVA_PORT   := 8080
WEB_PORT    := 3000
PG_PORT     := 54329

JAVA_URL    ?= http://localhost:$(JAVA_PORT)
WEB_URL     ?= http://localhost:$(WEB_PORT)
export DATABASE_URL ?= postgresql://postgres:postgres@localhost:$(PG_PORT)/ethsim

# Load .env safely (handles & and ? in URLs — quote values in .env when unsure).
WITH_ENV = source ./scripts/load-env.sh &&

.PHONY: help all stop test status env deps sync-fixtures \
        java-build java-test java-run \
        web-test web-run web-build frontend-test frontend-run \
        db-up db-down db-apply db-verify db-reset \
        smoke ci-smoke curl-price curl-sim curl-chart test-public-price test-price-fallback test-supabase setup-supabase-db \
        dev dev-build dev-logs down

help: ## Show available targets
	@echo "ETH Stablecoin Simulator"
	@echo ""
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z0-9_-]+:.*?## / {printf "  %-14s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

env: ## Create .env from .env.example when missing
	@test -f $(ENV_FILE) || cp .env.example $(ENV_FILE)

deps: env ## Install/build service dependencies (no tests)
	@cd $(JAVA_DIR) && mvn -q package -DskipTests
	@if [ ! -d $(WEB_DIR)/node_modules ]; then cd $(WEB_DIR) && npm ci; fi

all: env ## Start Postgres + Java + frontend (background)
	@./scripts/dev-start.sh

stop: ## Stop Docker stack and all dev servers on 8080/3000
	@if ./scripts/dev-stop.sh; then echo "stop: OK"; else echo "stop: FAILED" >&2; exit 1; fi

status: ## Show which dev ports are in use
	@for port in $(JAVA_PORT) $(WEB_PORT) $(PG_PORT); do \
		if lsof -nP -iTCP:$$port -sTCP:LISTEN >/dev/null 2>&1; then \
			echo "  $$port: listening"; \
		else \
			echo "  $$port: free"; \
		fi; \
	done

sync-fixtures: ## Copy chart fixtures into test directories
	@./scripts/sync-fixtures.sh

test: sync-fixtures ## Run Java and frontend test suites
	@$(MAKE) java-test
	@$(MAKE) web-test
	@echo "test: OK"

java-build: ## Build Java service (skip tests)
	@cd $(JAVA_DIR) && mvn -q package -DskipTests

java-test: sync-fixtures ## Run Java unit/integration tests (Java 25; not bare mvn at repo root)
	@./scripts/java-test.sh -q

java-run: env sync-fixtures ## Run Java API on :$(JAVA_PORT) (foreground)
	@$(WITH_ENV) cd $(JAVA_DIR) && mvn -q spring-boot:run

web-test: ## Run frontend typecheck, vitest, and build
	@cd $(WEB_DIR) && npm run typecheck && npm test && npm run build

web-run: env ## Run Next.js dev server on :$(WEB_PORT) (foreground)
	@$(WITH_ENV) cd $(WEB_DIR) && npm run dev -- --port $(WEB_PORT)

web-build: ## Production frontend build only
	@cd $(WEB_DIR) && npm run build

frontend-test: web-test ## Alias for frontend-test
frontend-run: web-run ## Alias for frontend-run

db-up: ## Start Postgres container only
	@$(COMPOSE) up -d postgres

db-down: ## Stop Postgres container
	@$(COMPOSE) stop postgres

db-apply: ## Apply database schema
	@./db/apply.sh "$(DATABASE_URL)"

db-verify: ## Verify database schema
	@./db/verify.sh "$(DATABASE_URL)"

db-reset: stop db-up ## Recreate Postgres volume and re-apply schema
	@$(COMPOSE) down -v
	@$(COMPOSE) up -d postgres
	@sleep 3
	@$(MAKE) db-apply db-verify

dev-build: java-build sync-fixtures ## Docker: build Java image + start Postgres and Java
	@$(COMPOSE) up --build -d

dev: ## Docker: start Postgres + Java (no rebuild)
	@$(COMPOSE) up -d

dev-logs: ## Tail Docker compose logs
	@$(COMPOSE) logs -f

down: ## Stop Docker compose stack only
	@$(COMPOSE) down

smoke: ## End-to-end smoke test (services must be running)
	@./scripts/smoke-test.sh "$(JAVA_URL)" "$(WEB_URL)"

ci-smoke: ## Full CI gate: tests + optional smoke when services are up
	@./scripts/ci-smoke.sh

curl-price: ## GET /api/price/eth
	@curl -sS "$(JAVA_URL)/api/price/eth" | jq .

curl-sim: ## POST sample simulation
	@curl -sS -X POST "$(JAVA_URL)/api/simulations" \
		-H "Content-Type: application/json" \
		-d '{"collateralUsd":7600,"protocol":"maker_sky","deployYieldPct":5,"years":1,"compoundsPerYear":12}' \
		| jq .

curl-chart: ## GET liquidation-band chart
	@curl -sS "$(JAVA_URL)/api/charts/liquidation-band?ethAmount=2&protocol=maker_sky" | jq .

test-public-price: env ## Verify PUBLIC_PRICE_API_URL responds (Step 2 direct check)
	@./scripts/test-public-price.sh

test-price-fallback: env ## Verify Java uses public_api when ETH_RPC_URL is disabled
	@./scripts/test-price-fallback.sh

test-supabase: env ## Verify Supabase URL/key and auth health (Step 5)
	@chmod +x ./scripts/test-supabase.sh
	@./scripts/test-supabase.sh

setup-supabase-db: env ## Apply schema + RLS + grants to remote Supabase (needs DATABASE_URL or SUPABASE_DB_PASSWORD)
	@chmod +x ./scripts/setup-supabase-db.sh
	@./scripts/setup-supabase-db.sh