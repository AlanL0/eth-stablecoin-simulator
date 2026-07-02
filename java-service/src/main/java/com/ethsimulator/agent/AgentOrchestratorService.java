package com.ethsimulator.agent;

import com.ethsimulator.agent.dto.AgentAnalysisResponse;
import com.ethsimulator.agent.dto.AgentAnalyzeRequest;
import com.ethsimulator.agent.dto.ToolProvenanceRecord;
import com.ethsimulator.agent.tools.FixedIncomeAnalyticsTools;
import com.ethsimulator.charts.ChartContract;
import com.ethsimulator.config.AgentAiProperties;
import com.ethsimulator.config.UnavailableChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgentOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestratorService.class);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final ChatModel chatModel;
    private final FixedIncomeAnalyticsTools analyticsTools;
    private final AgentDeterministicFallbackService fallbackService;
    private final AgentAiProperties agentAiProperties;
    private final Clock clock;

    public AgentOrchestratorService(
            ChatModel chatModel,
            FixedIncomeAnalyticsTools analyticsTools,
            AgentDeterministicFallbackService fallbackService,
            AgentAiProperties agentAiProperties,
            Clock clock
    ) {
        this.chatModel = chatModel;
        this.analyticsTools = analyticsTools;
        this.fallbackService = fallbackService;
        this.agentAiProperties = agentAiProperties;
        this.clock = clock;
    }

    public AgentAnalysisResponse analyze(AgentAnalyzeRequest request) {
        String traceId = traceId(request.correlationId());
        if (!agentAiProperties.isEnabled() || chatModel instanceof UnavailableChatModel) {
            return fallbackService.analyze(request.message(), request.correlationId(), "disabled");
        }

        try {
            return runModelLoop(request.message(), traceId);
        } catch (RuntimeException ex) {
            log.warn("Agent orchestration failed traceId={}: {}", traceId, ex.getMessage());
            return fallbackService.analyze(request.message(), request.correlationId(), "provider");
        }
    }

    private AgentAnalysisResponse runModelLoop(String message, String traceId) {
        ToolCallback[] toolCallbacks = ToolCallbacks.from(analyticsTools);
        Map<String, ToolCallback> callbacksByName = Arrays.stream(toolCallbacks)
                .collect(Collectors.toMap(
                        callback -> callback.getToolDefinition().name(),
                        callback -> callback,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<Message> conversation = new ArrayList<>();
        conversation.add(new SystemMessage(AgentSystemPrompt.TEXT));
        conversation.add(new UserMessage(message));

        List<ToolProvenanceRecord> provenance = new ArrayList<>();
        ChartContract chart = null;
        String narrative = null;

        int maxTurns = Math.max(1, agentAiProperties.getMaxTurns());
        for (int turn = 0; turn < maxTurns; turn++) {
            ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                    .toolCallbacks(List.of(toolCallbacks))
                    .build();
            ChatResponse response = chatModel.call(new Prompt(conversation, options));
            Generation generation = response.getResult();
            AssistantMessage assistantMessage = generation.getOutput();
            conversation.add(assistantMessage);

            if (!assistantMessage.hasToolCalls()) {
                narrative = assistantMessage.getText();
                break;
            }

            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                ToolCallback callback = callbacksByName.get(toolCall.name());
                if (callback == null) {
                    throw new IllegalStateException("Unknown tool requested: " + toolCall.name());
                }
                String result = callback.call(toolCall.arguments());
                provenance.add(new ToolProvenanceRecord(
                        toolCall.name(),
                        summarize(result),
                        clock.instant()
                ));
                chart = firstNonNull(chart, extractChart(result));
                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        toolCall.id(),
                        toolCall.name(),
                        result
                ));
            }
            conversation.add(ToolResponseMessage.builder().responses(toolResponses).build());
        }

        if (narrative == null || narrative.isBlank()) {
            return fallbackService.analyze(message, traceId, "invalid_output");
        }

        return new AgentAnalysisResponse(
                narrative,
                chart,
                List.of(),
                provenance,
                false,
                null,
                traceId,
                "spring-ai"
        );
    }

    private ChartContract extractChart(String toolResultJson) {
        try {
            JsonNode root = MAPPER.readTree(toolResultJson);
            if (root.has("chartId") && root.has("schemaVersion")) {
                return MAPPER.treeToValue(root, ChartContract.class);
            }
            if (root.has("charts") && root.get("charts").isArray() && !root.get("charts").isEmpty()) {
                return MAPPER.treeToValue(root.get("charts").get(0), ChartContract.class);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static String summarize(String result) {
        if (result == null) {
            return "";
        }
        return result.length() <= 160 ? result : result.substring(0, 157) + "...";
    }

    private static ChartContract firstNonNull(ChartContract current, ChartContract candidate) {
        return candidate != null ? candidate : current;
    }

    private static String traceId(String correlationId) {
        return correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
    }
}