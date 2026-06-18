package com.ethsimulator.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceObservation(
        UUID id,
        String baseAsset,
        String quoteAsset,
        BigDecimal value,
        String source,
        long chainId,
        long blockNumber,
        String blockHash,
        String roundId,
        Instant observedAt,
        Instant sourceTimestamp,
        boolean stale,
        boolean finalized,
        boolean reverted,
        Instant createdAt
) {
    public static PriceObservation newObservation(
            String baseAsset,
            String quoteAsset,
            BigDecimal value,
            String source,
            long chainId,
            long blockNumber,
            String blockHash,
            String roundId,
            Instant observedAt,
            Instant sourceTimestamp,
            boolean stale,
            boolean finalized,
            boolean reverted
    ) {
        return new PriceObservation(
                null,
                baseAsset,
                quoteAsset,
                value,
                source,
                chainId,
                blockNumber,
                blockHash,
                roundId,
                observedAt,
                sourceTimestamp,
                stale,
                finalized,
                reverted,
                null
        );
    }
}