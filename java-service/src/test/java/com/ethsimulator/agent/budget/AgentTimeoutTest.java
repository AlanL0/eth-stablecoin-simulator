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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentTimeoutTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-06-09T12:00:00Z");

    private final AtomicLong fakeNanos = new AtomicLong(0);
    private AgentBudgetGuard guard;

    @BeforeEach
    void setUp() {
        AgentBudgetProperties properties = new AgentBudgetProperties();
        properties.setRequestTimeoutMs(1_000);
        properties.setModelCallTimeoutMs(400);
        properties.setToolCallTimeoutMs(200);
        properties.setDailyModelCallCap(10);
        properties.setDailyUsdBudget(new BigDecimal("10.000000"));
        properties.validateAndClamp();

        guard = new AgentBudgetGuard(
                properties,
                new AgentDailyBudgetTracker(properties, Clock.fixed(FIXED_TIME, ZoneOffset.UTC)),
                new AgentCostLedger(),
                new SimpleMeterRegistry(),
                fakeNanos::get
        );
    }

    @Test
    void requestDeadlineIsNotResetBetweenTurns() {
        AgentRequestBudget budget = guard.openRequest("trace-timeout");
        advanceMillis(500);
        guard.reserveModelCall(budget);

        advanceMillis(400);
        assertThatThrownBy(() -> guard.reserveModelCall(budget))
                .isInstanceOf(AgentBudgetViolationException.class)
                .extracting(ex -> ((AgentBudgetViolationException) ex).fallbackReason())
                .isEqualTo(AgentFallbackReason.TIMEOUT);
    }

    @Test
    void insufficientRemainingTimeBlocksAnotherModelCall() {
        AgentRequestBudget budget = guard.openRequest("trace-timeout");
        advanceMillis(700);
        assertThatThrownBy(() -> guard.reserveModelCall(budget))
                .isInstanceOf(AgentBudgetViolationException.class)
                .extracting(ex -> ((AgentBudgetViolationException) ex).fallbackReason())
                .isEqualTo(AgentFallbackReason.TIMEOUT);
    }

    private void advanceMillis(long millis) {
        fakeNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(millis));
    }
}