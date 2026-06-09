# Java Service

Spring Boot 3, Java 21, web3j.

## Responsibilities

- Simulation math (single source of numeric truth)
- ETH price chain (Chainlink via web3j)
- ChartSpecV1 builders (personal + treasury context)
- Wallet balances + audit lite (WP-4)
- OpenAPI at `/v3/api-docs`

## Dev (WP-2+)

From repo root:

```bash
make java-test      # mvn test
make java-run       # spring-boot:run
make curl-sim       # POST sample simulation
make dev-build      # Docker: postgres + java-service
```

Or locally:

```bash
mvn test
mvn spring-boot:run
```

Default port: `8080`

### Endpoints

- `GET /health`
- `GET /api/price/eth` — ETH/USD with labelled source chain (cache → Chainlink → public API → static)
- `GET /api/yields?asset=USDC` — labelled yield assumptions (seed or static fallback)
- `POST /api/simulations` — simulation result + embedded chart specs + optional `treasuryContext`
- `GET /api/charts/simulation-projection` — yield chart spec (inline params)
- `GET /api/charts/liquidation-band` — uses Java-resolved ETH price (client hint advisory only)
- `GET /api/charts/health-ratio` — health ratio sweep chart spec

## Config

Copy variables from repo root `.env.example`. Never commit `application-local.yml` or real `ETH_RPC_URL`.