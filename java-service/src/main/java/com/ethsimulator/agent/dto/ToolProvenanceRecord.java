package com.ethsimulator.agent.dto;

import java.time.Instant;

public record ToolProvenanceRecord(String toolName, String summary, Instant executedAt) {
}