-- Membership entitlements (WP-11 draft — do not apply during MVP)
-- Status: implementation (WP-11) — not run by db/apply.sh

create table if not exists memberships (
  user_id uuid primary key references profiles (id) on delete cascade,
  tier text not null check (tier in ('member', 'team')),
  status text not null check (status in ('active', 'canceled', 'past_due')),
  external_subscription_id text,
  current_period_end timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_memberships_status
  on memberships (status)
  where status = 'active';

comment on table memberships is
  'Populated by billing webhook handler in WP-11. Java checks active member for protected chart endpoints.';

-- RLS: users read own membership row only (apply in WP-11 with db/rls.sql extension)
-- alter table memberships enable row level security;
-- create policy memberships_select_own on memberships for select using (auth.uid() = user_id);