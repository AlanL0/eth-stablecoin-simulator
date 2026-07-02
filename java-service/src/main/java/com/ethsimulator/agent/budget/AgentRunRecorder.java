package com.ethsimulator.agent.budget;

import com.ethsimulator.agent.AgentSystemPrompt;
import com.ethsimulator.agent.dto.AgentAnalysisResponse;
import com.ethsimulator.persistence.AgentRunRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AgentRunRecorder {

    private static final Logger log = LoggerFactory.getLogger(AgentRunRecorder.class);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final ObjectProvider<AgentRunRepository> agentRunRepository;
    private final Timer requestTimer;

    public AgentRunRecorder(ObjectProvider<AgentRunRepository> agentRunRepository, MeterRegistry meterRegistry) {
        this.agentRunRepository = agentRunRepository;
        this.requestTimer = meterRegistry.timer("ethsim_agent_request_latency");
    }

    public void record(AgentRequestBudget budget, AgentAnalysisResponse response, long latencyMs, BigDecimal estimatedCostUsd) {
        requestTimer.record(latencyMs, TimeUnit.MILLISECONDS);
        if (response.fallbackReason() != null) {
            io.micrometer.core.instrument.Metrics.counter(
                    "ethsim_agent_fallback_total",
                    "reason",
                    response.fallbackReason()
            ).increment();
        }
        AgentRunRepository repository = agentRunRepository.getIfAvailable();
        if (repository == null) {
            return;
        }
        try {
            repository.insert(new AgentRunRepository.AgentRunRecord(
                    AgentSystemPrompt.VERSION,
                    "spring-ai",
                    response.model(),
                    (int) Math.min(Integer.MAX_VALUE, latencyMs),
                    response.fallbackUsed(),
                    response.fallbackReason(),
                    estimatedCostUsd,
                    tokenUsage(response),
                    sanitizedStructuredOutput(response)
            ));
        } catch (RuntimeException ex) {
            log.warn("Failed to persist agent run traceId={}: {}", response.traceId(), ex.getMessage());
        }
    }

    private static Map<String, Object> tokenUsage(AgentAnalysisResponse response) {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("traceId", response.traceId());
        usage.put("toolCount", response.toolProvenance() == null ? 0 : response.toolProvenance().size());
        return usage;
    }

    private static String sanitizedStructuredOutput(AgentAnalysisResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceId", response.traceId());
        payload.put("fallbackUsed", response.fallbackUsed());
        payload.put("fallbackReason", response.fallbackReason());
        payload.put("toolCount", response.toolProvenance() == null ? 0 : response.toolProvenance().size());
        payload.put("hasChart", response.chart() != null);
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }
}