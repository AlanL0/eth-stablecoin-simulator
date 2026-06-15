"""Lightweight in-process rate limiting for public agent routes."""

from __future__ import annotations

import time
from collections import defaultdict, deque
from dataclasses import dataclass, field


@dataclass
class SlidingWindowLimiter:
    max_requests: int
    window_seconds: int
    _hits: dict[str, deque[float]] = field(default_factory=lambda: defaultdict(deque))

    def allow(self, key: str) -> bool:
        now = time.monotonic()
        window_start = now - self.window_seconds
        bucket = self._hits[key]
        while bucket and bucket[0] <= window_start:
            bucket.popleft()
        if len(bucket) >= self.max_requests:
            return False
        bucket.append(now)
        return True

    def reset(self) -> None:
        self._hits.clear()


public_agent_limiter = SlidingWindowLimiter(max_requests=60, window_seconds=3600)