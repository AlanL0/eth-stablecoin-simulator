package com.ethsimulator.agent.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ParseGoalResponse(
        String intent,
        Map<String, Object> entities,
        String suggestedAction,
        BigDecimal confidence,
        List<Map<String, Object>> toolsUsed,
        List<Map<String, Object>> chartSpecs,
        List<Map<String, Object>> feedback
) {
}