-- ETH Stablecoin Simulator — consolidated legacy schema (formerly db/*.sql)
-- Flyway V1 for clean databases; baseline version 1 for existing Supabase installs.

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------------
-- Protocol and market data
-- ---------------------------------------------------------------------------

create table protocol_presets (
  name text primary key,
  display_name text not null,
  collateral_ratio numeric(10, 4) not null,
  liquidation_ratio numeric(10, 4) not null,
  stability_fee_pct numeric(10, 4) not null,
  enabled boolean not null default true,
  created_at timestamptz not null default now()
);

create table yield_snapshots (
  id uuid primary key default gen_random_uuid(),
  asset text not null,
  protocol text not null,
  apy_pct numeric(10, 4) not null,
  source text not null,
  risk_tier text not null,
  observed_at timestamptz not null default now()
);

create index idx_yield_snapshots_asset_observed
  on yield_snapshots (asset, observed_at desc);

-- ---------------------------------------------------------------------------
-- Simulations and wallet/audit
-- ---------------------------------------------------------------------------

create table simulations (
  id uuid primary key default gen_random_uuid(),
  user_id uuid,
  input jsonb not null,
  result jsonb not null,
  created_at timestamptz not null default now()
);

create index idx_simulations_user_created
  on simulations (user_id, created_at desc);

create table wallet_snapshots (
  id uuid primary key default gen_random_uuid(),
  address text not null,
  token text not null,
  balance numeric(38, 18) not null,
  source text not null,
  fetched_at timestamptz not null default now()
);

create index idx_wallet_snapshots_address_token_fetched
  on wallet_snapshots (address, token, fetched_at desc);

create table transfer_events (
  id uuid primary key default gen_random_uuid(),
  address text not null,
  token text not null,
  tx_hash text not null,
  log_index integer not null,
  from_address text not null,
  to_address text not null,
  amount numeric(38, 18) not null,
  block_number bigint not null,
  occurred_at timestamptz not null,
  unique (tx_hash, log_index)
);

create index idx_transfer_events_address_occurred
  on transfer_events (address, occurred_at desc);

-- ---------------------------------------------------------------------------
-- Auth-related (RLS enabled separately via db/rls.sql)
-- ---------------------------------------------------------------------------

create table profiles (
  id uuid primary key,
  display_name text,
  avatar_url text,
  default_protocol text references protocol_presets (name),
  preferred_currency text not null default 'USD',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table home_layouts (
  user_id uuid primary key references profiles (id) on delete cascade,
  layout jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Agent observability and feedback backlog
-- ---------------------------------------------------------------------------

create table agent_runs (
  id uuid primary key default gen_random_uuid(),
  prompt_version text not null,
  provider text not null default 'unknown',
  model text not null,
  latency_ms integer not null,
  fallback_used boolean not null default false,
  fallback_reason text,
  estimated_cost_usd numeric(12, 6),
  token_usage jsonb not null default '{}'::jsonb,
  free_quota_used boolean not null default false,
  structured_output jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table feedback_entries (
  id uuid primary key default gen_random_uuid(),
  kind text not null check (kind in ('chart_request', 'tool_request', 'general')),
  message text not null,
  normalized_label text,
  request_count integer not null default 1 check (request_count >= 1),
  context jsonb not null default '{}'::jsonb,
  resolved boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index uq_feedback_chart_request_open
  on feedback_entries (kind, normalized_label)
  where resolved = false and kind = 'chart_request' and normalized_label is not null;

create index idx_feedback_entries_open_chart
  on feedback_entries (kind, normalized_label)
  where resolved = false;

-- ---------------------------------------------------------------------------
-- Product instrumentation + chart waitlist
-- ---------------------------------------------------------------------------

create table product_events (
  id uuid primary key default gen_random_uuid(),
  event_name text not null,
  session_id text,
  user_id uuid,
  properties jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index idx_product_events_name_created
  on product_events (event_name, created_at desc);

create index idx_product_events_session
  on product_events (session_id, created_at desc)
  where session_id is not null;

comment on table product_events is
  'Lightweight product analytics. No PII in properties by default.';

create table chart_waitlist (
  id uuid primary key default gen_random_uuid(),
  email text,
  normalized_label text not null,
  session_id text,
  user_id uuid,
  created_at timestamptz not null default now()
);

create unique index uq_chart_waitlist_label_email
  on chart_waitlist (normalized_label, lower(email))
  where email is not null;

create index idx_chart_waitlist_label
  on chart_waitlist (normalized_label, created_at desc);

comment on table chart_waitlist is
  'Users who want a chart before membership ships. Email optional.';

-- ---------------------------------------------------------------------------
-- Feedback backlog views
-- ---------------------------------------------------------------------------

create or replace view feedback_chart_backlog as
select
  fe.normalized_label,
  max(fe.message) as latest_message,
  sum(fe.request_count) as total_request_count,
  count(*) as open_entry_count,
  min(fe.created_at) as first_requested_at,
  max(fe.updated_at) as last_requested_at,
  (sum(fe.request_count) >= coalesce(
    nullif(current_setting('app.chart_escalation_threshold', true), '')::integer,
    3
  )) as escalated,
  jsonb_agg(
    jsonb_build_object(
      'id', fe.id,
      'requestCount', fe.request_count,
      'message', fe.message,
      'context', fe.context,
      'updatedAt', fe.updated_at
    )
    order by fe.updated_at desc
  ) as entries
from feedback_entries fe
where fe.kind = 'chart_request'
  and fe.resolved = false
  and fe.normalized_label is not null
group by fe.normalized_label
order by total_request_count desc, last_requested_at desc;

create or replace view feedback_chart_backlog_escalated as
select *
from feedback_chart_backlog
where escalated = true
order by total_request_count desc;

comment on view feedback_chart_backlog is
  'Unresolved chart requests grouped by normalized_label. Escalation when total_request_count >= 3.';

comment on view feedback_chart_backlog_escalated is
  'Subset at or above escalation threshold.';

-- ---------------------------------------------------------------------------
-- Feedback backlog resolution helper
-- ---------------------------------------------------------------------------

create or replace function resolve_chart_feedback(
  p_normalized_label text,
  p_resolution_note text default null
)
returns integer
language plpgsql
as $$
declare
  v_updated integer;
begin
  update feedback_entries
  set
    resolved = true,
    updated_at = now(),
    context = context || jsonb_build_object(
      'resolvedAt', now(),
      'resolutionNote', coalesce(p_resolution_note, 'Chart shipped')
    )
  where kind = 'chart_request'
    and normalized_label = p_normalized_label
    and resolved = false;

  get diagnostics v_updated = row_count;
  return v_updated;
end;
$$;

comment on function resolve_chart_feedback is
  'Marks all open chart_request rows for a label as resolved. Returns rows updated.';

-- ---------------------------------------------------------------------------
-- Seed data
-- ---------------------------------------------------------------------------

insert into protocol_presets (name, display_name, collateral_ratio, liquidation_ratio, stability_fee_pct)
values
  ('maker_sky', 'Maker/Sky-style vault', 1.80, 1.50, 5.00),
  ('liquity', 'Liquity-style borrowing', 2.00, 1.10, 0.50),
  ('aave_gho', 'Aave/GHO-style borrowing', 2.20, 1.25, 4.00),
  ('custom', 'Custom', 2.00, 1.50, 5.00)
on conflict (name) do nothing;