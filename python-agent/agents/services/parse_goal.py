"""Parse natural-language goals into structured intents."""

from __future__ import annotations

from typing import Any

from agents.services.chart_intent import classify, is_chart_request
from agents.services.feedback_backlog import feedback_backlog
from agents.services.llm_gateway import LlmGateway, load_prompt
from agents.tools.java_charts import JavaChartsClient, chart_params_from_simulation
from agents.tools.java_market_data import JavaMarketDataClient


async def parse_goal(
    *,
    message: str,
    session_id: str | None = None,
    simulation_result: dict[str, Any] | None = None,
    llm: LlmGateway | None = None,
    charts_client: JavaChartsClient | None = None,
    market_client: JavaMarketDataClient | None = None,
) -> dict[str, Any]:
    llm = llm or LlmGateway()
    charts_client = charts_client or JavaChartsClient(_java_url())
    market_client = market_client or JavaMarketDataClient(_java_url())

    tools_used: list[dict[str, Any]] = []
    chart_specs: list[dict[str, Any]] = []
    feedback: list[dict[str, Any]] = []

    chart_intent = classify(message)
    if chart_intent.kind == "unsupported" and chart_intent.normalized_label:
        record = feedback_backlog.upsert_chart_request(
            normalized_label=chart_intent.normalized_label,
            message=message,
            session_id=session_id,
            context={"source": "parse_goal"},
        )
        feedback.append(record.to_feedback_entry())
        return {
            "intent": "request_chart",
            "entities": {"chartType": chart_intent.normalized_label},
            "suggestedAction": "run_simulation",
            "confidence": 0.86,
            "toolsUsed": tools_used,
            "chartSpecs": chart_specs,
            "feedback": feedback,
        }

    if chart_intent.supported and chart_intent.tool_names:
        params = chart_params_from_simulation(simulation_result)
        for tool_name in chart_intent.tool_names:
            result = await _invoke_chart_tool(charts_client, market_client, tool_name, params)
            if result is None:
                continue
            tools_used.append(result.as_tools_used_entry(chart_id=result.payload.get("chartId")))
            chart_specs.append(result.payload)
        return {
            "intent": "request_chart",
            "entities": {"chartType": chart_intent.normalized_label or chart_intent.kind},
            "suggestedAction": "none",
            "confidence": 0.9,
            "toolsUsed": tools_used,
            "chartSpecs": chart_specs,
            "feedback": feedback,
        }

    def fallback_builder(**_: Any) -> dict[str, Any]:
        return _deterministic_parse(message)

    completion = await llm.complete_json(
        system_prompt=load_prompt("parse-goal-v1.txt"),
        user_payload={"message": message, "sessionId": session_id, "simulationResult": simulation_result},
        fallback_builder=fallback_builder,
    )
    payload = completion.content
    payload.setdefault("toolsUsed", tools_used)
    payload.setdefault("chartSpecs", chart_specs)
    payload.setdefault("feedback", feedback)
    if is_chart_request(message) and not chart_specs and not feedback:
        payload["intent"] = "request_chart"
    return payload


def _deterministic_parse(message: str) -> dict[str, Any]:
    lowered = message.lower()
    if any(token in lowered for token in ("wallet", "balance")):
        return {
            "intent": "unknown",
            "entities": {},
            "suggestedAction": "open_wallet",
            "confidence": 0.7,
            "toolsUsed": [],
            "chartSpecs": [],
            "feedback": [],
        }
    if any(token in lowered for token in ("audit", "transfer")):
        return {
            "intent": "audit_help",
            "entities": {},
            "suggestedAction": "open_audit",
            "confidence": 0.7,
            "toolsUsed": [],
            "chartSpecs": [],
            "feedback": [],
        }
    if any(token in lowered for token in ("borrow", "simulate", "how much")):
        return {
            "intent": "run_simulation",
            "entities": {},
            "suggestedAction": "run_simulation",
            "confidence": 0.75,
            "toolsUsed": [],
            "chartSpecs": [],
            "feedback": [],
        }
    return {
        "intent": "unknown",
        "entities": {},
        "suggestedAction": "none",
        "confidence": 0.5,
        "toolsUsed": [],
        "chartSpecs": [],
        "feedback": [],
    }


async def _invoke_chart_tool(
    charts_client: JavaChartsClient,
    market_client: JavaMarketDataClient,
    tool_name: str,
    params: dict[str, Any],
):
    if tool_name == "get_simulation_projection_chart":
        return await charts_client.get_simulation_projection_chart(params)
    if tool_name == "get_liquidation_band_chart":
        return await charts_client.get_liquidation_band_chart(params)
    if tool_name == "get_health_ratio_chart":
        return await charts_client.get_health_ratio_chart(params)
    if tool_name == "get_eth_price":
        return await market_client.get_eth_price()
    return None


def _java_url() -> str:
    import os

    return os.getenv("JAVA_API_URL", "http://localhost:8080")