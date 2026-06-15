"""Shared request validation helpers for agent payloads."""

from __future__ import annotations

import json
from typing import Any

from fastapi import HTTPException

MAX_JSON_BYTES = 256_000
MAX_MESSAGE_LENGTH = 4_000
MAX_SESSION_ID_LENGTH = 128
MAX_LIST_ITEMS = 200
MAX_DICT_KEYS = 50
MAX_NESTED_JSON_BYTES = 32_000


def _json_size(value: Any) -> int:
    return len(json.dumps(value, default=str).encode("utf-8"))


def ensure_json_size(value: Any, *, max_bytes: int, field_name: str) -> None:
    if _json_size(value) > max_bytes:
        raise HTTPException(status_code=413, detail=f"{field_name} exceeds size limit")


def ensure_bounded_dict(value: dict[str, Any], *, field_name: str) -> None:
    if len(value) > MAX_DICT_KEYS:
        raise HTTPException(status_code=413, detail=f"{field_name} has too many keys")
    ensure_json_size(value, max_bytes=MAX_NESTED_JSON_BYTES, field_name=field_name)


def ensure_bounded_list(value: list[Any], *, field_name: str, max_items: int = MAX_LIST_ITEMS) -> None:
    if len(value) > max_items:
        raise HTTPException(status_code=413, detail=f"{field_name} has too many items")
    ensure_json_size(value, max_bytes=MAX_NESTED_JSON_BYTES, field_name=field_name)