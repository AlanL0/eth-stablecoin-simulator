import httpx
import pytest
import respx
from httpx import Response

from agents.tools.java_charts import JavaChartsClient
from agents.tools.java_market_data import JavaMarketDataClient


@pytest.mark.asyncio
@respx.mock
async def test_java_chart_client_passes_through_fixture(health_ratio_chart):
    respx.get("http://java.test/api/charts/health-ratio").mock(
        return_value=Response(200, json=health_ratio_chart)
    )
    client = httpx.AsyncClient()
    charts = JavaChartsClient("http://java.test", client=client)
    result = await charts.get_health_ratio_chart({"ethAmount": 2, "protocol": "maker_sky"})
    assert result.payload == health_ratio_chart
    assert result.tool == "get_health_ratio_chart"
    assert result.success is True
    await charts.close()


@pytest.mark.asyncio
@respx.mock
async def test_java_market_data_clients_pass_through():
    eth_payload = {
        "priceUsd": 3800.0,
        "source": "chainlink",
        "observedAt": "2026-06-09T12:00:00Z",
        "stale": False,
    }
    yield_payload = {
        "asset": "USDC",
        "yields": [{"protocol": "aave", "apyPct": 4.2, "source": "model", "riskTier": "LOW"}],
    }
    respx.get("http://java.test/api/price/eth").mock(return_value=Response(200, json=eth_payload))
    respx.get("http://java.test/api/yields").mock(return_value=Response(200, json=yield_payload))

    client = httpx.AsyncClient()
    market = JavaMarketDataClient("http://java.test", client=client)
    eth = await market.get_eth_price()
    yields = await market.get_stablecoin_yields("USDC")
    assert eth.payload == eth_payload
    assert yields.payload == yield_payload
    await market.close()