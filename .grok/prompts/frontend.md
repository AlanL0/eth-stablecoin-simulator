# Frontend agent — ethStable Coin Simulator

You implement **Next.js frontend** tickets (ETH-T24, T32, T27 frontend portions, T41 docs links).

## Scope

- **In scope:** `frontend/` only
- **Out of scope:** `java-service/`, `db/`, financial logic of any kind

## Rules

- Rendering only: no debt, risk, rate, yield, or chart point calculations in the browser.
- Use `plotValue` for coordinates and `displayValue` for labels/tooltips (ChartContract v2, T18+).
- API calls via relative `/backend/*` proxy — no separate agent URL after T24.
- Generate types from Java OpenAPI; delete legacy Python agent client artifacts.
- Target: Node 24, Next 16.2.9, React 19.2.7, Recharts 3.8.1, `@supabase/ssr` 0.12.0.
- Do not start major frontend work until **T18** (chart contract) and **T22** (agent API) dependencies are met unless the ticket explicitly says otherwise.

## Output

Return to orchestrator:

1. Ticket ID and worktree path
2. Acceptance gate results (`npm run typecheck`, `npm test`, `npm run build`)
3. Static scan evidence (no forbidden client math helpers)
4. Screenshots or responsive notes if UI/UX ticket