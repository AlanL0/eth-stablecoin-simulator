"""HTTP clients for Java market data and simulation endpoints."""

from __future__ import annotations

import time
from typing import Any

import httpx


class ToolCallResult:
    def __init__(self, tool: str, payload: dict[str, Any], latency_ms: int, success: bool):
        self.tool = tool
        self.payload = payload
        self.latency_ms = latency_ms
        self.success = success

    def as_tools_used_entry(self, chart_id: str | None = None) -> dict[str, Any]:
        entry: dict[str, Any] = {
            "tool": self.tool,
            "latencyMs": self.latency_ms,
            "success": self.success,
        }
        if chart_id:
            entry["chartId"] = chart_id
        return entry


class JavaMarketDataClient:
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

    async def get_eth_price(self) -> ToolCallResult:
        return await self._get_json("get_eth_price", "/api/price/eth")

    async def get_stablecoin_yields(self, asset: str = "USDC") -> ToolCallResult:
        return await self._get_json(
            "get_stablecoin_yields",
            "/api/yields",
            params={"asset": asset},
        )

    async def run_simulation(self, payload: dict[str, Any]) -> ToolCallResult:
        return await self._post_json("run_simulation", "/api/simulations", payload)

    async def _get_json(
        self,
        tool: str,
        path: str,
        params: dict[str, Any] | None = None,
    ) -> ToolCallResult:
        client = await self._get_client()
        started = time.perf_counter()
        response = await client.get(f"{self.base_url}{path}", params=params)
        latency_ms = int((time.perf_counter() - started) * 1000)
        response.raise_for_status()
        return ToolCallResult(tool=tool, payload=response.json(), latency_ms=latency_ms, success=True)

    async def _post_json(self, tool: str, path: str, payload: dict[str, Any]) -> ToolCallResult:
        client = await self._get_client()
        started = time.perf_counter()
        response = await client.post(f"{self.base_url}{path}", json=payload)
        latency_ms = int((time.perf_counter() - started) * 1000)
        response.raise_for_status()
        return ToolCallResult(tool=tool, payload=response.json(), latency_ms=latency_ms, success=True)