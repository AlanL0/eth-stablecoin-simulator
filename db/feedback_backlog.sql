-- Chart/visual demand backlog views (WP-1)
-- Apply after schema.sql

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