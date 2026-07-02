package com.ethsimulator.agent;

import com.ethsimulator.agent.budget.AgentBudgetGuard;
import com.ethsimulator.agent.budget.AgentBudgetProperties;
import com.ethsimulator.agent.budget.AgentBudgetViolationException;
import com.ethsimulator.agent.budget.AgentCostLedger;
import com.ethsimulator.agent.budget.AgentFallbackReason;
import com.ethsimulator.agent.budget.AgentModelBulkhead;
import com.ethsimulator.agent.budget.AgentProviderFailureClassifier;
import com.ethsimulator.agent.budget.AgentRequestBudget;
import com.ethsimulator.agent.budget.AgentRunRecorder;
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
import org.springframework.ai.chat.metadata.Usage;
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

import java.math.BigDecimal;
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
    private final AgentBudgetProperties budgetProperties;
    private final AgentBudgetGuard budgetGuard;
    private final AgentModelBulkhead modelBulkhead;
    private final AgentCostLedger costLedger;
    private final AgentRunRecorder runRecorder;
    private final Clock clock;

    public AgentOrchestratorService(
            ChatModel chatModel,
            FixedIncomeAnalyticsTools analyticsTools,
            AgentDeterministicFallbackService fallbackService,
            AgentAiProperties agentAiProperties,
            AgentBudgetProperties budgetProperties,
            AgentBudgetGuard budgetGuard,
            AgentModelBulkhead modelBulkhead,
            AgentCostLedger costLedger,
            AgentRunRecorder runRecorder,
            Clock clock
    ) {
        this.chatModel = chatModel;
        this.analyticsTools = analyticsTools;
        this.fallbackService = fallbackService;
        this.agentAiProperties = agentAiProperties;
        this.budgetProperties = budgetProperties;
        this.budgetGuard = budgetGuard;
        this.modelBulkhead = modelBulkhead;
        this.costLedger = costLedger;
        this.runRecorder = runRecorder;
        this.clock = clock;
    }

    public AgentAnalysisResponse analyze(AgentAnalyzeRequest request) {
        String traceId = traceId(request.correlationId());
        AgentRequestBudget budget = budgetGuard.openRequest(traceId);
        long startedAtMillis = clock.millis();
        BigDecimal estimatedCostUsd = BigDecimal.ZERO;

        try {
            if (!agentAiProperties.isEnabled() || chatModel instanceof UnavailableChatModel) {
                return finishFallback(request, traceId, AgentFallbackReason.DISABLED, budget, startedAtMillis, estimatedCostUsd);
            }
            if (!modelBulkhead.tryAcquire()) {
                return finishFallback(request, traceId, AgentFallbackReason.GUARDRAIL, budget, startedAtMillis, estimatedCostUsd);
            }
            try {
                return runModelLoop(request.message(), traceId, budget, startedAtMillis);
            } finally {
                modelBulkhead.release();
            }
        } catch (AgentBudgetViolationException ex) {
            return finishFallback(request, traceId, ex.fallbackReason(), budget, startedAtMillis, estimatedCostUsd);
        } catch (RuntimeException ex) {
            log.warn("Agent orchestration failed traceId={}: {}", traceId, ex.getMessage());
            return finishFallback(request, traceId, AgentFallbackReason.PROVIDER, budget, startedAtMillis, estimatedCostUsd);
        }
    }

    private AgentAnalysisResponse finishFallback(
            AgentAnalyzeRequest request,
            String traceId,
            String reason,
            AgentRequestBudget budget,
            long startedAtMillis,
            BigDecimal estimatedCostUsd
    ) {
        AgentAnalysisResponse response = fallbackService.analyze(request.message(), traceId, reason);
        runRecorder.record(budget, response, clock.millis() - startedAtMillis, estimatedCostUsd);
        return response;
    }

    private AgentAnalysisResponse runModelLoop(
            String message,
            String traceId,
            AgentRequestBudget budget,
            long startedAtMillis
    ) {
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
        BigDecimal estimatedCostUsd = BigDecimal.ZERO;
        List<String> warnings = new ArrayList<>();

        while (true) {
            budgetGuard.assertRequestAlive(budget);
            ModelTurnResult turn = callModelWithOptionalRetry(conversation, toolCallbacks, budget);
            estimatedCostUsd = estimatedCostUsd.add(turn.costUsd());

            ChatResponse response = turn.response();
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
                    return finishFallback(
                            new AgentAnalyzeRequest(message, traceId),
                            traceId,
                            AgentFallbackReason.UNSUPPORTED,
                            budget,
                            startedAtMillis,
                            estimatedCostUsd
                    );
                }
                String result = executeTool(callback, toolCall.arguments(), budget);
                provenance.add(new ToolProvenanceRecord(
                        toolCall.name(),
                        summarize(result),
                        clock.instant()
                ));
                ChartContract extracted = extractChart(result);
                if (extracted != null) {
                    if (budgetGuard.canAttachChart(budget)) {
                        chart = firstNonNull(chart, extracted);
                        budgetGuard.recordChart(budget);
                    } else {
                        warnings.add("Chart omitted because request chart budget was exhausted.");
                    }
                }
                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        toolCall.id(),
                        toolCall.name(),
                        result
                ));
            }
            conversation.add(ToolResponseMessage.builder().responses(toolResponses).build());
        }

        if (narrative == null || narrative.isBlank()) {
            return finishFallback(
                    new AgentAnalyzeRequest(message, traceId),
                    traceId,
                    AgentFallbackReason.INVALID_OUTPUT,
                    budget,
                    startedAtMillis,
                    estimatedCostUsd
            );
        }

        AgentAnalysisResponse response = new AgentAnalysisResponse(
                narrative,
                chart,
                warnings,
                provenance,
                false,
                null,
                traceId,
                "spring-ai"
        );
        runRecorder.record(budget, response, clock.millis() - startedAtMillis, estimatedCostUsd);
        return response;
    }

    private ModelTurnResult callModelWithOptionalRetry(
            List<Message> conversation,
            ToolCallback[] toolCallbacks,
            AgentRequestBudget budget
    ) {
        try {
            return invokeReservedModelTurn(conversation, toolCallbacks, budget);
        } catch (RuntimeException firstFailure) {
            if (!AgentProviderFailureClassifier.isTransient(firstFailure) || budget.retryUsed()) {
                throw firstFailure;
            }
            budget.markRetryUsed();
            budgetGuard.assertRequestAlive(budget);
            return invokeReservedModelTurn(conversation, toolCallbacks, budget);
        }
    }

    private ModelTurnResult invokeReservedModelTurn(
            List<Message> conversation,
            ToolCallback[] toolCallbacks,
            AgentRequestBudget budget
    ) {
        BigDecimal reservation = budgetGuard.reserveModelCall(budget);
        ChatResponse response = invokeModel(conversation, toolCallbacks);
        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        budgetGuard.reconcileModelCall(reservation, usage);
        budgetGuard.recordOutputTokens(budget, usage);
        BigDecimal actualCost = costLedger.actualCost(usage, budgetProperties);
        return new ModelTurnResult(response, actualCost);
    }

    private ChatResponse invokeModel(List<Message> conversation, ToolCallback[] toolCallbacks) {
        // Spring AI 2.0: ChatModel.call returns tool-call responses only; execution stays here.
        // Do not route this flow through ChatClient's auto-registered ToolCallingAdvisor.
        return chatModel.call(new Prompt(conversation, toolCallingOptions(toolCallbacks)));
    }

    private String executeTool(ToolCallback callback, String arguments, AgentRequestBudget budget) {
        budgetGuard.beforeToolExecution(budget);
        long startedAtNanos = budgetGuard.nanoTime();
        try {
            return callback.call(arguments);
        } finally {
            budgetGuard.afterToolExecution(budget, startedAtNanos);
        }
    }

    private static ToolCallingChatOptions toolCallingOptions(ToolCallback[] toolCallbacks) {
        return ToolCallingChatOptions.builder()
                .toolCallbacks(List.of(toolCallbacks))
                .build();
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

    private record ModelTurnResult(ChatResponse response, BigDecimal costUsd) {
    }
}