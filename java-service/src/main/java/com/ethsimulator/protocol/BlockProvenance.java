package com.ethsimulator.protocol;

import java.time.Instant;

public record BlockProvenance(
        long chainId,
        long blockNumber,
        String blockHash,
        Instant blockTimestamp
) {
}