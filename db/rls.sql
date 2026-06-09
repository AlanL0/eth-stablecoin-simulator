-- Row Level Security policies (WP-8)
-- Status: implementation (WP-8) — not run by db/apply.sh
-- Requires: db/schema.sql already applied.
-- Assumes: profiles.id = auth.users.id (standard Supabase pattern).

-- ---------------------------------------------------------------------------
-- Profiles
-- ---------------------------------------------------------------------------

alter table profiles enable row level security;

create policy profiles_select_own
  on profiles for select
  using (auth.uid() = id);

create policy profiles_insert_own
  on profiles for insert
  with check (auth.uid() = id);

create policy profiles_update_own
  on profiles for update
  using (auth.uid() = id)
  with check (auth.uid() = id);

-- ---------------------------------------------------------------------------
-- Home layouts
-- ---------------------------------------------------------------------------

alter table home_layouts enable row level security;

create policy home_layouts_select_own
  on home_layouts for select
  using (auth.uid() = user_id);

create policy home_layouts_insert_own
  on home_layouts for insert
  with check (auth.uid() = user_id);

create policy home_layouts_update_own
  on home_layouts for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy home_layouts_delete_own
  on home_layouts for delete
  using (auth.uid() = user_id);

-- ---------------------------------------------------------------------------
-- Simulations
-- ---------------------------------------------------------------------------

alter table simulations enable row level security;

-- Signed-in users read their own rows. Anonymous rows (user_id is null) are
-- readable for intentional demo/share links only. Frontend anon key cannot INSERT null.
-- Java service role must not persist wallet-tied or sensitive simulations with user_id null.
create policy simulations_select_own_or_anonymous
  on simulations for select
  using (user_id is null or user_id = auth.uid());

create policy simulations_insert_own
  on simulations for insert
  with check (user_id = auth.uid());

create policy simulations_update_own
  on simulations for update
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

create policy simulations_delete_own
  on simulations for delete
  using (user_id = auth.uid());

-- ---------------------------------------------------------------------------
-- Tables WITHOUT RLS in V1 (service role access from Java/Python)
-- ---------------------------------------------------------------------------
-- agent_runs, feedback_entries, yield_snapshots, wallet_snapshots,
-- transfer_events, protocol_presets
--
-- Java and Python use DATABASE_URL with service role — bypasses RLS.
-- Never expose service role key to the browser.

-- ---------------------------------------------------------------------------
-- New user bootstrap (optional trigger — enable if using Supabase Auth)
-- ---------------------------------------------------------------------------

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, display_name, created_at, updated_at)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'display_name', split_part(new.email, '@', 1)),
    now(),
    now()
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

-- Uncomment after Supabase Auth hookup:
-- create trigger on_auth_user_created
--   after insert on auth.users
--   for each row execute function public.handle_new_user();