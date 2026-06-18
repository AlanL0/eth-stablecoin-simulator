-- ETH-T19: PostgreSQL CQRS read model for finalized market ingestion cache.
-- Numeric values use numeric(38,18). Timestamps are UTC (timestamptz).

-- ---------------------------------------------------------------------------
-- Ingestion cursors and source health
-- ---------------------------------------------------------------------------

create table ingestion_cursors (
  source_key text not null,
  chain_id bigint not null,
  next_block bigint not null,
  last_finalized_block bigint,
  last_finalized_block_hash text,
  lease_owner text,
  lease_expires_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (source_key, chain_id),
  constraint chk_ingestion_cursors_next_block_nonneg check (next_block >= 0),
  constraint chk_ingestion_cursors_finalized_block_nonneg
    check (last_finalized_block is null or last_finalized_block >= 0)
);

create index idx_ingestion_cursors_lease_expiry
  on ingestion_cursors (lease_expires_at)
  where lease_expires_at is not null;

comment on table ingestion_cursors is
  'Durable ingestion cursor per source/chain with optional lease metadata for leader election.';

create table source_health (
  source_key text primary key,
  last_success_at timestamptz,
  last_failure_at timestamptz,
  consecutive_failures integer not null default 0,
  lag_blocks bigint,
  status text not null,
  reason text,
  updated_at timestamptz not null default now(),
  constraint chk_source_health_status
    check (status in ('healthy', 'degraded', 'unavailable')),
  constraint chk_source_health_consecutive_failures_nonneg
    check (consecutive_failures >= 0),
  constraint chk_source_health_lag_blocks_nonneg
    check (lag_blocks is null or lag_blocks >= 0)
);

create index idx_source_health_status_updated
  on source_health (status, updated_at desc);

comment on table source_health is
  'Latest health snapshot per upstream source; reason is sanitized for operator display.';

-- ---------------------------------------------------------------------------
-- Price observations
-- ---------------------------------------------------------------------------

create table price_observations (
  id uuid primary key default gen_random_uuid(),
  base_asset text not null,
  quote_asset text not null,
  value numeric(38, 18) not null,
  source text not null,
  chain_id bigint not null,
  block_number bigint not null,
  block_hash text not null,
  round_id text,
  observed_at timestamptz not null,
  source_timestamp timestamptz,
  is_stale boolean not null default false,
  is_finalized boolean not null default true,
  is_reverted boolean not null default false,
  created_at timestamptz not null default now(),
  constraint chk_price_observations_block_number_nonneg check (block_number >= 0),
  constraint chk_price_observations_value_positive check (value > 0),
  constraint chk_price_observations_finalized_provenance check (
    not is_finalized
    or (block_hash is not null and length(trim(block_hash)) > 0)
  ),
  constraint chk_price_observations_reverted_state check (
    not is_reverted or is_finalized
  )
);

create unique index uq_price_observations_idempotent
  on price_observations (
    source,
    chain_id,
    block_number,
    base_asset,
    quote_asset,
    coalesce(round_id, '')
  );

create index idx_price_observations_pair_observed
  on price_observations (base_asset, quote_asset, observed_at desc);

create index idx_price_observations_source_chain_block
  on price_observations (source, chain_id, block_number desc);

create index idx_price_observations_finalized_lookup
  on price_observations (base_asset, quote_asset, source, block_number desc)
  where is_finalized = true and is_reverted = false;

comment on table price_observations is
  'Immutable finalized ETH/USD (and other pair) observations with block provenance.';

-- ---------------------------------------------------------------------------
-- Rate observations
-- ---------------------------------------------------------------------------

create table rate_observations (
  id uuid primary key default gen_random_uuid(),
  protocol text not null,
  product text not null,
  side text not null,
  annualized_value numeric(38, 18) not null,
  convention text not null,
  methodology text not null,
  lookback_window text,
  contract_address text,
  chain_id bigint not null,
  block_number bigint not null,
  block_hash text not null,
  observed_at timestamptz not null,
  source_timestamp timestamptz,
  is_finalized boolean not null default true,
  is_reverted boolean not null default false,
  created_at timestamptz not null default now(),
  constraint chk_rate_observations_block_number_nonneg check (block_number >= 0),
  constraint chk_rate_observations_finalized_provenance check (
    not is_finalized
    or (block_hash is not null and length(trim(block_hash)) > 0)
  ),
  constraint chk_rate_observations_reverted_state check (
    not is_reverted or is_finalized
  ),
  constraint chk_rate_observations_convention
    check (convention in ('APR', 'APY'))
);

create unique index uq_rate_observations_idempotent
  on rate_observations (
    protocol,
    product,
    side,
    chain_id,
    block_number,
    coalesce(contract_address, '')
  );

create index idx_rate_observations_product_observed
  on rate_observations (protocol, product, side, observed_at desc);

create index idx_rate_observations_chain_block
  on rate_observations (chain_id, block_number desc);

create index idx_rate_observations_finalized_lookup
  on rate_observations (protocol, product, side, block_number desc)
  where is_finalized = true and is_reverted = false;

comment on table rate_observations is
  'Immutable finalized protocol rate observations with methodology and block provenance.';

-- ---------------------------------------------------------------------------
-- Read model views (finalized, non-reverted observations only)
-- ---------------------------------------------------------------------------

create or replace view price_observations_latest as
select distinct on (base_asset, quote_asset, source)
  id,
  base_asset,
  quote_asset,
  value,
  source,
  chain_id,
  block_number,
  block_hash,
  round_id,
  observed_at,
  source_timestamp,
  is_stale,
  created_at
from price_observations
where is_finalized = true
  and is_reverted = false
order by base_asset, quote_asset, source, block_number desc, observed_at desc;

create or replace view price_observations_history as
select
  id,
  base_asset,
  quote_asset,
  value,
  source,
  chain_id,
  block_number,
  block_hash,
  round_id,
  observed_at,
  source_timestamp,
  is_stale,
  created_at
from price_observations
where is_finalized = true
  and is_reverted = false
order by base_asset, quote_asset, observed_at desc, block_number desc;

create or replace view rate_observations_latest as
select distinct on (protocol, product, side, coalesce(contract_address, ''))
  id,
  protocol,
  product,
  side,
  annualized_value,
  convention,
  methodology,
  lookback_window,
  contract_address,
  chain_id,
  block_number,
  block_hash,
  observed_at,
  source_timestamp,
  created_at
from rate_observations
where is_finalized = true
  and is_reverted = false
order by protocol, product, side, coalesce(contract_address, ''), block_number desc, observed_at desc;

create or replace view rate_observations_history as
select
  id,
  protocol,
  product,
  side,
  annualized_value,
  convention,
  methodology,
  lookback_window,
  contract_address,
  chain_id,
  block_number,
  block_hash,
  observed_at,
  source_timestamp,
  created_at
from rate_observations
where is_finalized = true
  and is_reverted = false
order by protocol, product, side, observed_at desc, block_number desc;

comment on view price_observations_latest is
  'Latest finalized, non-reverted price per asset pair and source.';

comment on view price_observations_history is
  'Historical finalized, non-reverted price observations for charting.';

comment on view rate_observations_latest is
  'Latest finalized, non-reverted annualized rate per protocol/product/side.';

comment on view rate_observations_history is
  'Historical finalized, non-reverted rate observations for charting.';