import copy

import pytest
import respx
from httpx import ASGITransport, AsyncClient, Response

from app.main import app


@pytest.mark.asyncio
async def test_chart_specs_not_mutated(simulation_result, health_ratio_chart):
    original = copy.deepcopy(health_ratio_chart)
    respx.get("http://java.test/api/charts/health-ratio").mock(return_value=Response(200, json=health_ratio_chart))
    respx.get("http://java.test/api/charts/liquidation-band").mock(return_value=Response(200, json=health_ratio_chart))

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        response = await client.post(
            "/agent/parse-goal",
            json={
                "message": "Show liquidation buffer and health ratio",
                "simulationResult": simulation_result,
            },
        )

    chart = response.json()["chartSpecs"][0]
    assert chart == original
    assert chart["series"][0]["points"][2]["y"] == 1.2