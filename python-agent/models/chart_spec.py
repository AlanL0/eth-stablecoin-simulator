"""Pydantic models aligned with docs/chart-spec-schema.md and ChartSpecV1 OpenAPI."""

from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, Field


class ChartAxis(BaseModel):
    type: Literal["linear", "time", "category"]
    label: str
    unit: str | None = None
    format: Literal["number", "usd", "percent", "eth", "month_index"] | None = None
    domain: list[float] | None = Field(default=None, min_length=2, max_length=2)
    tickCount: int | None = Field(default=None, ge=2)


class ChartPoint(BaseModel):
    x: Any
    y: float | None = None
    y0: float | None = None
    y1: float | None = None
    label: str | None = None


class ChartSeriesStyle(BaseModel):
    colorToken: Literal[
        "primary", "secondary", "positive", "negative", "warning", "neutral"
    ] | None = None
    strokeDash: Literal["solid", "dashed", "dotted"] | None = None
    fillOpacity: float | None = Field(default=None, ge=0, le=1)


class ChartSeries(BaseModel):
    id: str
    name: str
    geometry: Literal["line", "area", "band", "bar"]
    points: list[ChartPoint] = Field(min_length=1)
    style: ChartSeriesStyle | None = None


class ChartAnnotation(BaseModel):
    id: str
    kind: Literal["horizontal_line", "vertical_line", "band", "point", "label"]
    axis: Literal["x", "y"] | None = None
    value: Any | None = None
    valueEnd: Any | None = None
    label: str | None = None
    severity: Literal["info", "low", "medium", "high"] | None = None


class ChartLegend(BaseModel):
    position: Literal["top", "bottom", "right", "hidden"] | None = None
    show: bool | None = None


class ChartSource(BaseModel):
    field: str
    source: str
    observedAt: datetime
    stale: bool | None = None


class ChartMeta(BaseModel):
    simulationId: str | None = None
    protocol: str | None = None
    ethPriceUsd: float | None = None
    ethAmount: float | None = None
    stablecoinDebtUsd: float | None = None
    sources: list[ChartSource] | None = None


class ChartSpecV1(BaseModel):
    schemaVersion: Literal["1.0"]
    chartId: str
    chartType: Literal["line", "area", "band", "bar", "composed"]
    title: str
    subtitle: str | None = None
    xAxis: ChartAxis
    yAxis: ChartAxis
    series: list[ChartSeries] = Field(min_length=1)
    annotations: list[ChartAnnotation] | None = None
    legend: ChartLegend | None = None
    meta: ChartMeta | None = None
    source: str
    generatedAt: datetime


def validate_chart_spec_passthrough(data: dict[str, Any]) -> dict[str, Any]:
    """Validate structure without mutating numeric values."""
    ChartSpecV1.model_validate(data)
    return data