"""FastAPI application for the ETH stablecoin Python agent."""

from __future__ import annotations

import os
from typing import Any

from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

from agents.services.feedback_backlog import feedback_backlog
from agents.services.parse_goal import parse_goal
from agents.services.recommend_yield import recommend_yield
from agents.services.summarize_audit import summarize_audit

app = FastAPI(title="ETH Stablecoin Python Agent", version="0.1.0")


class HealthResponse(BaseModel):
    status: str
    service: str


class ToolUsedEntry(BaseModel):
    tool: str
    latencyMs: int
    success: bool
    chartId: str | None = None


class FeedbackEntry(BaseModel):
    kind: str
    normalizedLabel: str
    requestCount: int
    backlogEscalated: bool
    message: str


class RecommendYieldRequest(BaseModel):
    simulationResult: dict[str, Any]
    availableYields: list[dict[str, Any]] = Field(default_factory=list)
    riskPreference: str = "balanced"
    message: str = ""
    sessionId: str | None = None


class RecommendYieldResponse(BaseModel):
    summary: str
    recommendations: list[str]
    risks: list[str]
    assumptions: list[str]
    toolsUsed: list[ToolUsedEntry] = Field(default_factory=list)
    chartSpecs: list[dict[str, Any]] = Field(default_factory=list)
    feedback: list[FeedbackEntry] = Field(default_factory=list)
    model: str = ""
    fallbackUsed: bool = False


class ParseGoalRequest(BaseModel):
    message: str
    sessionId: str | None = None
    simulationResult: dict[str, Any] | None = None


class ParseGoalResponse(BaseModel):
    intent: str
    entities: dict[str, Any] = Field(default_factory=dict)
    suggestedAction: str
    confidence: float
    toolsUsed: list[ToolUsedEntry] = Field(default_factory=list)
    chartSpecs: list[dict[str, Any]] = Field(default_factory=list)
    feedback: list[FeedbackEntry] = Field(default_factory=list)


class SummarizeAuditRequest(BaseModel):
    events: list[dict[str, Any]] = Field(default_factory=list)
    hideValues: bool = False


class SummarizeAuditResponse(BaseModel):
    summary: str
    notablePatterns: list[str] = Field(default_factory=list)
    risks: list[str] = Field(default_factory=list)
    assumptions: list[str] = Field(default_factory=list)
    model: str = ""
    fallbackUsed: bool = False


class ChartRequestFeedbackRequest(BaseModel):
    normalizedLabel: str
    message: str
    sessionId: str | None = None
    context: dict[str, Any] = Field(default_factory=dict)


class ChartRequestFeedbackResponse(BaseModel):
    recorded: bool
    requestCount: int
    backlogEscalated: bool


def _verify_internal_api_key(x_internal_api_key: str | None) -> None:
    expected = os.getenv("INTERNAL_API_KEY", "")
    if not expected or x_internal_api_key != expected:
        raise HTTPException(status_code=401, detail="Invalid or missing internal API key")


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(status="ok", service="python-agent")


@app.post("/agent/recommend-yield", response_model=RecommendYieldResponse)
async def agent_recommend_yield(request: RecommendYieldRequest) -> RecommendYieldResponse:
    result = await recommend_yield(
        simulation_result=request.simulationResult,
        available_yields=request.availableYields,
        risk_preference=request.riskPreference,
        message=request.message,
        session_id=request.sessionId,
    )
    return RecommendYieldResponse.model_validate(result)


@app.post("/agent/parse-goal", response_model=ParseGoalResponse)
async def agent_parse_goal(request: ParseGoalRequest) -> ParseGoalResponse:
    result = await parse_goal(
        message=request.message,
        session_id=request.sessionId,
        simulation_result=request.simulationResult,
    )
    return ParseGoalResponse.model_validate(result)


@app.post("/agent/summarize-audit", response_model=SummarizeAuditResponse)
async def agent_summarize_audit(request: SummarizeAuditRequest) -> SummarizeAuditResponse:
    result = await summarize_audit(events=request.events, hide_values=request.hideValues)
    return SummarizeAuditResponse.model_validate(result)


@app.post("/internal/feedback/chart-request", response_model=ChartRequestFeedbackResponse)
async def internal_chart_request_feedback(
    request: ChartRequestFeedbackRequest,
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-Api-Key"),
) -> ChartRequestFeedbackResponse:
    _verify_internal_api_key(x_internal_api_key)
    record = feedback_backlog.upsert_chart_request(
        normalized_label=request.normalizedLabel,
        message=request.message,
        session_id=request.sessionId,
        context=request.context,
    )
    return ChartRequestFeedbackResponse(
        recorded=True,
        requestCount=record.request_count,
        backlogEscalated=record.backlog_escalated,
    )