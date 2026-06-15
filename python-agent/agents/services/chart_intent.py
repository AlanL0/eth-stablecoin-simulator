"""Rule-based chart intent classifier before LLM tool selection."""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Literal

ChartIntentKind = Literal[
    "yield_over_time",
    "liquidation_risk",
    "eth_price_context",
    "unsupported",
    "none",
]


@dataclass(frozen=True)
class ChartIntent:
    kind: ChartIntentKind
    normalized_label: str | None = None
    tool_names: tuple[str, ...] = ()
    supported: bool = False

    @property
    def primary_tool(self) -> str | None:
        return self.tool_names[0] if self.tool_names else None


_UNSUPPORTED_PATTERNS: list[tuple[re.Pattern[str], str]] = [
    (re.compile(r"\b(bear|bull)\b.*\b(scenarios?|cases?|path)\b", re.I), "bear_bull_eth_scenarios"),
    (re.compile(r"\bscenario(s)?\b.*\b(eth|price)\b", re.I), "bear_bull_eth_scenarios"),
    (re.compile(r"\b(historical|last year|over time)\b.*\beth\b", re.I), "historical_eth_price"),
    (re.compile(r"\beth\b.*\b(last year|historical)\b", re.I), "historical_eth_price"),
    (re.compile(r"\b(compare|vs\.?|versus)\b.*\b(aave|maker|compound|yield)\b", re.I), "defi_spread_comparison"),
    (re.compile(r"\bdefi\b.*\bspread\b", re.I), "defi_spread_comparison"),
    (re.compile(r"\b(all|multiple)\b.*\b(wallet|address)\b", re.I), "portfolio_multi_address"),
    (re.compile(r"\bportfolio\b", re.I), "portfolio_multi_address"),
    (
        re.compile(r"\bliquidation\b.*\b(over time|timeline|path|stress)\b", re.I),
        "liquidation_timeline_advanced",
    ),
]

_SUPPORTED_PATTERNS: list[tuple[re.Pattern[str], ChartIntent]] = [
    (
        re.compile(r"\b(yield|fee|net|projection|return)\b.*\b(over time|chart|graph|plot)\b", re.I),
        ChartIntent("yield_over_time", "yield_over_time", ("get_simulation_projection_chart",), True),
    ),
    (
        re.compile(r"\b(graph|plot|chart)\b.*\b(yield|fee|projection)\b", re.I),
        ChartIntent("yield_over_time", "yield_over_time", ("get_simulation_projection_chart",), True),
    ),
    (
        re.compile(r"\b(liquidation|health ratio|health factor|buffer)\b", re.I),
        ChartIntent(
            "liquidation_risk",
            "liquidation_risk",
            ("get_liquidation_band_chart", "get_health_ratio_chart"),
            True,
        ),
    ),
    (
        re.compile(r"\b(current|spot)\b.*\beth\b.*\b(price)?\b", re.I),
        ChartIntent("eth_price_context", "eth_price_context", ("get_eth_price",), True),
    ),
    (
        re.compile(r"\beth\b.*\b(price|spot)\b", re.I),
        ChartIntent("eth_price_context", "eth_price_context", ("get_eth_price",), True),
    ),
]

_CHART_CUE = re.compile(r"\b(chart|graph|plot|visuali[sz]e|over time)\b", re.I)


def classify(message: str) -> ChartIntent:
    text = (message or "").strip()
    if not text:
        return ChartIntent("none")

    for pattern, label in _UNSUPPORTED_PATTERNS:
        if pattern.search(text):
            return ChartIntent("unsupported", label, (), False)

    for pattern, intent in _SUPPORTED_PATTERNS:
        if pattern.search(text):
            return intent

    if _CHART_CUE.search(text):
        return ChartIntent("unsupported", "unsupported_visual", (), False)

    return ChartIntent("none")


def is_chart_request(message: str) -> bool:
    intent = classify(message)
    return intent.kind in {"yield_over_time", "liquidation_risk", "eth_price_context", "unsupported"}