import json

import pytest
import respx
from httpx import ASGITransport, AsyncClient, Response

from app.main import app


def _client():
    return AsyncClient(transport=ASGITransport(app=app), base_url="http://test")


@pytest.mark.asyncio
async def test_recommend_yield_schema_and_mocked_dependencies(
    simulation_result,
    health_ratio_chart,
    liquidation_band_chart,
    mock_llm_response,
):
    message = "How risky is this if ETH drops? Show health ratio chart"
    respx.post("https://llm.test/chat/completions").mock(return_value=Response(200, json=mock_llm_response))
    respx.get("http://java.test/api/charts/liquidation-band").mock(
        return_value=Response(200, json=liquidation_band_chart)
    )
    respx.get("http://java.test/api/charts/health-ratio").mock(return_value=Response(200, json=health_ratio_chart))

    async with _client() as client:
        response = await client.post(
            "/agent/recommend-yield",
            json={
                "simulationResult": simulation_result,
                "availableYields": [{"protocol": "aave", "apyPct": 4.2}],
                "riskPreference": "conservative",
                "message": message,
            },
        )

    assert response.status_code == 200
    payload = response.json()
    assert "summary" in payload
    assert isinstance(payload["recommendations"], list)
    assert isinstance(payload["risks"], list)
    assert any(entry["tool"] == "get_health_ratio_chart" for entry in payload["toolsUsed"])
    chart_ids = [spec["chartId"] for spec in payload["chartSpecs"]]
    assert "health_ratio_sweep" in chart_ids
    assert health_ratio_chart in payload["chartSpecs"]


@pytest.mark.asyncio
async def test_parse_goal_unsupported_chart_records_feedback(simulation_result):
    async with _client() as client:
        response = await client.post(
            "/agent/parse-goal",
            json={
                "message": "Show me a graph of bear and bull cases for my vault",
                "sessionId": "sess-1",
                "simulationResult": simulation_result,
            },
        )

    assert response.status_code == 200
    payload = response.json()
    assert payload["intent"] == "request_chart"
    assert payload["feedback"][0]["normalizedLabel"] == "bear_bull_eth_scenarios"
    assert payload["feedback"][0]["requestCount"] == 1
    assert payload["chartSpecs"] == []


@pytest.mark.asyncio
async def test_parse_goal_supported_chart_fetches_java_tool(simulation_result, yield_projection_chart):
    respx.get("http://java.test/api/charts/simulation-projection").mock(
        return_value=Response(200, json=yield_projection_chart)
    )
    async with _client() as client:
        response = await client.post(
            "/agent/parse-goal",
            json={
                "message": "Plot yield over time",
                "simulationResult": simulation_result,
            },
        )

    payload = response.json()
    assert payload["intent"] == "request_chart"
    assert payload["toolsUsed"][0]["tool"] == "get_simulation_projection_chart"
    assert payload["chartSpecs"][0] == yield_projection_chart


@pytest.mark.asyncio
async def test_summarize_audit_schema(mock_llm_response):
    audit_llm = {
        "choices": [
            {
                "message": {
                    "content": json.dumps(
                        {
                            "summary": "Two transfers found.",
                            "notablePatterns": ["Both on same day."],
                            "risks": ["Operational privacy note only."],
                            "assumptions": ["Lite audit."],
                            "model": "",
                            "fallbackUsed": False,
                        }
                    )
                }
            }
        ]
    }
    respx.post("https://llm.test/chat/completions").mock(return_value=Response(200, json=audit_llm))
    async with _client() as client:
        response = await client.post(
            "/agent/summarize-audit",
            json={
                "events": [{"date": "2026-01-01", "direction": "in", "amountUsd": 100}],
                "hideValues": False,
            },
        )
    assert response.status_code == 200
    payload = response.json()
    assert payload["summary"]
    assert isinstance(payload["notablePatterns"], list)


@pytest.mark.asyncio
async def test_internal_feedback_requires_api_key():
    async with _client() as client:
        denied = await client.post(
            "/internal/feedback/chart-request",
            json={"normalizedLabel": "bear_bull_eth_scenarios", "message": "need chart"},
        )
        assert denied.status_code == 401

        allowed = await client.post(
            "/internal/feedback/chart-request",
            json={"normalizedLabel": "bear_bull_eth_scenarios", "message": "need chart"},
            headers={"X-Internal-Api-Key": "test-internal-key"},
        )
        assert allowed.status_code == 200
        body = allowed.json()
        assert body["recorded"] is True
        assert body["requestCount"] == 1


@pytest.mark.asyncio
async def test_internal_feedback_escalates_at_threshold(monkeypatch):
    monkeypatch.setenv("CHART_REQUEST_ESCALATION_THRESHOLD", "3")
    async with _client() as client:
        headers = {"X-Internal-Api-Key": "test-internal-key"}
        body = {"normalizedLabel": "historical_eth_price", "message": "plot eth history"}
        for expected_count in (1, 2, 3):
            response = await client.post(
                "/internal/feedback/chart-request",
                json=body,
                headers=headers,
            )
            payload = response.json()
            assert payload["requestCount"] == expected_count
        assert payload["backlogEscalated"] is True