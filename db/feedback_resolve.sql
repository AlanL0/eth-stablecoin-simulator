-- Feedback backlog resolution helper (WP-1)
-- Apply after schema.sql

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