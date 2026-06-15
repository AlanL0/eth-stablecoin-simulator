"""FastAPI application for the ETH stablecoin Python agent."""

from __future__ import annotations

import os
from typing import Any

from fastapi import FastAPI, Header, HTTPException, Request
from pydantic import BaseModel, Field, field_validator

from agents.services.feedback_backlog import feedback_backlog
from agents.services.parse_goal import parse_goal
from agents.services.rate_limit import public_agent_limiter
from agents.services.recommend_yield import recommend_yield
from agents.services.request_limits import (
    MAX_JSON_BYTES,
    MAX_MESSAGE_LENGTH,
    MAX_SESSION_ID_LENGTH,
    ensure_bounded_dict,
    ensure_bounded_list,
    ensure_json_size,
)
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
    riskPreference: str = Field(default="balanced", max_length=32)
    message: str = Field(default="", max_length=MAX_MESSAGE_LENGTH)
    sessionId: str | None = Field(default=None, max_length=MAX_SESSION_ID_LENGTH)

    @field_validator("simulationResult")
    @classmethod
    def validate_simulation_result(cls, value: dict[str, Any]) -> dict[str, Any]:
        ensure_bounded_dict(value, field_name="simulationResult")
        return value

    @field_validator("availableYields")
    @classmethod
    def validate_available_yields(cls, value: list[dict[str, Any]]) -> list[dict[str, Any]]:
        ensure_bounded_list(value, field_name="availableYields", max_items=50)
        return value


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
    message: str = Field(max_length=MAX_MESSAGE_LENGTH)
    sessionId: str | None = Field(default=None, max_length=MAX_SESSION_ID_LENGTH)
    simulationResult: dict[str, Any] | None = None

    @field_validator("simulationResult")
    @classmethod
    def validate_simulation_result(cls, value: dict[str, Any] | None) -> dict[str, Any] | None:
        if value is not None:
            ensure_bounded_dict(value, field_name="simulationResult")
        return value


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

    @field_validator("events")
    @classmethod
    def validate_events(cls, value: list[dict[str, Any]]) -> list[dict[str, Any]]:
        ensure_bounded_list(value, field_name="events", max_items=200)
        return value


class SummarizeAuditResponse(BaseModel):
    summary: str
    notablePatterns: list[str] = Field(default_factory=list)
    risks: list[str] = Field(default_factory=list)
    assumptions: list[str] = Field(default_factory=list)
    model: str = ""
    fallbackUsed: bool = False


class ChartRequestFeedbackRequest(BaseModel):
    normalizedLabel: str = Field(max_length=128)
    message: str = Field(max_length=MAX_MESSAGE_LENGTH)
    sessionId: str | None = Field(default=None, max_length=MAX_SESSION_ID_LENGTH)
    context: dict[str, Any] = Field(default_factory=dict)

    @field_validator("context")
    @classmethod
    def validate_context(cls, value: dict[str, Any]) -> dict[str, Any]:
        ensure_bounded_dict(value, field_name="context")
        return value


class ChartRequestFeedbackResponse(BaseModel):
    recorded: bool
    requestCount: int
    backlogEscalated: bool


def _verify_internal_api_key(x_internal_api_key: str | None) -> None:
    expected = os.getenv("INTERNAL_API_KEY", "")
    if not expected or x_internal_api_key != expected:
        raise HTTPException(status_code=401, detail="Invalid or missing internal API key")


def _client_key(request: Request) -> str:
    forwarded = request.headers.get("x-forwarded-for")
    if forwarded:
        return forwarded.split(",")[0].strip()
    if request.client:
        return request.client.host
    return "unknown"


def _enforce_public_agent_limits(request: Request) -> None:
    content_length = request.headers.get("content-length")
    if content_length is not None:
        try:
            if int(content_length) > MAX_JSON_BYTES:
                raise HTTPException(status_code=413, detail="Request body too large")
        except ValueError as exc:
            raise HTTPException(status_code=400, detail="Invalid Content-Length header") from exc
    if not public_agent_limiter.allow(_client_key(request)):
        raise HTTPException(status_code=429, detail="Rate limit exceeded")


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(status="ok", service="python-agent")


@app.post("/agent/recommend-yield", response_model=RecommendYieldResponse)
async def agent_recommend_yield(
    request: Request,
    body: RecommendYieldRequest,
) -> RecommendYieldResponse:
    _enforce_public_agent_limits(request)
    ensure_json_size(body.model_dump(), max_bytes=MAX_JSON_BYTES, field_name="request")
    result = await recommend_yield(
        simulation_result=body.simulationResult,
        available_yields=body.availableYields,
        risk_preference=body.riskPreference,
        message=body.message,
        session_id=body.sessionId,
    )
    return RecommendYieldResponse.model_validate(result)


@app.post("/agent/parse-goal", response_model=ParseGoalResponse)
async def agent_parse_goal(
    request: Request,
    body: ParseGoalRequest,
) -> ParseGoalResponse:
    _enforce_public_agent_limits(request)
    ensure_json_size(body.model_dump(), max_bytes=MAX_JSON_BYTES, field_name="request")
    result = await parse_goal(
        message=body.message,
        session_id=body.sessionId,
        simulation_result=body.simulationResult,
    )
    return ParseGoalResponse.model_validate(result)


@app.post("/agent/summarize-audit", response_model=SummarizeAuditResponse)
async def agent_summarize_audit(
    request: Request,
    body: SummarizeAuditRequest,
) -> SummarizeAuditResponse:
    _enforce_public_agent_limits(request)
    ensure_json_size(body.model_dump(), max_bytes=MAX_JSON_BYTES, field_name="request")
    result = await summarize_audit(events=body.events, hide_values=body.hideValues)
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