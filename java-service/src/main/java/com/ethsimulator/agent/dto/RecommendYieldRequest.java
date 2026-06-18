package com.ethsimulator.agent.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record RecommendYieldRequest(
        @NotEmpty Map<String, Object> simulationResult,
        List<Map<String, Object>> availableYields,
        @Size(max = 32) String riskPreference,
        @Size(max = 4000) String message,
        @Size(max = 128) String sessionId
) {
    public RecommendYieldRequest {
        if (availableYields == null) {
            availableYields = List.of();
        }
        if (riskPreference == null || riskPreference.isBlank()) {
            riskPreference = "balanced";
        }
        if (message == null) {
            message = "";
        }
    }
}