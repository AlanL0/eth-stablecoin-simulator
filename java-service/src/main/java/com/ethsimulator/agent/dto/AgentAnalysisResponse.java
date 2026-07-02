package com.ethsimulator.agent.dto;

import com.ethsimulator.charts.ChartContract;

import java.util.List;

public record AgentAnalysisResponse(
        String narrative,
        ChartContract chart,
        List<String> warnings,
        List<ToolProvenanceRecord> toolProvenance,
        boolean fallbackUsed,
        String fallbackReason,
        String traceId,
        String model
) {
}