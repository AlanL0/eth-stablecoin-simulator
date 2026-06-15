import os

from agents.services.feedback_backlog import FeedbackBacklog


def test_backlog_upsert_and_escalation(monkeypatch):
    monkeypatch.setenv("CHART_REQUEST_ESCALATION_THRESHOLD", "3")
    backlog = FeedbackBacklog()

    first = backlog.upsert_chart_request(
        normalized_label="bear_bull_eth_scenarios",
        message="first",
        session_id="sess-1",
    )
    assert first.request_count == 1
    assert first.backlog_escalated is False

    second = backlog.upsert_chart_request(
        normalized_label="bear_bull_eth_scenarios",
        message="second",
        session_id="sess-1",
    )
    assert second.request_count == 2
    assert second.backlog_escalated is False

    third = backlog.upsert_chart_request(
        normalized_label="bear_bull_eth_scenarios",
        message="third",
        session_id="sess-1",
    )
    assert third.request_count == 3
    assert third.backlog_escalated is True
    assert third.context.get("backlogEscalated") is True