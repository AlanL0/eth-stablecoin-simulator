package com.ethsimulator.persistence;

import java.time.Instant;

public record IngestionCursor(
        String sourceKey,
        long chainId,
        long nextBlock,
        Long lastFinalizedBlock,
        String lastFinalizedBlockHash,
        String leaseOwner,
        Instant leaseExpiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}