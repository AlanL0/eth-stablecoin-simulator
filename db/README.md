# Database

Supabase Postgres 15.

## WP-1

Tracked implementation SQL will be added here (migrations or `schema.sql` copied from local planning drafts).

Local planning drafts (`db/*.sql` in gitignore) include:

- `schema.sql`, `feedback_backlog.sql`, `feedback_resolve.sql`, `product_events.sql`, `rls.sql`, `memberships.sql`

Apply when WP-1 completes:

```bash
psql "$DATABASE_URL" -f db/schema.sql
```

Never commit `DATABASE_URL` or service role keys.