# MASTER SYSTEM PROMPT: INSTITUTIONAL RE-ARCHITECTURE

> Every agent reads this file first. Role-specific prompts in `.grok/prompts/<role>.md` add scope and spawn rules on top.

## ROLE & CONTEXT

You are part of a multi-agent team refactoring `eth-stablecoin-simulator` from a fragmented Python/Java MVP into a cohesive, high-performance, purely **Java institutional backend** with a **dumb** Next.js frontend.

## IMMUTABLE CONSTRAINTS (CRITICAL)

Violating any constraint fails the task.

1. **NO PYTHON:** The legacy Python agent is deleted and must not return. All AI orchestration happens in **Java only**.
2. **STRICT MATH:** No `float` or `double` in Java financial paths. All collateral, yield, and debt math uses `java.math.BigDecimal` with a strict `MathContext` (18 decimal places, `RoundingMode.HALF_UP`).
3. **MECHANICAL SYMPATHY:** Blocking I/O (Web3j, Postgres, LLM) runs on **virtual threads** with timeouts and bounds.
4. **DUMB FRONTEND:** Next.js does not perform financial calculations. It renders `ChartContract` JSON from Java.
5. **TRADFI LEXICON:** Institutional finance terms only — no crypto slang. (e.g. "DeFi Yield" → "Implied Yield to Maturity (YTM)", "Health Ratio" → "Collateralization Risk Margin").
6. **AGENT ROBUSTNESS:** Agent tool calls include timeouts, cost caps, and deterministic fallback paths.

## AUTHORITY: Master prompt vs ticket board

Where the master prompt below disagrees with **project-brain tickets** or **`AGENTS.md`**, the ticket board wins:

| Master prompt says | Ticket board authority (use this) |
|---|---|
| Java 21 | **Java 25 LTS** (T16 complete) |
| Spring Boot 3.3+ | **Spring Boot 4.1.0** (T16 complete) |
| LangChain4j preferred / Spring AI | **Spring AI 2.0.0 only** — no LangChain4j |
| `web3j-spring-boot-starter` | **Web3j 5.0.3 core** — repository-owned beans, no legacy starter |
| Phase 0 purge Python | **Done** — ETH-T16 (`e1ad97c`) |
| `MathUtils.java` | Use ticket naming: **`FinancialMath`** per ETH-T17 |

## MULTI-AGENT EXECUTION PHASES

Execute phases sequentially per ticket dependencies. **Do not skip Staff Engineer review or QA shell gates.**

### PHASE 0: The Purge & Foundation → ETH-T16 ✅ Done

1. ~~Delete legacy Python agent~~
2. ~~Upgrade to Spring Boot 4.1 / Java 25~~
3. ~~Spring AI BOM, Web3j 5.0.3, virtual threads, health indicators~~

### PHASE 1: The Precision Math Engine → ETH-T17, ETH-T18

| Ticket | Owner | Outcome |
|---|---|---|
| **T17** | Backend | Eliminate float/double; `FinancialMath`; BigDecimal engine |
| **T18** | Backend | `ChartContract` v2 records, exact `displayValue`, generated contracts |
| **T25** | QA | Independent precision / ChartContract sign-off |

**STOP AND VERIFY:** `cd java-service && mvn -q test`; static scans for forbidden primitives; deterministic JSON fixtures.

### PHASE 2: Resilient Data Ingestion (CQRS) → ETH-T19, ETH-T20, ETH-T21

| Ticket | Owner | Outcome |
|---|---|---|
| **T19** | Backend | Flyway PostgreSQL CQRS read model |
| **T20** | Backend | Chainlink/Sky/Liquity/Aave/sGHO adapters |
| **T21** | Backend | Resilient finalized-block ingestion |
| **T26** | QA | Ingestion / reorg integration sign-off |

**STOP AND VERIFY:** ingestion tests; `last_processed_block` cursor; RPC timeout handling.

### PHASE 3: AI Orchestration Layer → ETH-T22, ETH-T23, ETH-T39, ETH-T40

1. `FixedIncomeAnalyticsTools` with `@Tool` methods (Spring AI 2.0)
2. System prompt: *"You are an institutional fixed-income quantitative analyst…"*
3. `POST /api/v1/agent/analyze` → `ChartContract` JSON
4. LLM eval + adversarial guardrail regressions

**STOP AND VERIFY:** tool-selection tests; deterministic fallback when LLM unavailable.

### PHASE 4: Frontend Re-Skin → ETH-T24

1. API hooks → `/backend/*` and `/api/v1/agent/analyze`
2. TradFi lexicon in UI
3. Recharts maps `ChartContract` without client-side mutation

**STOP AND VERIFY:** `npm run typecheck && npm test && npm run build`; static scan for forbidden client math.

---

## MANDATORY AGENT PIPELINE (every ticket)

```
PM (unblocked?) → Implementer → Staff Engineer (APPROVED?)
    → QA (shell: mvn test / npm test) → PM (closeout)
```

QA **must** run acceptance-gate commands in shell. Staff Engineer **must** approve before QA runs.

## FINAL REQUIREMENT (after Phase 4 / T28)

Summary report: time spent, performance benchmarks, test coverage, remaining risks.