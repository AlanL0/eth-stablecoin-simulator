package com.ethsimulator.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RateObservation(
        UUID id,
        String protocol,
        String product,
        String side,
        BigDecimal annualizedValue,
        String convention,
        String methodology,
        String lookbackWindow,
        String contractAddress,
        long chainId,
        long blockNumber,
        String blockHash,
        Instant observedAt,
        Instant sourceTimestamp,
        boolean finalized,
        boolean reverted,
        Instant createdAt
) {
    public static RateObservation newObservation(
            String protocol,
            String product,
            String side,
            BigDecimal annualizedValue,
            String convention,
            String methodology,
            String lookbackWindow,
            String contractAddress,
            long chainId,
            long blockNumber,
            String blockHash,
            Instant observedAt,
            Instant sourceTimestamp,
            boolean finalized,
            boolean reverted
    ) {
        return new RateObservation(
                null,
                protocol,
                product,
                side,
                annualizedValue,
                convention,
                methodology,
                lookbackWindow,
                contractAddress,
                chainId,
                blockNumber,
                blockHash,
                observedAt,
                sourceTimestamp,
                finalized,
                reverted,
                null
        );
    }
}