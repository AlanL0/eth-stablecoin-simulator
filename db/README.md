# Database (WP-1)

Supabase Postgres 15 (or local Postgres for dev).

## Supabase project setup (2026)

Use this when wiring **ETH-T10 Step 5** (auth + saved simulations).

### 1. Create project

1. [supabase.com/dashboard](https://supabase.com/dashboard) → **New project** (Postgres 15, pick region).
2. Wait for provisioning (~2 minutes).

### 2. API keys (browser vs server)

Supabase now uses **publishable** (`sb_publishable_...`) and **secret** (`sb_secret_...`) keys. Legacy **anon** / **service_role** JWT keys still work but are [deprecated by end of 2026](https://supabase.com/docs/guides/getting-started/api-keys).

| Variable | Where | Key type |
|---|---|---|
| `NEXT_PUBLIC_SUPABASE_URL` | Frontend (`.env`, Vercel) | Project URL |
| `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` | Frontend only | Publishable (preferred) |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Frontend only | Legacy anon (fallback) |
| `DATABASE_URL` | Java/Python only | **Direct** or **pooler** URI with DB password — never in browser |

Copy URL + publishable key from **Connect** (Next.js tab) or **Settings → API Keys**.

### 3. Auth (magic link)

1. **Authentication → Providers → Email** — enable Email; magic link is the default OTP flow.
2. **Authentication → URL configuration** — add redirect URLs:
   - `http://localhost:3000/auth/callback`
   - `http://localhost:3000/profile`
   - Your production URL + `/auth/callback` when deploying to Vercel.

The frontend sends magic links to `/auth/callback?next=/profile` (PKCE code exchange via `@supabase/ssr`).

### 4. Database connection strings

From **Settings → Database → Connection string**:

| Use case | Mode | Notes |
|---|---|---|
| `make db-apply` / `psql` migrations | **Direct** or **Session pooler** | Use the URI with your DB password |
| Serverless Java/Python (later) | **Transaction pooler** | Port 6543, `?pgbouncer=true` |

Put the migration URI in root `.env` as `DATABASE_URL`. Do **not** commit it.

### 5. Apply schema + RLS

```bash
# From repo root — loads .env via make
make db-apply
make db-verify
psql "$DATABASE_URL" -f db/rls.sql
```

In the Supabase SQL editor, uncomment the `on_auth_user_created` trigger at the bottom of `db/rls.sql` so new sign-ups get a `profiles` row.

### 6. Verify

```bash
make test-supabase          # URL + key + auth health
make frontend-run           # or make all
```

Browser checks:

1. Unsigned → run simulation at `/` → success (no Supabase writes).
2. `/login` → magic link → lands on `/profile`.
3. Save simulation → reload → row appears.
4. Incognito → cannot see another user's saves.

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

## RLS verification (WP-8 / ETH-T06)

Apply `db/rls.sql` after `schema.sql` when Supabase Auth is enabled:

```bash
psql "$DATABASE_URL" -f db/rls.sql
```

### Prerequisites

- `profiles.id` matches `auth.users.id` (standard Supabase pattern)
- Frontend uses **publishable key** (`NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`) or legacy **anon key** (`NEXT_PUBLIC_SUPABASE_ANON_KEY`)
- Optional: enable `on_auth_user_created` trigger in `rls.sql` for profile bootstrap

### Policy checks (Supabase SQL editor)

Run as authenticated user A:

```sql
select * from profiles;              -- only user A
select * from home_layouts;          -- only user A
select id, user_id from simulations; -- user A rows + user_id is null demo rows
```

### Negative tests

| Test | Actor | Action | Expected |
|---|---|---|---|
| Cross-user read | User A | `select * from simulations where user_id = <user B>` | 0 rows |
| Cross-user update | User A | `update profiles set display_name = 'x' where id = <user B>` | 0 rows affected |
| Anon insert simulation | Anon key | `insert into simulations (user_id, input, result) values (null, '{}', '{}')` | denied |
| Signed insert | User A | insert with `user_id = auth.uid()` | allowed |
| Service role cache | Java/Python | insert via service `DATABASE_URL` | allowed (bypasses RLS) |

### Anonymous simulator path

- Unsigned users can run simulations without Supabase writes (frontend calls Java API only)
- Saved simulations require sign-in; inserts use `user_id = auth.uid()` under `simulations_insert_own`
- Sign-in does not block anonymous simulator state on the home page

### Browser checks

1. Open app unsigned → run simulation → success
2. Sign in → save simulation → appears on `/profile`
3. Sign out → saved list hidden; anonymous sim still works
4. Second account → cannot see first account's saved simulations

### Service role isolation

- Do not expose `DATABASE_URL` or service role keys to the frontend/Vercel
- Do not expose `INTERNAL_API_KEY` to the browser bundle

### Production persistence checklist

Before enabling saved simulations in production:

- Never store wallet-tied or private simulation payloads with `user_id = null` (anonymous rows are intentionally readable by any signed-in user per `simulations_select_own_or_anonymous`).
- Keep Java/Python on service-role `DATABASE_URL` only — never in `NEXT_PUBLIC_*` env vars.
- Tables without RLS (`agent_runs`, `feedback_entries`, etc.) must remain server-only via service role.
- Rotate Supabase DB password and API keys if they were shared during setup.