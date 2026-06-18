package com.ethsimulator.agent.dto;

import java.util.List;
import java.util.Map;

public record SummarizeAuditRequest(
        List<Map<String, Object>> events,
        boolean hideValues
) {
    public SummarizeAuditRequest {
        if (events == null) {
            events = List.of();
        }
    }
}