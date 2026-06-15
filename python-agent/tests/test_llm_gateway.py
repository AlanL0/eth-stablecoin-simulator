import json

import httpx
import pytest
import respx
from httpx import Response

from agents.services.llm_gateway import DailyCallTracker, LlmGateway


@pytest.mark.asyncio
@respx.mock
async def test_provider_configs_share_gateway_interface():
    for provider, base_url in (
        ("deepseek", "https://api.deepseek.com"),
        ("nvidia_nim", "https://integrate.api.nvidia.com/v1"),
        ("local", "http://localhost:11434/v1"),
        ("openrouter", "https://openrouter.ai/api/v1"),
    ):
        respx.post(f"{base_url}/chat/completions").mock(
            return_value=Response(
                200,
                json={
                    "choices": [
                        {"message": {"content": json.dumps({"ok": True})}}
                    ]
                },
            )
        )
        gateway = LlmGateway(provider=provider, base_url=base_url, api_key="k")
        result = await gateway.complete_json(
            system_prompt="sys",
            user_payload={"message": "hi"},
            fallback_builder=lambda **_: {"ok": False},
        )
        assert result.fallback_used is False
        assert result.provider == provider
        await gateway.close()


@pytest.mark.asyncio
async def test_daily_cap_triggers_deterministic_fallback(capped_llm_gateway):
    result = await capped_llm_gateway.complete_json(
        system_prompt="sys",
        user_payload={"message": "hi"},
        fallback_builder=lambda **_: {"summary": "fallback", "fallbackUsed": True},
    )
    assert result.fallback_used is True
    assert result.model == "deterministic-fallback"
    assert result.content["summary"] == "fallback"


def test_daily_call_tracker_records_calls():
    tracker = DailyCallTracker(cap=2)
    assert tracker.can_call() is True
    tracker.record_call()
    tracker.record_call()
    assert tracker.can_call() is False