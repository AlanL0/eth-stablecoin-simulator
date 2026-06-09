.PHONY: help dev dev-build dev-logs down stop stop-host-ports reset-db db-apply db-verify java-build java-test java-run test sync-fixtures smoke curl-sim curl-price curl-chart

COMPOSE := docker compose
export DATABASE_URL ?= postgresql://postgres:postgres@localhost:54329/ethsim
JAVA_URL ?= http://localhost:8080

help:
	@echo "ETH Stablecoin Simulator — local dev"
	@echo ""
	@echo "  make dev-build    Build Java image and start postgres + java-service"
	@echo "  make dev          Start stack (no rebuild)"
	@echo "  make dev-logs     Tail compose logs"
	@echo "  make down         Stop Docker stack only"
	@echo "  make stop         Stop Docker stack + host dev processes (8080/8000/3000)"
	@echo "  make reset-db     Recreate postgres volume and re-apply schema"
	@echo "  make db-apply     Apply WP-1 SQL to DATABASE_URL"
	@echo "  make db-verify    Run WP-1 verification"
	@echo "  make java-build   mvn package (skip tests)"
	@echo "  make java-test    mvn test"
	@echo "  make java-run     Run Spring Boot on host (port 8080)"
	@echo "  make test         Alias for java-test"
	@echo "  make sync-fixtures  Copy chart fixtures to java-service tests"
	@echo "  make curl-sim     POST sample simulation to running java-service"
	@echo "  make curl-price   GET /api/price/eth"
	@echo "  make curl-chart   GET liquidation-band chart"
	@echo "  make smoke        scripts/smoke-test.sh (needs agent+frontend later)"

dev-build: java-build sync-fixtures
	$(COMPOSE) up --build -d
	@echo "Java API: $(JAVA_URL)/health"
	@echo "Postgres: $(DATABASE_URL)"

dev:
	$(COMPOSE) up -d

dev-logs:
	$(COMPOSE) logs -f

down:
	$(COMPOSE) down

stop:
	-$(COMPOSE) down
	@$(MAKE) stop-host-ports
	@echo "All local dev sessions stopped."

# Kill host-run services from prior make java-run / agent / frontend sessions.
stop-host-ports:
	@for port in 8080 8000 3000; do \
		pids=$$(lsof -ti:$$port 2>/dev/null || true); \
		if [ -n "$$pids" ]; then \
			echo "Stopping process(es) on port $$port: $$pids"; \
			kill $$pids 2>/dev/null || true; \
		fi; \
	done

reset-db: down
	$(COMPOSE) down -v
	$(MAKE) dev
	@sleep 3
	$(MAKE) db-apply
	$(MAKE) db-verify

db-apply:
	./db/apply.sh "$(DATABASE_URL)"

db-verify:
	./db/verify.sh "$(DATABASE_URL)"

java-build:
	cd java-service && mvn -q package -DskipTests

java-test: sync-fixtures
	cd java-service && mvn test

java-run: sync-fixtures
	cd java-service && mvn spring-boot:run

test: java-test

sync-fixtures:
	@mkdir -p java-service/src/test/resources/fixtures/charts
	@if [ -d docs/fixtures/charts ]; then \
		cp docs/fixtures/charts/*.json java-service/src/test/resources/fixtures/charts/; \
		cp docs/fixtures/treasury-context-response.json java-service/src/test/resources/fixtures/; \
		echo "fixtures synced"; \
	else \
		echo "docs/fixtures not found (gitignored planning) — using committed test fixtures"; \
	fi

curl-sim:
	curl -sS -X POST "$(JAVA_URL)/api/simulations" \
		-H "Content-Type: application/json" \
		-d '{"collateralUsd":7600,"protocol":"maker_sky","deployYieldPct":5,"years":1,"compoundsPerYear":12}' | python3 -m json.tool

curl-price:
	curl -sS "$(JAVA_URL)/api/price/eth" | python3 -m json.tool

curl-chart:
	curl -sS "$(JAVA_URL)/api/charts/liquidation-band?ethAmount=2&protocol=maker_sky" | python3 -m json.tool

smoke:
	./scripts/smoke-test.sh "$(JAVA_URL)" "http://localhost:8000" "http://localhost:3000"