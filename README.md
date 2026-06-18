# ETH Stablecoin Simulator

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A **hobby-first, open-source web app** for modeling ETH collateral → stablecoin mint/borrow scenarios. Explore liquidation risk, projected yield, health ratios, and educational Treasury context **before** committing real collateral — with every number computed deterministically in Java, not by an LLM.

**Live repo:** https://github.com/AlanL0/eth-stablecoin-simulator

---

## Goal

Help ETH holders, DeFi learners, and small treasury volunteers answer questions like:

- How much stablecoin can I mint or borrow against my ETH?
- At what ETH price am I at risk of liquidation?
- What do stability fees and deploy yield look like over time?
- How does **my** DeFi yield compare to illustrative **issuer** reserve economics (educational only)?

The simulator is a **sandbox with labelled assumptions**. It is not a compliance tool, trading bot, or live protocol integration.

---

## What you get (MVP direction)

| Feature | Description |
|--------|-------------|
| **Simulation engine** | Protocol presets (Maker/Sky, Liquity, Aave/GHO, Custom), debt, liquidation price, fees, net yield |
| **Risk & charts** | Health ratio, risk tier, ChartSpecV1 JSON (yield projection, liquidation band, health sweep) |
| **Treasury panel** | Free educational context: T-bill backing proxy, issuer reserve yield vs your DeFi yield |
| **Market data** | ETH price chain: cache → Chainlink (web3j) → public API → static fallback — always source-labelled |
| **AI layer** *(planned)* | Spring AI in Java explains results and calls tools; never owns the math |
| **Wallet & audit** *(planned)* | Allowlisted stablecoin balances and lite CSV/JSON export |

**Free forever:** liquidation band, health sweep, basic yield projection, Treasury education panel, and full AI (infra rate limits only). Advanced multi-scenario charts may be gated later; safety primitives stay free.

---

## Architecture

```text
frontend/       Next.js — renders ChartSpecV1; no finance math
java-service/   Spring Boot 4 + web3j — simulation, charts, chain reads, Spring AI (single source of truth)
db/             Postgres schema (profiles, simulations, feedback backlog, …)
```

**Golden rules**

1. Java owns every number and every chart `series[]` point.
2. LLMs explain and classify — they do not calculate.
3. AI failure must not block simulation or charts.
4. Assumptions and data sources are visible in API responses.

---

## Example (canonical fixture)

**Inputs:** 2 ETH @ $3,800, Maker/Sky preset, 5% stability fee, 5% deploy yield, 1 year.

| Output | Value |
|--------|-------|
| Collateral | $7,600 |
| Stablecoin debt | $4,222.22 |
| Liquidation price | $3,166.67 |
| Health ratio | 1.2 (HIGH — below 1.25 band) |
| Projected net yield (1y) | ~$4.91 |
| Treasury panel (USDC-style, 90% / 4.5%) | ~$3,800 backing → ~$171/yr issuer yield |

---

## Status

| Work package | Status |
|--------------|--------|
| WP-0 Scaffold | Done |
| WP-1 Database schema | Done |
| WP-2 Java core simulator | Done |
| WP-3 Market data + chart APIs | Done |
| WP-4 Wallet + audit lite | Done |
| WP-5 Agent core (Java Spring AI) | In progress (ETH-T22) |
| WP-6 API contracts + typegen | Done |
| WP-7 Frontend MVP | Done |
| WP-8 Auth + saved state | Done |
| WP-9 CI | Done |
| WP-10 Deployment + smoke | Ready (configure env + deploy) |
| WP-11 Advanced chart membership | Backlog |

**Implemented Java API (today)**

- `GET /health`
- `GET /actuator/health` (degraded RPC/DB/LLM when unconfigured)
- `GET /api/price/eth`
- `GET /api/yields?asset=USDC`
- `POST /api/simulations`
- `GET /api/charts/simulation-projection`
- `GET /api/charts/liquidation-band`
- `GET /api/charts/health-ratio`
- `GET /api/wallet/{address}/stablecoins`
- `GET /api/audit/{address}` (+ CSV/JSON export)
- Agent (Java stub): `POST /agent/recommend-yield`, `POST /agent/parse-goal`, `POST /agent/summarize-audit`

---

## Quick start

**Prerequisites:** Java 25, Maven 3.9+, Node 24+, Docker (optional).

```bash
git clone https://github.com/AlanL0/eth-stablecoin-simulator.git
cd eth-stablecoin-simulator
cp .env.example .env   # fill ETH_RPC_URL etc. locally; never commit .env

make stop              # stop: OK
make all               # Postgres + Java + frontend (background)
make curl-price        # ETH price + source
make test              # Java + frontend test suites
make smoke             # end-to-end smoke (after make all)
```

Foreground (one service per terminal):

```bash
make java-run          # http://localhost:8080
make web-run           # http://localhost:3000
```

Docker Java + Postgres only:

```bash
make dev-build         # Postgres :54329 + Java API :8080
```

Database (optional until persistence features ship):

```bash
export DATABASE_URL='postgresql://postgres:postgres@localhost:54329/ethsim'
make db-apply && make db-verify
```

See `Makefile` (`make help`) for `down`, `reset-db`, and chart curl helpers.

---

## Configuration

Copy `.env.example` — never commit secrets.

| Variable | Purpose |
|----------|---------|
| `ETH_RPC_URL` | Ethereum RPC for Chainlink ETH/USD reads |
| `PUBLIC_PRICE_API_URL` | HTTP fallback (e.g. CoinGecko-style JSON) |
| `STATIC_ETH_PRICE_USD` | Last-resort price (default `3800`) |
| `DATABASE_URL` | Postgres for schema apply / future persistence |
| `ALLOWED_ORIGINS` | CORS for local frontend |
| `LLM_API_KEY` | Optional Spring AI credentials (ETH-T22+) |

---

## Project layout

```text
frontend/         Next.js (scaffold)
java-service/     Spring Boot 4 simulator + charts + market data + agent stub
db/               Versioned SQL + apply/verify scripts
scripts/          Smoke test, fixture sync
```

---

## Contributing

Issues and PRs welcome. Please do not commit API keys, RPC URLs with credentials, or `.env` files. Run `make java-test` before opening Java changes.

---

## Disclaimer

This project is for **education and personal modelling only**. Outputs use simplified assumptions (e.g. linear stability fees). They are not financial, tax, or legal advice. Issuer/Treasury panels are illustrative — not official fiscal or issuer data. Always verify live protocol terms before using real collateral.

---

## License

This project is licensed under the [MIT License](LICENSE).

Copyright (c) 2026 AlanL0