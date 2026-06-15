from __future__ import annotations

import json
import os
from pathlib import Path

import httpx
import pytest
import respx
from httpx import Response

from agents.services.feedback_backlog import feedback_backlog
from agents.services.llm_gateway import DailyCallTracker, LlmGateway

FIXTURES_DIR = Path(__file__).parent / "fixtures" / "charts"


@pytest.fixture(autouse=True)
def _reset_feedback_backlog():
    feedback_backlog.clear()
    yield
    feedback_backlog.clear()


@pytest.fixture(autouse=True)
def _activate_respx():
    with respx.mock:
        yield


@pytest.fixture(autouse=True)
def _test_env(monkeypatch):
    monkeypatch.setenv("JAVA_API_URL", "http://java.test")
    monkeypatch.setenv("INTERNAL_API_KEY", "test-internal-key")
    monkeypatch.setenv("LLM_API_KEY", "test-llm-key")
    monkeypatch.setenv("LLM_PROVIDER", "deepseek")
    monkeypatch.setenv("LLM_BASE_URL", "https://llm.test")
    monkeypatch.setenv("LLM_DAILY_FREE_CALL_CAP", "200")
    monkeypatch.setenv("CHART_REQUEST_ESCALATION_THRESHOLD", "3")


def load_fixture(name: str) -> dict:
    with open(FIXTURES_DIR / name, encoding="utf-8") as handle:
        return json.load(handle)


@pytest.fixture
def simulation_result() -> dict:
    return {
        "id": "00000000-0000-4000-8000-000000000001",
        "collateralValueUsd": 7600,
        "stablecoinDebtUsd": 4222.22,
        "liquidationPriceUsd": 3166.67,
        "annualStabilityFeeUsd": 211.11,
        "projectedGrossYieldUsd": 216.02,
        "projectedNetYieldUsd": 4.91,
        "healthRatio": 1.2,
        "riskTier": "HIGH",
        "warnings": [],
        "assumptions": {
            "protocol": "maker_sky",
            "ethAmount": 2,
            "ethPriceUsd": 3800,
            "ethPriceSource": "chainlink",
            "targetCollateralRatio": 1.8,
            "liquidationRatio": 1.5,
            "stabilityFeePct": 5.0,
            "deployYieldPct": 5.0,
            "years": 1,
            "compoundsPerYear": 12,
            "stabilityFeeModel": "linear_annualized_v1",
        },
    }


@pytest.fixture
def health_ratio_chart() -> dict:
    return load_fixture("health-ratio-sweep.json")


@pytest.fixture
def yield_projection_chart() -> dict:
    return load_fixture("simulation-yield-projection.json")


@pytest.fixture
def liquidation_band_chart() -> dict:
    return load_fixture("liquidation-price-band.json")


@pytest.fixture
def mock_llm_response() -> dict:
    return {
        "choices": [
            {
                "message": {
                    "content": json.dumps(
                        {
                            "summary": "Mocked LLM summary using simulation numbers only.",
                            "recommendations": ["Keep collateral buffer healthy."],
                            "risks": ["Health ratio is 1.2 in simulationResult."],
                            "assumptions": ["Model assumptions only."],
                            "toolsUsed": [],
                            "chartSpecs": [],
                            "feedback": [],
                        }
                    )
                }
            }
        ]
    }


@pytest.fixture
def mock_parse_goal_llm_response() -> dict:
    return {
        "choices": [
            {
                "message": {
                    "content": json.dumps(
                        {
                            "intent": "explain_result",
                            "entities": {},
                            "suggestedAction": "none",
                            "confidence": 0.8,
                            "toolsUsed": [],
                            "chartSpecs": [],
                            "feedback": [],
                        }
                    )
                }
            }
        ]
    }


@pytest.fixture
def httpx_client():
    return httpx.AsyncClient(transport=httpx.ASGITransport(app=_get_app()), base_url="http://test")


def _get_app():
    from app.main import app

    return app


@pytest.fixture
def capped_llm_gateway():
    tracker = DailyCallTracker(cap=0)
    client = httpx.AsyncClient()
    gateway = LlmGateway(
        provider="deepseek",
        base_url="https://llm.test",
        api_key="key",
        daily_cap=0,
        client=client,
        call_tracker=tracker,
    )
    return gateway