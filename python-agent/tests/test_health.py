import pytest


@pytest.mark.asyncio
async def test_health(httpx_client):
    response = await httpx_client.get("/health")
    assert response.status_code == 200
    payload = response.json()
    assert payload == {"status": "ok", "service": "python-agent"}