package com.ethsimulator.persistence;

import java.time.Instant;

public record SourceHealth(
        String sourceKey,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        int consecutiveFailures,
        Long lagBlocks,
        String status,
        String reason,
        Instant updatedAt
) {
}