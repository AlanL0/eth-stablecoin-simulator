package com.ethsimulator.agent.budget;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentBudgetGuardTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-06-09T12:00:00Z");

    private final AtomicLong fakeNanos = new AtomicLong(0);
    private AgentBudgetProperties properties;
    private AgentDailyBudgetTracker dailyBudgetTracker;
    private AgentBudgetGuard guard;

    @BeforeEach
    void setUp() {
        properties = new AgentBudgetProperties();
        properties.setMaxTurns(2);
        properties.setMaxToolExecutions(2);
        properties.setMaxCharts(1);
        properties.setMaxOutputTokens(100);
        properties.setDailyModelCallCap(10);
        properties.setDailyUsdBudget(new BigDecimal("0.010000"));
        properties.setRequestTimeoutMs(1_000);
        properties.setModelCallTimeoutMs(500);
        properties.setToolCallTimeoutMs(200);
        properties.validateAndClamp();

        dailyBudgetTracker = new AgentDailyBudgetTracker(properties, Clock.fixed(FIXED_TIME, ZoneOffset.UTC));
        guard = new AgentBudgetGuard(
                properties,
                dailyBudgetTracker,
                new AgentCostLedger(),
                new SimpleMeterRegistry(),
                fakeNanos::get
        );
    }

    @Test
    void maxTurnsTripsGuardrail() {
        AgentRequestBudget budget = openBudget();
        guard.reserveModelCall(budget);
        guard.reserveModelCall(budget);

        assertThatThrownBy(() -> guard.reserveModelCall(budget))
                .isInstanceOf(AgentBudgetViolationException.class)
                .extracting(ex -> ((AgentBudgetViolationException) ex).fallbackReason())
                .isEqualTo(AgentFallbackReason.GUARDRAIL);
    }

    @Test
    void maxToolExecutionsTripsGuardrail() {
        AgentRequestBudget budget = openBudget();
        guard.beforeToolExecution(budget);
        guard.afterToolExecution(budget, fakeNanos.get());
        guard.beforeToolExecution(budget);
        guard.afterToolExecution(budget, fakeNanos.get());

        assertThatThrownBy(() -> guard.beforeToolExecution(budget))
                .isInstanceOf(AgentBudgetViolationException.class)
                .extracting(ex -> ((AgentBudgetViolationException) ex).fallbackReason())
                .isEqualTo(AgentFallbackReason.GUARDRAIL);
    }

    @Test
    void dailyBudgetTripsBudgetFallback() {
        properties.setDailyModelCallCap(1);
        properties.validateAndClamp();
        dailyBudgetTracker = new AgentDailyBudgetTracker(properties, Clock.fixed(FIXED_TIME, ZoneOffset.UTC));
        guard = new AgentBudgetGuard(
                properties,
                dailyBudgetTracker,
                new AgentCostLedger(),
                new SimpleMeterRegistry(),
                fakeNanos::get
        );

        AgentRequestBudget first = openBudget();
        guard.reserveModelCall(first);

        AgentRequestBudget second = openBudget();
        assertThatThrownBy(() -> guard.reserveModelCall(second))
                .isInstanceOf(AgentBudgetViolationException.class)
                .extracting(ex -> ((AgentBudgetViolationException) ex).fallbackReason())
                .isEqualTo(AgentFallbackReason.BUDGET);
    }

    @Test
    void outputTokenCapTripsGuardrail() {
        AgentRequestBudget budget = openBudget();
        assertThatThrownBy(() -> guard.recordOutputTokens(budget, new org.springframework.ai.chat.metadata.DefaultUsage(10, 150)))
                .isInstanceOf(AgentBudgetViolationException.class)
                .extracting(ex -> ((AgentBudgetViolationException) ex).fallbackReason())
                .isEqualTo(AgentFallbackReason.GUARDRAIL);
    }

    private AgentRequestBudget openBudget() {
        fakeNanos.set(0);
        return guard.openRequest("trace-budget");
    }
}