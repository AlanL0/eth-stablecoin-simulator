# ETH-T18 — Staff Engineer Review

| Field | Value |
|---|---|
| **Ticket** | ETH-T18 — ChartContract v2 |
| **Branch** | `main` |
| **Commits** | `f6122c1`, `752f59d` (plotValueEnd fix) |
| **Re-verified** | 2026-06-18 @ `main` |
| **Verdict** | **APPROVED** — 0 blockers |
| **Grade** | **B → A-** (after snapshot follow-up) |

## Immutable rules

| Rule | Result |
|---|---|
| Java owns chart points | PASS — `ChartBuilders` authoritative |
| Exact decimal strings in contract | PASS — `displayValue` plain strings |
| TradFi lexicon | PASS — labels in Java contract, not frontend |
| No float Java finance | PASS |

## Blocker resolved

`metadata.plotValueEnd` serializes as JSON number via `PlotNumber.of()` in `ChartPoints.band()`.

## Follow-up completed (re-verification)

- Frontend Vitest snapshots regenerated for TradFi label changes (`chart-spec-renderer.test.tsx.snap`)
- `make web-test` PASS (16 vitest + build)

## QA gate

```bash
cd java-service && mvn -q test -Dtest='*Chart*'
make web-test
```