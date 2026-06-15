"""Explain simulation results and recommend next steps."""

from __future__ import annotations

from typing import Any

from agents.services.chart_intent import classify
from agents.services.feedback_backlog import feedback_backlog
from agents.services.llm_gateway import LlmGateway, load_prompt
from agents.tools.java_charts import JavaChartsClient, chart_params_from_simulation
from agents.tools.java_market_data import JavaMarketDataClient


async def recommend_yield(
    *,
    simulation_result: dict[str, Any],
    available_yields: list[dict[str, Any]] | None = None,
    risk_preference: str = "balanced",
    message: str = "",
    session_id: str | None = None,
    llm: LlmGateway | None = None,
    charts_client: JavaChartsClient | None = None,
    market_client: JavaMarketDataClient | None = None,
) -> dict[str, Any]:
    llm = llm or LlmGateway()
    java_url = _java_url()
    charts_client = charts_client or JavaChartsClient(java_url)
    market_client = market_client or JavaMarketDataClient(java_url)

    tools_used: list[dict[str, Any]] = []
    chart_specs: list[dict[str, Any]] = []
    feedback: list[dict[str, Any]] = []

    if message:
        chart_intent = classify(message)
        if chart_intent.kind == "unsupported" and chart_intent.normalized_label:
            record = feedback_backlog.upsert_chart_request(
                normalized_label=chart_intent.normalized_label,
                message=message,
                session_id=session_id,
                context={"source": "recommend_yield"},
            )
            feedback.append(record.to_feedback_entry())
        elif chart_intent.supported and chart_intent.tool_names:
            params = chart_params_from_simulation(simulation_result)
            for tool_name in chart_intent.tool_names:
                result = await _invoke_tool(charts_client, market_client, tool_name, params)
                if result is None:
                    continue
                tools_used.append(result.as_tools_used_entry(chart_id=result.payload.get("chartId")))
                chart_specs.append(result.payload)

    def fallback_builder(**_: Any) -> dict[str, Any]:
        return _deterministic_recommendation(simulation_result, risk_preference, message)

    completion = await llm.complete_json(
        system_prompt=load_prompt("recommend-yield-v1.txt"),
        user_payload={
            "simulationResult": simulation_result,
            "availableYields": available_yields or [],
            "riskPreference": risk_preference,
            "message": message,
            "toolsUsed": tools_used,
            "chartSpecs": chart_specs,
        },
        fallback_builder=fallback_builder,
    )

    payload = completion.content
    payload["toolsUsed"] = tools_used or payload.get("toolsUsed", [])
    payload["chartSpecs"] = chart_specs or payload.get("chartSpecs", [])
    payload["feedback"] = feedback or payload.get("feedback", [])
    payload["model"] = completion.model
    payload["fallbackUsed"] = completion.fallback_used
    return payload


def _deterministic_recommendation(
    simulation_result: dict[str, Any],
    risk_preference: str,
    message: str,
) -> dict[str, Any]:
    debt = simulation_result.get("stablecoinDebtUsd")
    liquidation = simulation_result.get("liquidationPriceUsd")
    health = simulation_result.get("healthRatio")
    risk_tier = simulation_result.get("riskTier")
    net_yield = simulation_result.get("projectedNetYieldUsd")
    assumptions = simulation_result.get("assumptions") or {}
    eth_price = assumptions.get("ethPriceUsd")

    summary = (
        f"Simulation shows about ${debt} stablecoin debt with health ratio {health} ({risk_tier}). "
        f"Liquidation price is near ${liquidation} per ETH versus spot ${eth_price}. "
        f"Projected net yield after fees is about ${net_yield} over the modeled horizon."
    )
    if message:
        summary = f"{summary} Your question: {message}"

    recommendations = [
        "Review collateral buffer before increasing borrow size.",
        "Compare deploy yield assumptions against the stability fee model.",
    ]
    if risk_preference == "conservative":
        recommendations.append("Consider adding collateral to move away from the HIGH risk band.")

    risks = [
        f"Health ratio is {health} with liquidation near ${liquidation} per ETH.",
        f"Net yield after fees is only ${net_yield} in the deterministic model.",
    ]

    return {
        "summary": summary,
        "recommendations": recommendations,
        "risks": risks,
        "assumptions": [
            "Protocol presets are model assumptions, not live on-chain guarantees.",
            f"Stability fee model: {assumptions.get('stabilityFeeModel', 'linear_annualized_v1')}.",
        ],
        "toolsUsed": [],
        "chartSpecs": [],
        "feedback": [],
    }


async def _invoke_tool(
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