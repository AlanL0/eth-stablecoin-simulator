# Frontend

Next.js 14+ App Router, TypeScript, Tailwind, Recharts.

## Responsibilities

- Simulator UI (USD/ETH collateral toggle)
- Render `ChartSpecV1` from Java (no financial math in browser)
- Treasury education panel
- Wallet, audit, AI panel (WP-7)
- Auth + saved sims (WP-8)

## Dev

```bash
npm install
cp ../.env.example .env.local   # fill NEXT_PUBLIC_* only
npm run dev
```

## Generated types

Offline generation from committed OpenAPI snapshots:

```bash
npm run gen:types
```

Refresh snapshots from running services:

```bash
REFRESH_OPENAPI=1 npm run gen:types
```

## Acceptance gates

```bash
npm run typecheck && npm test && npm run build
```