# ethStable Coin Simulator — Agent Instructions

Re-architecture reset: **2026-06-18**. Active work is **ETH-T16–T54** only (T42–T54 are the 2026-07-01 hardening wave). Legacy ETH-T01–T15 are archived and must not be resumed.

**Ticket tracking:** Use GitHub issues/PRs or a local PM system — do not commit ticket boards, agent prompts, or review artifacts to this public repo.

**Baseline:** T16 complete (`e1ad97c`). Active phase: deterministic backend (T17–T21 done); next **ETH-T22** (Spring AI orchestration).

## Immutable rules

1. **Java is the financial authority.** Browser and LLM never derive debt, risk, rate, yield, or chart values.
2. **No floating-point Java finance.** Use `BigDecimal`, shared `MathContext`, named scales, explicit APR/APY conventions.
3. **No Python runtime.** Spring AI orchestrates in Java; Web3j uses repository-owned Spring beans (no legacy starter).
4. **Bound blocking I/O.** Web3j, JDBC, and model calls use virtual threads, timeouts, concurrency bounds, deterministic fallbacks.
5. **Finalized data with provenance.** Live values expose source, contract, chain/block, observation time, methodology, stale state.
6. **AI has least authority.** Read-only tools, budgets, validation; AI failure must not block deterministic simulation.
7. **Failure isolation.** RPC, database, or LLM failure cannot silently invent data.

## Approved stack (do not substitute)

| Layer | Version / policy |
|---|---|
| JVM | Java 25 LTS (Java 21 only as T16 bridge) |
| Backend | Spring Boot 4.1.0, Spring AI 2.0.0, Web3j 5.0.3, springdoc 3.0.3 |
| Data/test | Flyway 12.6.2, Testcontainers 2.0.5, WireMock 3.13.2 stable |
| Frontend | Node 24, Next.js 16.2.9, React 19.2.7, Recharts 3.8.1, TS 6.0.3 |
| AI | Spring AI only — no LangChain4j, no Python agent |

## Ticket workflow

1. Read the ticket **Current state** and confirm against the live repo before editing.
2. Check dependencies on the board — do not skip phase gates.
3. Write/extend required tests first; implement only ticket scope.
4. Run the ticket **acceptance gate** exactly as written.
5. Fill the ticket **Verification log**; set `Status: Done`; append `Completed.md`; archive ticket.
6. One focused commit per ticket when practical: `ETH-T<n>: <outcome>`.

## Execution waves (parallelism limits)

| Wave | Tickets | Parallel? |
|---|---|---|
| 1 | T16 | Done |
| 3 | T17 → T18∥T19 → T25 → T26 | Done |
| 4+ | T20–T22, T24… | Per index |
| 4 | T20 → T21; T25 when T17+T18 done; T26 when T19–T21 done | Partial |
| 5 | T22 → T23 → T24 | Sequential core |
| 6+ | T29–T41 per index | See board |

Frontend/UI work before **T18** (chart contract) or **T22** (agent API) will be discarded — do not start early.

## Multi-agent orchestration

When running parallel roles, the **parent session is the orchestrator**. It does not implement — it spawns subagents, tracks gates, and merges worktrees.

### Role assignment

| Role | Persona | Scope | Isolation | Capability |
|---|---|---|---|---|
| Backend | `backend` | `java-service/`, `db/`, scripts touching Java | `worktree` | `all` |
| Frontend | `frontend` | `frontend/` | `worktree` | `all` |
| Staff Engineer | `staff-engineer` | Code review before QA | shared | `read-only` |
| QA | `qa` | **Runs** acceptance gates in shell (`mvn test`, scans) | shared | `all` — **never read-only** |
| Product Mgmt | `pm` | Ticket closeout (external tracker) | shared | `read-write` |
| UI/UX | `ui-ux` | `frontend/` layout, copy, a11y | `worktree` when implementing | `all` |

### Spawn convention

Prefix subagent `description` with the role tag so the tasks pane shows it:

```
[Backend] ETH-T17 strict BigDecimal engine
[Staff Engineer] Review ETH-T17 commits
[QA] Run T17 acceptance gate (mvn test in shell)
[PM] Update verification log for T17
```

Use `background: true` and `isolation: worktree` for parallel implementers. Never run two write-capable subagents on the same paths without worktree isolation.

### Ticket completion pipeline (mandatory order)

```
PM (unblocked?) → Backend/Frontend (implement) → Staff Engineer (review)
    → [if CHANGES_REQUESTED: Backend fixes → re-review]
    → QA (run gates in shell) → PM (closeout)
```

| Step | Gate to proceed |
|---|---|
| Staff Engineer | Verdict **APPROVED** (zero blockers) — review notes kept locally, not in repo |
| QA | **PASS** with command output (`mvn -q test` exit 0, scans clean) |
| PM | Both above green; then update verification log + archive |

**Orchestrator rules:**

- Never spawn QA with `readonly: true` or `capability_mode: read-only` — QA must execute `mvn test`.
- Never trust implementer or orchestrator gate claims — only QA shell output counts.
- Never spawn PM closeout before Staff Engineer **APPROVED** and QA **PASS**.
- Orchestrator may verify informally but cannot substitute for QA.

### Merge discipline

1. Staff Engineer **APPROVED** on the worktree branch.
2. QA runs acceptance gate on the same branch **before** merge.
3. Orchestrator merges worktree → main only after both pass.
4. PM updates external ticket status only after Staff Engineer APPROVED + QA PASS evidence.

## Monitoring progress (no separate web dashboard)

Grok does not ship a browser dashboard for multi-agent runs. Use the **TUI**:

| What | How |
|---|---|
| Live subagent status | **Ctrl+B** — tasks pane (running/completed subagents, background jobs, task IDs) |
| Orchestrator todos | **Ctrl+T** — todos pane |
| Subagent transcript | **Enter** on a subagent block in parent scrollback, or open from tasks pane |
| Goal progress | `/goal status` when goal mode is active |
| Agent/persona config | `/config-agents` or `/agents` (configuration only — not progress) |
| Separate role sessions | **Ctrl+S** session picker — each worktree session appears independently |

Subagent blocks in scrollback show live activity (e.g. `mvn test`, tool name) and color-coded completion state.

## Repo layout (target)

```
frontend/       Next.js — rendering only; no finance math
java-service/   Spring Boot — simulation, charts, chain reads, Spring AI
db/             Flyway migrations, Supabase SQL
```

No Python runtime in the target architecture.