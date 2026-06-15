"""Tests for public agent abuse controls."""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from agents.services.rate_limit import public_agent_limiter
from app.main import app


@pytest.fixture
def client() -> TestClient:
    public_agent_limiter.reset()
    return TestClient(app)


def test_rejects_oversized_content_length(client: TestClient) -> None:
    response = client.post(
        "/agent/parse-goal",
        headers={"Content-Length": "500000"},
        json={"message": "hello"},
    )
    assert response.status_code == 413


def test_rejects_overlong_message(client: TestClient) -> None:
    response = client.post(
        "/agent/parse-goal",
        json={"message": "x" * 5000},
    )
    assert response.status_code == 422


def test_rate_limits_public_agent_routes(client: TestClient) -> None:
    public_agent_limiter.max_requests = 2
    body = {"message": "How risky is this?"}
    assert client.post("/agent/parse-goal", json=body).status_code == 200
    assert client.post("/agent/parse-goal", json=body).status_code == 200
    assert client.post("/agent/parse-goal", json=body).status_code == 429