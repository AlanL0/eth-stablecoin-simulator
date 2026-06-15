package com.ethsimulator.audit;

import java.util.List;

public record AuditResponse(
        String address,
        List<AuditEvent> events,
        boolean hideValues,
        List<String> assumptions
) {
}