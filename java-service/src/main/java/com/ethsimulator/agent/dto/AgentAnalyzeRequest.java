package com.ethsimulator.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentAnalyzeRequest(
        @NotBlank @Size(max = 4_000) String message,
        @Size(max = 128) String correlationId
) {
}