package com.ethsimulator.agent;

import com.ethsimulator.agent.budget.AgentBudgetGuard;
import com.ethsimulator.agent.budget.AgentBudgetProperties;
import com.ethsimulator.agent.budget.AgentCostLedger;
import com.ethsimulator.agent.budget.AgentDailyBudgetTracker;
import com.ethsimulator.agent.budget.AgentModelBulkhead;
import com.ethsimulator.agent.budget.AgentRunRecorder;
import com.ethsimulator.agent.dto.AgentAnalyzeRequest;
import com.ethsimulator.agent.support.AutoToolExecutingChatModel;
import com.ethsimulator.agent.support.ProviderShapedChatModel;
import com.ethsimulator.agent.support.ScriptedChatModel;
import com.ethsimulator.agent.tools.FixedIncomeAnalyticsTools;
import com.ethsimulator.config.AgentAiProperties;
import com.ethsimulator.config.UnavailableChatModel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AgentOrchestratorServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-06-09T12:00:00Z");

    @Autowired
    private FixedIncomeAnalyticsTools analyticsTools;

    private ScriptedChatModel chatModel;
    private AgentOrchestratorService orchestratorService;
    private AgentBudgetProperties budgetProperties;

    @BeforeEach
    void setUp() {
        chatModel = new ScriptedChatModel();
        Clock clock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
        AgentAiProperties properties = new AgentAiProperties();
        properties.setEnabled(true);

        budgetProperties = new AgentBudgetProperties();
        budgetProperties.setDailyModelCallCap(100);
        budgetProperties.setDailyUsdBudget(new BigDecimal("100.000000"));
        budgetProperties.validateAndClamp();

        AgentDailyBudgetTracker dailyBudgetTracker = new AgentDailyBudgetTracker(budgetProperties, clock);
        AgentCostLedger costLedger = new AgentCostLedger();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentBudgetGuard budgetGuard = new AgentBudgetGuard(
                budgetProperties,
                dailyBudgetTracker,
                costLedger,
                meterRegistry
        );
        AgentModelBulkhead bulkhead = new AgentModelBulkhead(budgetProperties);
        @SuppressWarnings("unchecked")
        ObjectProvider<com.ethsimulator.persistence.AgentRunRepository> repositoryProvider =
                mock(ObjectProvider.class);
        when(repositoryProvider.getIfAvailable()).thenReturn(null);
        AgentRunRecorder runRecorder = new AgentRunRecorder(repositoryProvider, meterRegistry);

        orchestratorService = new AgentOrchestratorService(
                chatModel,
                analyticsTools,
                new AgentDeterministicFallbackService(analyticsTools, clock),
                properties,
                budgetProperties,
                budgetGuard,
                bulkhead,
                costLedger,
                runRecorder,
                clock
        );
    }

    @Test
    void modelToolLoopProducesNarrativeAndProvenance() {
        chatModel.enqueue(toolCallResponse("getLatestYields", "{\"asset\":\"USDC\"}"));
        chatModel.enqueue(textResponse("Borrowing costs are shown from authoritative Java yields."));

        var response = orchestratorService.analyze(new AgentAnalyzeRequest("Compare current borrowing costs", "trace-1"));

        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.narrative()).contains("Borrowing costs");
        assertThat(response.toolProvenance()).hasSize(1);
        assertThat(response.toolProvenance().getFirst().toolName()).isEqualTo("getLatestYields");
        assertThat(response.traceId()).isEqualTo("trace-1");
    }

    @Test
    void providerShapedModelLeavesToolExecutionToOrchestrator() {
        ProviderShapedChatModel providerModel = new ProviderShapedChatModel();
        providerModel.enqueue(toolCallResponse("getLatestYields", "{\"asset\":\"USDC\"}"));
        providerModel.enqueue(textResponse("USDC yields are authoritative from Java tools."));
        AgentOrchestratorService providerBacked = orchestratorWithChatModel(providerModel);

        var response = providerBacked.analyze(new AgentAnalyzeRequest("Compare USDC yields", "provider-shaped"));

        assertThat(providerModel.callCount()).isEqualTo(2);
        assertThat(providerModel.toolCallbacksObserved()).isTrue();
        assertThat(response.toolProvenance()).hasSize(1);
        assertThat(response.toolProvenance().getFirst().toolName()).isEqualTo("getLatestYields");
        assertThat(response.narrative()).contains("authoritative");
    }

    @Test
    void autoExecutingChatModelBypassesManualProvenance() {
        AgentOrchestratorService autoExecuting = orchestratorWithChatModel(new AutoToolExecutingChatModel());

        var response = autoExecuting.analyze(new AgentAnalyzeRequest("Compare USDC yields", "auto-exec"));

        assertThat(response.toolProvenance()).isEmpty();
        assertThat(response.narrative()).contains("Auto-executed");
    }

    private AgentOrchestratorService orchestratorWithChatModel(org.springframework.ai.chat.model.ChatModel model) {
        Clock clock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
        AgentAiProperties properties = new AgentAiProperties();
        properties.setEnabled(true);
        AgentBudgetProperties localBudget = new AgentBudgetProperties();
        localBudget.setDailyModelCallCap(100);
        localBudget.setDailyUsdBudget(new BigDecimal("100.000000"));
        localBudget.validateAndClamp();
        AgentDailyBudgetTracker dailyBudgetTracker = new AgentDailyBudgetTracker(localBudget, clock);
        AgentCostLedger costLedger = new AgentCostLedger();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentBudgetGuard budgetGuard = new AgentBudgetGuard(localBudget, dailyBudgetTracker, costLedger, meterRegistry);
        @SuppressWarnings("unchecked")
        ObjectProvider<com.ethsimulator.persistence.AgentRunRepository> repositoryProvider =
                mock(ObjectProvider.class);
        when(repositoryProvider.getIfAvailable()).thenReturn(null);
        return new AgentOrchestratorService(
                model,
                analyticsTools,
                new AgentDeterministicFallbackService(analyticsTools, clock),
                properties,
                localBudget,
                budgetGuard,
                new AgentModelBulkhead(localBudget),
                costLedger,
                new AgentRunRecorder(repositoryProvider, meterRegistry),
                clock
        );
    }

    @Test
    void providerFailureFallsBackDeterministically() {
        chatModel.failWith(new RuntimeException("provider down"));

        var response = orchestratorService.analyze(new AgentAnalyzeRequest("Compare borrowing costs", null));

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("provider");
        assertThat(response.model()).isEqualTo("deterministic-fallback");
        assertThat(response.narrative()).containsIgnoringCase("borrowing");
    }

    @Test
    void disabledModelFallsBackWithoutCallingProvider() {
        Clock clock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
        AgentAiProperties properties = new AgentAiProperties();
        properties.setEnabled(true);
        AgentBudgetProperties localBudget = new AgentBudgetProperties();
        localBudget.validateAndClamp();
        AgentDailyBudgetTracker dailyBudgetTracker = new AgentDailyBudgetTracker(localBudget, clock);
        AgentCostLedger costLedger = new AgentCostLedger();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentBudgetGuard budgetGuard = new AgentBudgetGuard(localBudget, dailyBudgetTracker, costLedger, meterRegistry);
        @SuppressWarnings("unchecked")
        ObjectProvider<com.ethsimulator.persistence.AgentRunRepository> disabledRepositoryProvider =
                mock(ObjectProvider.class);
        when(disabledRepositoryProvider.getIfAvailable()).thenReturn(null);
        AgentOrchestratorService disabled = new AgentOrchestratorService(
                new UnavailableChatModel(),
                analyticsTools,
                new AgentDeterministicFallbackService(analyticsTools, clock),
                properties,
                localBudget,
                budgetGuard,
                new AgentModelBulkhead(localBudget),
                costLedger,
                new AgentRunRecorder(disabledRepositoryProvider, meterRegistry),
                clock
        );

        var response = disabled.analyze(new AgentAnalyzeRequest("unknown question about poetry", "corr"));

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("disabled");
    }

    @Test
    void malformedFinalOutputFallsBack() {
        chatModel.enqueue(toolCallResponse("getLatestYields", "{\"asset\":\"USDC\"}"));
        chatModel.enqueue(textResponse("   "));

        var response = orchestratorService.analyze(new AgentAnalyzeRequest("Compare borrowing costs", null));

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("invalid_output");
    }

    private static ChatResponse toolCallResponse(String toolName, String arguments) {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", toolName, arguments)))
                .build();
        return ChatResponse.builder().generations(List.of(new Generation(message))).build();
    }

    private static ChatResponse textResponse(String text) {
        AssistantMessage message = new AssistantMessage(text);
        return ChatResponse.builder().generations(List.of(new Generation(message))).build();
    }
}