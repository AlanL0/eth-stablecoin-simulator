# Frontend

Next.js 14+ App Router, TypeScript, Tailwind, Recharts.

## Responsibilities

- Simulator UI (USD/ETH collateral toggle)
- Render `ChartSpecV1` from Java (no financial math in browser)
- Treasury education panel
- Wallet, audit, AI panel (WP-7)
- Auth + saved sims (WP-8)

## Dev (WP-7+)

```bash
npm install
cp ../.env.example .env.local   # fill NEXT_PUBLIC_* only
npm run dev
```

## Generated types (WP-6+)

```bash
npm run gen:types
```