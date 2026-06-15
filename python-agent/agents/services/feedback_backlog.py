"""In-memory chart-request feedback backlog."""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from datetime import UTC, datetime
from typing import Any


@dataclass
class FeedbackRecord:
    kind: str
    normalized_label: str
    message: str
    session_id: str | None = None
    context: dict[str, Any] = field(default_factory=dict)
    request_count: int = 1
    created_at: datetime = field(default_factory=lambda: datetime.now(UTC))
    updated_at: datetime = field(default_factory=lambda: datetime.now(UTC))

    @property
    def backlog_escalated(self) -> bool:
        threshold = int(os.getenv("CHART_REQUEST_ESCALATION_THRESHOLD", "3"))
        return self.request_count >= threshold

    def to_feedback_entry(self) -> dict[str, Any]:
        return {
            "kind": self.kind,
            "normalizedLabel": self.normalized_label,
            "requestCount": self.request_count,
            "backlogEscalated": self.backlog_escalated,
            "message": self.message,
        }


class FeedbackBacklog:
    """Process-local store keyed by (kind, normalized_label)."""

    def __init__(self) -> None:
        self._entries: dict[tuple[str, str], FeedbackRecord] = {}

    def clear(self) -> None:
        self._entries.clear()

    def upsert_chart_request(
        self,
        normalized_label: str,
        message: str,
        session_id: str | None = None,
        context: dict[str, Any] | None = None,
    ) -> FeedbackRecord:
        key = ("chart_request", normalized_label)
        now = datetime.now(UTC)
        if key in self._entries:
            record = self._entries[key]
            record.request_count += 1
            record.message = message
            record.session_id = session_id or record.session_id
            record.context = {**record.context, **(context or {})}
            record.updated_at = now
            if record.backlog_escalated:
                record.context["backlogEscalated"] = True
                if session_id:
                    record.context["sessionId"] = session_id
                record.context["lastMessage"] = message
        else:
            record = FeedbackRecord(
                kind="chart_request",
                normalized_label=normalized_label,
                message=message,
                session_id=session_id,
                context=context or {},
            )
            self._entries[key] = record
        return record

    def get(self, normalized_label: str) -> FeedbackRecord | None:
        return self._entries.get(("chart_request", normalized_label))


# Shared singleton for app + tests (reset in tests via clear()).
feedback_backlog = FeedbackBacklog()