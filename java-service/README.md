# Java Service

Spring Boot 3, Java 21, web3j.

## Responsibilities

- Simulation math (single source of numeric truth)
- ETH price chain (Chainlink via web3j)
- ChartSpecV1 builders (personal + treasury context)
- Wallet balances + audit lite (WP-4)
- OpenAPI at `/v3/api-docs`

## Dev (WP-2+)

```bash
mvn test
mvn spring-boot:run
```

Default port: `8080`

## Config

Copy variables from repo root `.env.example`. Never commit `application-local.yml` or real `ETH_RPC_URL`.