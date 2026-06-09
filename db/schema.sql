-- ETH Stablecoin Simulator — core schema (WP-1)
-- Apply first: psql "$DATABASE_URL" -f db/schema.sql

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------------
-- Protocol and market data
-- ---------------------------------------------------------------------------

create table if not exists protocol_presets (
  name text primary key,
  display_name text not null,
  collateral_ratio numeric(10, 4) not null,
  liquidation_ratio numeric(10, 4) not null,
  stability_fee_pct numeric(10, 4) not null,
  enabled boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists yield_snapshots (
  id uuid primary key default gen_random_uuid(),
  asset text not null,
  protocol text not null,
  apy_pct numeric(10, 4) not null,
  source text not null,
  risk_tier text not null,
  observed_at timestamptz not null default now()
);

create index if not exists idx_yield_snapshots_asset_observed
  on yield_snapshots (asset, observed_at desc);

-- ---------------------------------------------------------------------------
-- Simulations and wallet/audit
-- ---------------------------------------------------------------------------

create table if not exists simulations (
  id uuid primary key default gen_random_uuid(),
  user_id uuid,
  input jsonb not null,
  result jsonb not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_simulations_user_created
  on simulations (user_id, created_at desc);

create table if not exists wallet_snapshots (
  id uuid primary key default gen_random_uuid(),
  address text not null,
  token text not null,
  balance numeric(38, 18) not null,
  source text not null,
  fetched_at timestamptz not null default now()
);

create index if not exists idx_wallet_snapshots_address_token_fetched
  on wallet_snapshots (address, token, fetched_at desc);

create table if not exists transfer_events (
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

create index if not exists idx_transfer_events_address_occurred
  on transfer_events (address, occurred_at desc);

-- ---------------------------------------------------------------------------
-- Auth-related (RLS enabled in WP-8 via db/rls.sql)
-- ---------------------------------------------------------------------------

create table if not exists profiles (
  id uuid primary key,
  display_name text,
  avatar_url text,
  default_protocol text references protocol_presets (name),
  preferred_currency text not null default 'USD',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists home_layouts (
  user_id uuid primary key references profiles (id) on delete cascade,
  layout jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Agent observability and feedback backlog
-- ---------------------------------------------------------------------------

create table if not exists agent_runs (
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

create table if not exists feedback_entries (
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

create unique index if not exists uq_feedback_chart_request_open
  on feedback_entries (kind, normalized_label)
  where resolved = false and kind = 'chart_request' and normalized_label is not null;

create index if not exists idx_feedback_entries_open_chart
  on feedback_entries (kind, normalized_label)
  where resolved = false;

-- ---------------------------------------------------------------------------
-- Seed data (Java app config is authoritative in V1; DB seed for future admin)
-- ---------------------------------------------------------------------------

insert into protocol_presets (name, display_name, collateral_ratio, liquidation_ratio, stability_fee_pct)
values
  ('maker_sky', 'Maker/Sky-style vault', 1.80, 1.50, 5.00),
  ('liquity', 'Liquity-style borrowing', 2.00, 1.10, 0.50),
  ('aave_gho', 'Aave/GHO-style borrowing', 2.20, 1.25, 4.00),
  ('custom', 'Custom', 2.00, 1.50, 5.00)
on conflict (name) do nothing;