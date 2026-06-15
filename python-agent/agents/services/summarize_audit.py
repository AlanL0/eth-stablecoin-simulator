"""Summarize audit transfer events for hobby users."""

from __future__ import annotations

from typing import Any

from agents.services.llm_gateway import LlmGateway, load_prompt


async def summarize_audit(
    *,
    events: list[dict[str, Any]],
    hide_values: bool = False,
    llm: LlmGateway | None = None,
) -> dict[str, Any]:
    llm = llm or LlmGateway()

    def fallback_builder(**_: Any) -> dict[str, Any]:
        return _deterministic_audit_summary(events, hide_values)

    completion = await llm.complete_json(
        system_prompt=load_prompt("summarize-audit-v1.txt"),
        user_payload={"events": events, "hideValues": hide_values},
        fallback_builder=fallback_builder,
    )
    payload = completion.content
    payload["model"] = completion.model
    payload["fallbackUsed"] = completion.fallback_used
    payload.setdefault("assumptions", [
        "Audit is lite, allowlisted tokens only, and may be incomplete.",
    ])
    return payload


def _deterministic_audit_summary(events: list[dict[str, Any]], hide_values: bool) -> dict[str, Any]:
    if not events:
        return {
            "summary": (
                "No transfer events were found for this address and date range. "
                "Verify the wallet address and widen the date filter."
            ),
            "notablePatterns": [],
            "risks": ["An empty audit may mean the address had no allowlisted stablecoin activity."],
            "assumptions": ["Audit is lite, allowlisted tokens only, and may be incomplete."],
            "model": "deterministic-fallback",
            "fallbackUsed": True,
        }

    count = len(events)
    if hide_values:
        summary = f"Found {count} hidden transfers in the selected period."
    else:
        summary = f"Found {count} allowlisted stablecoin transfers in the selected period."

    return {
        "summary": summary,
        "notablePatterns": [f"{count} transfer event(s) in the supplied audit payload."],
        "risks": ["Transfer visibility depends on allowlisted tokens and cached fetch windows."],
        "assumptions": ["Audit is lite, allowlisted tokens only, and may be incomplete."],
        "model": "deterministic-fallback",
        "fallbackUsed": True,
    }