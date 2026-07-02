package com.ethsimulator.agent;

import com.ethsimulator.agent.dto.AgentAnalyzeRequest;
import com.ethsimulator.agent.support.ScriptedChatModel;
import com.ethsimulator.agent.tools.FixedIncomeAnalyticsTools;
import com.ethsimulator.config.AgentAiProperties;
import com.ethsimulator.config.UnavailableChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

    @BeforeEach
    void setUp() {
        chatModel = new ScriptedChatModel();
        AgentAiProperties properties = new AgentAiProperties();
        properties.setEnabled(true);
        properties.setMaxTurns(3);
        orchestratorService = new AgentOrchestratorService(
                chatModel,
                analyticsTools,
                new AgentDeterministicFallbackService(analyticsTools, Clock.fixed(FIXED_TIME, ZoneOffset.UTC)),
                properties,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
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
        AgentAiProperties properties = new AgentAiProperties();
        properties.setEnabled(true);
        AgentOrchestratorService disabled = new AgentOrchestratorService(
                new UnavailableChatModel(),
                analyticsTools,
                new AgentDeterministicFallbackService(analyticsTools, Clock.fixed(FIXED_TIME, ZoneOffset.UTC)),
                properties,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
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