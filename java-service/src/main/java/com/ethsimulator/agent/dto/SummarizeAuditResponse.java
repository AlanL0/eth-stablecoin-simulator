package com.ethsimulator.agent.dto;

import java.util.List;

public record SummarizeAuditResponse(
        String summary,
        List<String> notablePatterns,
        List<String> risks,
        List<String> assumptions,
        String model,
        boolean fallbackUsed
) {
}