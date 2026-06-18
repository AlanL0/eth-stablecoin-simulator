# ETH-T21 — Staff Engineer Review

**Verdict: APPROVED**  
**Blockers: 0**  
**Branch:** `main` @ `dc999e2`  
**Re-verified:** 2026-06-18  
**Grade:** **A**  
**Reviewer:** Staff Engineer (Wave 3)

## Scope reviewed

- `IngestionCoordinator`, persistence service, reorg handler, leader lock
- `IngestionConfig` / scheduler / wake signal
- Integration tests: cursor advance, contention, reorg, virtual-thread boundary

## Immutable rules

| Rule | Result |
|---|---|
| Java finance authority | PASS |
| BigDecimal only in observation mapping | PASS |
| Bounded I/O with timeouts and retries | PASS |
| Failure isolation — stale metadata, no duplicate rows | PASS |

## Notes

- Ingestion gated on `DATABASE_URL` **and** HTTP RPC (`hasHttpRpcUrl`) — aligns with T20 wiring; `make java-test` no longer requires live RPC.
- Advisory lease, transactional cursor commits, idempotent inserts, bounded reorg window.
- `FinalizedBlockReader` built from `ObjectProvider<Web3j>` — no orphan `EthCallClient` bean.

## QA gate

```bash
cd java-service && mvn -q verify -Dtest='*Ingestion*,*Cursor*,*Reorg*,*SourceHealth*,*Leader*'
```