-- Data API grants for frontend auth tables (run after schema.sql + rls.sql).
-- See: https://supabase.com/docs/guides/api/securing-your-api

grant usage on schema public to anon, authenticated, service_role;

grant select, insert, update on table public.profiles to authenticated;
grant select, insert, update, delete on table public.home_layouts to authenticated;
grant select, insert, update, delete on table public.simulations to authenticated;

-- Service role for Java/Python persistence (bypasses RLS when used server-side).
grant select, insert, update, delete on all tables in schema public to service_role;
grant usage, select on all sequences in schema public to service_role;