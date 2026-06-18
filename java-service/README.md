# Java Service

Spring Boot 4.1, Java 25, web3j 5, Spring AI 2 (stub until ETH-T22).

## Responsibilities

- Simulation math (single source of numeric truth)
- ETH price chain (Chainlink via web3j)
- ChartSpecV1 builders (personal + treasury context)
- Wallet balances + audit lite
- Agent stub endpoints (`/agent/*`) with deterministic fallbacks
- OpenAPI at `/v3/api-docs`

## Dev

From repo root:

```bash
make java-test      # mvn test
make java-run       # spring-boot:run
make curl-sim       # POST sample simulation
make dev-build      # Docker: postgres + java-service
```

Or locally (**Java 25 required**, must run inside `java-service/`):

```bash
cd java-service && mvn test
cd java-service && mvn spring-boot:run
```

From repo root (recommended):

```bash
make java-test
./scripts/java-test.sh
```

### Troubleshooting `mvn test`

| Error | Cause | Fix |
|---|---|---|
| `no POM in this directory` | Ran `mvn test` from repo root | `cd java-service && mvn test` or `make java-test` |
| `release version 25 not supported` / wrong Java | Terminal uses Java 21 or 17 | `export JAVA_HOME=$(brew --prefix openjdk@25)/libexec/openjdk.jdk/Contents/Home` |
| Enforcer: `Java 25 LTS required` | Same as above | Run `./scripts/check-java.sh` for setup hints |
| `EthRpcConfigLiveTest` skipped | Normal without `ETH_RPC_URL` | Not a failure (1 skipped expected) |

Default port: `8080`

### Endpoints

- `GET /health`
- `GET /actuator/health` — reports degraded RPC/DB/LLM when credentials absent
- `GET /api/price/eth` — ETH/USD with labelled source chain
- `GET /api/yields?asset=USDC` — labelled yield assumptions
- `POST /api/simulations` — simulation result + embedded chart specs
- `GET /api/charts/*` — chart specs
- `POST /agent/recommend-yield`, `/agent/parse-goal`, `/agent/summarize-audit` — deterministic AI stub

## Config

Copy variables from repo root `.env.example`. Never commit `application-local.yml` or real `ETH_RPC_URL`.