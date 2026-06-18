package com.ethsimulator.agent.dto;

import java.util.List;
import java.util.Map;

public record RecommendYieldResponse(
        String summary,
        List<String> recommendations,
        List<String> risks,
        List<String> assumptions,
        List<Map<String, Object>> toolsUsed,
        List<Map<String, Object>> chartSpecs,
        List<Map<String, Object>> feedback,
        String model,
        boolean fallbackUsed
) {
}