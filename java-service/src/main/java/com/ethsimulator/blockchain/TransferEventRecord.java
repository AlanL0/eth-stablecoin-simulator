package com.ethsimulator.blockchain;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferEventRecord(
        String token,
        String txHash,
        int logIndex,
        String fromAddress,
        String toAddress,
        BigDecimal amount,
        long blockNumber,
        Instant occurredAt
) {
}