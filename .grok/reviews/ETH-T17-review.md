# ETH-T17 Staff Engineer Review

| Field | Value |
|---|---|
| **Ticket** | ETH-T17 — Strict BigDecimal financial engine |
| **Branch** | `main` (merged from `feat/t17`) |
| **Commit** | `ea49d77` |
| **Re-verified** | 2026-06-18 @ `main` |
| **Reviewer** | Staff Engineer |
| **Grade** | **B+** |

## Verdict

**APPROVED** — **0 blockers**

QA PASS on `main`: `make java-test` 181/0; float scan empty; `PrecisionQaTest` green.

## Immutable constraints

| # | Constraint | Result |
|---|---|---|
| 1 | NO PYTHON | PASS |
| 2 | STRICT MATH | PASS — 0 forbidden float/double in `java-service/src` |
| 3 | MECHANICAL SYMPATHY | PASS |
| 4 | DUMB FRONTEND | PASS (N/A) |
| 5 | TRADFI LEXICON | PASS (N/A) |
| 6 | AGENT ROBUSTNESS | PASS (N/A) |

## Non-goals

| Non-goal | Result |
|---|---|
| No browser charting | PASS |
| No protocol ingestion | PASS |
| No ChartContract v2 plotting | PASS |

## Findings

| Severity | Location | Issue | Suggestion |
|---|---|---|---|
| major | test chart fixtures | JSON numbers vs API string decimals | Update in T25/T18 |
| major | tests | No byte-equivalent JSON determinism test | Track in T25 |
| minor | blockchain readers | Local setScale vs FinancialMath | Optional unify later |
| nit | ChartModels | Jackson 2 annotations only | Acceptable |

## Summary

`FinancialMath` + full BigDecimal refactor across simulation, charts, market, treasury, wallet, agent. `JacksonConfig` plain-string serialization. OpenAPI financial fields as strings. Ready for QA shell gate.