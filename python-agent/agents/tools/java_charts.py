"""HTTP clients for Java chart endpoints."""

from __future__ import annotations

import time
from typing import Any

import httpx

from agents.tools.java_market_data import ToolCallResult
from models.chart_spec import validate_chart_spec_passthrough


class JavaChartsClient:
    def __init__(self, base_url: str, client: httpx.AsyncClient | None = None):
        self.base_url = base_url.rstrip("/")
        self._client = client
        self._owns_client = client is None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=30.0)
        return self._client

    async def close(self) -> None:
        if self._owns_client and self._client is not None:
            await self._client.aclose()
            self._client = None

    async def get_simulation_projection_chart(
        self, params: dict[str, Any] | None = None
    ) -> ToolCallResult:
        return await self._fetch_chart(
            "get_simulation_projection_chart",
            "/api/charts/simulation-projection",
            params or {},
        )

    async def get_liquidation_band_chart(
        self, params: dict[str, Any] | None = None
    ) -> ToolCallResult:
        return await self._fetch_chart(
            "get_liquidation_band_chart",
            "/api/charts/liquidation-band",
            params or {},
        )

    async def get_health_ratio_chart(
        self, params: dict[str, Any] | None = None
    ) -> ToolCallResult:
        return await self._fetch_chart(
            "get_health_ratio_chart",
            "/api/charts/health-ratio",
            params or {},
        )

    async def _fetch_chart(
        self,
        tool: str,
        path: str,
        params: dict[str, Any],
    ) -> ToolCallResult:
        client = await self._get_client()
        query = {k: v for k, v in params.items() if v is not None}
        started = time.perf_counter()
        response = await client.get(f"{self.base_url}{path}", params=query)
        latency_ms = int((time.perf_counter() - started) * 1000)
        response.raise_for_status()
        payload = validate_chart_spec_passthrough(response.json())
        return ToolCallResult(tool=tool, payload=payload, latency_ms=latency_ms, success=True)


def chart_params_from_simulation(simulation_result: dict[str, Any] | None) -> dict[str, Any]:
    """Extract inline chart query params from a Java simulation response."""
    if not simulation_result:
        return {}
    params: dict[str, Any] = {}
    if simulation_id := simulation_result.get("id"):
        params["simulationId"] = simulation_id
    assumptions = simulation_result.get("assumptions") or {}
    for key in (
        "ethAmount",
        "protocol",
        "targetCollateralRatio",
        "liquidationRatio",
        "stabilityFeePct",
        "deployYieldPct",
        "years",
        "compoundsPerYear",
        "ethPriceUsd",
    ):
        if key in assumptions and assumptions[key] is not None:
            params[key] = assumptions[key]
    return params