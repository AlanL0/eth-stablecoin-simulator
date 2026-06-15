package com.ethsimulator.audit;

public record AuditEvent(
        String token,
        String txHash,
        int logIndex,
        String fromAddress,
        String toAddress,
        String amount,
        long blockNumber,
        String occurredAt
) {
}