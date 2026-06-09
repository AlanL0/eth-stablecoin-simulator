# ETH Stablecoin Simulator

Hobby-first web app: model ETH collateral → stablecoin mint/borrow → yield, risk, and Treasury education context.

## Monorepo layout

```text
frontend/       Next.js (App Router, TypeScript, Recharts)
java-service/   Spring Boot 3 + web3j — all finance, charts, chain reads
python-agent/   FastAPI — AI explain + Java tool calls only
db/             Postgres schema (WP-1+)
scripts/        Dev utilities (fixtures sync, smoke test)
```

## Planning docs (local only)

Design specs, fixtures, draft SQL, and prompts are **gitignored** and stay on your machine:

- `ETH_Stablecoin_Simulator_Complete_Plan.md`
- `docs/` (architecture, API contracts, chart specs, etc.)
- `db/*.sql` (drafts — implementation SQL added per work package)
- `python-agent/prompts/` (drafts — tracked prompts added in WP-5)

Do not commit secrets. Use `.env.example` as a template.

## Prerequisites

- Node.js 20+
- Java 21 + Maven 3.9+
- Python 3.12
- Docker (optional)

## Quick start (after WP-1+)

```bash
# Database
psql "$DATABASE_URL" -f db/schema.sql   # when WP-1 ships tracked SQL

# Java
cd java-service && mvn spring-boot:run

# Python
cd python-agent && pip install -e . && uvicorn app.main:app --reload

# Frontend
cd frontend && npm install && npm run dev
```

## Work packages

Implementation order: WP-0 scaffold → WP-1 DB → WP-2 Java sim → …

See local `docs/work-packages.md` (gitignored) for full prompts.

## Security

- Never commit `.env`, API keys, RPC URLs with keys, or service role keys.
- Frontend uses Supabase **anon** key only.
- Java/Python use service `DATABASE_URL` server-side only.