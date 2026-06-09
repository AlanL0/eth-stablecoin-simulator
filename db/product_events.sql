-- Product instrumentation + chart waitlist (WP-1)
-- Apply after schema.sql

create table if not exists product_events (
  id uuid primary key default gen_random_uuid(),
  event_name text not null,
  session_id text,
  user_id uuid,
  properties jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_product_events_name_created
  on product_events (event_name, created_at desc);

create index if not exists idx_product_events_session
  on product_events (session_id, created_at desc)
  where session_id is not null;

comment on table product_events is
  'Lightweight product analytics. No PII in properties by default.';

create table if not exists chart_waitlist (
  id uuid primary key default gen_random_uuid(),
  email text,
  normalized_label text not null,
  session_id text,
  user_id uuid,
  created_at timestamptz not null default now()
);

create unique index if not exists uq_chart_waitlist_label_email
  on chart_waitlist (normalized_label, lower(email))
  where email is not null;

create index if not exists idx_chart_waitlist_label
  on chart_waitlist (normalized_label, created_at desc);

comment on table chart_waitlist is
  'Users who want a chart before membership ships. Email optional.';