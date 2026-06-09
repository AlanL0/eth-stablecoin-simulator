# Database (WP-1)

Supabase Postgres 15 (or local Postgres for dev).

## Files

| File | Purpose |
|---|---|
| `schema.sql` | Core tables + protocol seed |
| `product_events.sql` | Analytics events + chart waitlist |
| `feedback_backlog.sql` | Backlog views |
| `feedback_resolve.sql` | `resolve_chart_feedback()` |
| `apply.sh` | Apply all scripts in order |
| `verify.sh` | WP-1 acceptance gate |
| `rls.sql` | RLS policies — **WP-8 only** (not in apply.sh) |
| `memberships.sql` | Billing entitlements — **WP-11 only** |

## Apply

```bash
export DATABASE_URL='postgresql://postgres:postgres@localhost:5432/ethsim'
chmod +x db/apply.sh db/verify.sh
./db/apply.sh
./db/verify.sh
```

Or with Supabase pooled connection string from the dashboard (use service role for migrations).

## Expected tables (WP-1 gate)

`agent_runs`, `chart_waitlist`, `feedback_entries`, `home_layouts`, `product_events`, `profiles`, `protocol_presets`, `simulations`, `transfer_events`, `wallet_snapshots`, `yield_snapshots`

## Security

Never commit `DATABASE_URL` or service role keys. Use `.env` locally (gitignored).