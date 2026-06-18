# ETH-T20 — Staff Engineer Review

**Verdict: APPROVED**  
**Blockers: 0**  
**Branch:** `main` @ `dc1200f`  
**Reviewer:** Staff Engineer (Wave 3)

## Scope reviewed

- Protocol adapters: Chainlink, Sky, Liquity, Aave/sGHO
- `ProtocolAdapterConfig`, `BlockchainConfig` optional Web3j wiring
- Removal of `PersistenceConfig` (Spring Boot 4.1 REGISTER_BEAN fix)

## Immutable rules

| Rule | Result |
|---|---|
| Java finance authority | PASS |
| BigDecimal only in finance paths | PASS |
| Bounded I/O (timeouts, virtual threads) | PASS |
| Failure isolation — no silent invented data | PASS |

## Notes

- Adapters return explicit unavailable reasons; block-pinned `fetchQuotesAtBlock` preserves provenance.
- `Web3j` registers only for HTTP RPC URLs; protocol adapter list empty without RPC — tests stay green.
- `MainnetProtocolSmokeTest` correctly opt-in.

## QA gate

```bash
cd java-service && mvn -q test -Dtest='*Chainlink*Adapter*,*Sky*Adapter*,*Liquity*Adapter*,*Aave*Adapter*'
```