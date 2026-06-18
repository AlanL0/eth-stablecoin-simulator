# ETH-T19 — Staff Engineer Review

| Field | Value |
|---|---|
| **Ticket** | ETH-T19 — Flyway PostgreSQL CQRS |
| **Branch** | `main` |
| **Commit** | `da60828` |
| **Re-verified** | 2026-06-18 @ `main` |
| **Verdict** | **APPROVED** — 0 blockers |
| **Grade** | **D+ → B+** (review expanded with evidence) |

## Immutable rules

| Rule | Result |
|---|---|
| Finalized observations with provenance | PASS |
| Idempotent uniqueness constraints | PASS — Flyway V2 migration |
| No float finance in persistence | PASS — `BigDecimal` columns |
| Failure isolation | PASS — repos `@ConditionalOnBean(DataSource)` |

## Evidence on `main`

- `V2__cqrs_read_model.sql` — price/rate observations, cursors, source health
- `FlywayMigrationTest`, `*RepositoryTest`, `*ReadModelTest` — Postgres Testcontainers
- Ingestion repos wired for T21 without `PersistenceConfig` scan hack

## QA gate

```bash
cd java-service && mvn -q test -Dtest='*Flyway*,*ReadModel*,*Observation*,*Cursor*,*SourceHealth*'
```