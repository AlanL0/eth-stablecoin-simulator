"""Provider-neutral LLM gateway with daily call cap and deterministic fallback."""

from __future__ import annotations

import json
import os
from dataclasses import dataclass
from datetime import UTC, date, datetime
from collections.abc import Callable
from typing import Any, Literal

import httpx

ProviderName = Literal["deepseek", "nvidia_nim", "local", "openrouter"]


@dataclass
class LlmCompletion:
    content: dict[str, Any]
    model: str
    provider: ProviderName
    fallback_used: bool = False
    latency_ms: int = 0


class DailyCallTracker:
    def __init__(self, cap: int) -> None:
        self.cap = cap
        self._day = date.today()
        self._count = 0

    def reset(self) -> None:
        self._day = date.today()
        self._count = 0

    def can_call(self) -> bool:
        self._roll_day()
        return self._count < self.cap

    def record_call(self) -> None:
        self._roll_day()
        self._count += 1

    @property
    def count(self) -> int:
        self._roll_day()
        return self._count

    def _roll_day(self) -> None:
        today = date.today()
        if today != self._day:
            self._day = today
            self._count = 0


class LlmGateway:
    PROVIDER_PATHS = {
        "deepseek": "/chat/completions",
        "nvidia_nim": "/chat/completions",
        "local": "/chat/completions",
        "openrouter": "/chat/completions",
    }

    def __init__(
        self,
        provider: str | None = None,
        base_url: str | None = None,
        api_key: str | None = None,
        model_primary: str | None = None,
        model_fallback: str | None = None,
        daily_cap: int | None = None,
        client: httpx.AsyncClient | None = None,
        call_tracker: DailyCallTracker | None = None,
    ) -> None:
        self.provider: ProviderName = (provider or os.getenv("LLM_PROVIDER", "deepseek"))  # type: ignore[assignment]
        self.base_url = (base_url or os.getenv("LLM_BASE_URL", "https://api.deepseek.com")).rstrip("/")
        self.api_key = api_key if api_key is not None else os.getenv("LLM_API_KEY", "")
        self.model_primary = model_primary or os.getenv("MODEL_PRIMARY") or self._default_model(self.provider)
        self.model_fallback = model_fallback or os.getenv("MODEL_FALLBACK") or self.model_primary
        cap = daily_cap if daily_cap is not None else int(os.getenv("LLM_DAILY_FREE_CALL_CAP", "200"))
        self.call_tracker = call_tracker or DailyCallTracker(cap)
        self._client = client
        self._owns_client = client is None

    @staticmethod
    def _default_model(provider: ProviderName) -> str:
        defaults = {
            "deepseek": "deepseek-chat",
            "nvidia_nim": "meta/llama-3.1-8b-instruct",
            "local": "llama3",
            "openrouter": "openrouter/auto",
        }
        return defaults[provider]

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=60.0)
        return self._client

    async def close(self) -> None:
        if self._owns_client and self._client is not None:
            await self._client.aclose()
            self._client = None

    async def complete_json(
        self,
        *,
        system_prompt: str,
        user_payload: dict[str, Any],
        fallback_builder: Callable[..., dict[str, Any]],
    ) -> LlmCompletion:
        if not self.call_tracker.can_call():
            return LlmCompletion(
                content=fallback_builder(reason="daily_cap_exceeded"),
                model="deterministic-fallback",
                provider=self.provider,
                fallback_used=True,
            )

        started = datetime.now(UTC)
        try:
            raw = await self._invoke_chat(system_prompt, user_payload)
            content = _extract_json_object(raw)
            latency_ms = int((datetime.now(UTC) - started).total_seconds() * 1000)
            self.call_tracker.record_call()
            return LlmCompletion(
                content=content,
                model=self.model_primary,
                provider=self.provider,
                fallback_used=False,
                latency_ms=latency_ms,
            )
        except Exception:
            return LlmCompletion(
                content=fallback_builder(reason="provider_error"),
                model="deterministic-fallback",
                provider=self.provider,
                fallback_used=True,
            )

    async def _invoke_chat(self, system_prompt: str, user_payload: dict[str, Any]) -> str:
        client = await self._get_client()
        path = self.PROVIDER_PATHS[self.provider]
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        body = {
            "model": self.model_primary,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": json.dumps(user_payload)},
            ],
            "response_format": {"type": "json_object"},
        }
        response = await client.post(f"{self.base_url}{path}", headers=headers, json=body)
        response.raise_for_status()
        data = response.json()
        return data["choices"][0]["message"]["content"]


def _extract_json_object(text: str) -> dict[str, Any]:
    text = text.strip()
    if text.startswith("```"):
        text = text.strip("`")
        if text.startswith("json"):
            text = text[4:].strip()
    parsed = json.loads(text)
    if not isinstance(parsed, dict):
        raise ValueError("LLM response was not a JSON object")
    return parsed


def load_prompt(filename: str) -> str:
    base = os.path.join(os.path.dirname(__file__), "..", "..", "prompts")
    path = os.path.join(base, filename)
    with open(path, encoding="utf-8") as handle:
        return handle.read()