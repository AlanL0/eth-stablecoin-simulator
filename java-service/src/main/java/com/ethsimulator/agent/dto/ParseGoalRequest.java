package com.ethsimulator.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ParseGoalRequest(
        @NotBlank @Size(max = 4000) String message,
        @Size(max = 128) String sessionId,
        Map<String, Object> simulationResult
) {
}