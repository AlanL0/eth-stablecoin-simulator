package com.ethsimulator.agent.budget;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
public class AgentBudgetGuard {

    private final AgentBudgetProperties properties;
    private final AgentDailyBudgetTracker dailyBudgetTracker;
    private final AgentCostLedger costLedger;
    private final MonotonicClock monotonicClock;

    @Autowired
    public AgentBudgetGuard(
            AgentBudgetProperties properties,
            AgentDailyBudgetTracker dailyBudgetTracker,
            AgentCostLedger costLedger,
            MeterRegistry meterRegistry
    ) {
        this(properties, dailyBudgetTracker, costLedger, meterRegistry, MonotonicClock.system());
    }

    public AgentBudgetGuard(
            AgentBudgetProperties properties,
            AgentDailyBudgetTracker dailyBudgetTracker,
            AgentCostLedger costLedger,
            MeterRegistry meterRegistry,
            MonotonicClock monotonicClock
    ) {
        this.properties = properties;
        this.dailyBudgetTracker = dailyBudgetTracker;
        this.costLedger = costLedger;
        this.monotonicClock = monotonicClock;
        meterRegistry.timer("ethsim_agent_request_latency");
    }

    public AgentBudgetProperties properties() {
        return properties;
    }

    public long nanoTime() {
        return monotonicClock.nanoTime();
    }

    public AgentRequestBudget openRequest(String traceId) {
        long startedAtNanos = monotonicClock.nanoTime();
        long deadlineNanos = startedAtNanos + TimeUnit.MILLISECONDS.toNanos(properties.getRequestTimeoutMs());
        return new AgentRequestBudget(traceId, startedAtNanos, deadlineNanos);
    }

    public void assertRequestAlive(AgentRequestBudget budget) {
        if (budget.isExpired(monotonicClock.nanoTime())) {
            throw new AgentBudgetViolationException(AgentFallbackReason.TIMEOUT, "Agent request deadline exceeded");
        }
    }

    public BigDecimal reserveModelCall(AgentRequestBudget budget) {
        assertRequestAlive(budget);
        if (budget.modelTurns() >= properties.getMaxTurns()) {
            throw new AgentBudgetViolationException(AgentFallbackReason.GUARDRAIL, "Maximum model turns exceeded");
        }
        long remainingNanos = budget.remainingNanos(monotonicClock.nanoTime());
        long modelCallNanos = TimeUnit.MILLISECONDS.toNanos(properties.getModelCallTimeoutMs());
        if (remainingNanos < modelCallNanos) {
            throw new AgentBudgetViolationException(AgentFallbackReason.TIMEOUT, "Insufficient time for model call");
        }
        BigDecimal reservation = costLedger.worstCaseReservation(properties);
        if (!dailyBudgetTracker.tryReserveModelCallAndUsd(reservation)) {
            throw new AgentBudgetViolationException(AgentFallbackReason.BUDGET, "Daily agent budget exceeded");
        }
        budget.incrementModelTurn();
        return reservation;
    }

    public void reconcileModelCall(BigDecimal reservation, org.springframework.ai.chat.metadata.Usage usage) {
        dailyBudgetTracker.reconcile(reservation, costLedger.actualCost(usage, properties));
    }

    public void beforeToolExecution(AgentRequestBudget budget) {
        assertRequestAlive(budget);
        if (budget.toolExecutions() >= properties.getMaxToolExecutions()) {
            throw new AgentBudgetViolationException(AgentFallbackReason.GUARDRAIL, "Maximum tool executions exceeded");
        }
        long remainingNanos = budget.remainingNanos(monotonicClock.nanoTime());
        long toolCallNanos = TimeUnit.MILLISECONDS.toNanos(properties.getToolCallTimeoutMs());
        if (remainingNanos < toolCallNanos) {
            throw new AgentBudgetViolationException(AgentFallbackReason.TIMEOUT, "Insufficient time for tool call");
        }
        budget.incrementToolExecution();
    }

    public void afterToolExecution(AgentRequestBudget budget, long startedAtNanos) {
        long elapsedNanos = monotonicClock.nanoTime() - startedAtNanos;
        if (elapsedNanos > TimeUnit.MILLISECONDS.toNanos(properties.getToolCallTimeoutMs())
                || budget.isExpired(monotonicClock.nanoTime())) {
            throw new AgentBudgetViolationException(AgentFallbackReason.TIMEOUT, "Tool call exceeded timeout");
        }
    }

    public boolean canAttachChart(AgentRequestBudget budget) {
        return budget.chartsEmitted() < properties.getMaxCharts();
    }

    public void recordChart(AgentRequestBudget budget) {
        budget.incrementChartsEmitted();
    }

    public void recordOutputTokens(AgentRequestBudget budget, org.springframework.ai.chat.metadata.Usage usage) {
        int completionTokens = usage == null || usage.getCompletionTokens() == null
                ? 0
                : usage.getCompletionTokens();
        budget.addOutputTokens(completionTokens);
        if (budget.outputTokens() > properties.getMaxOutputTokens()) {
            throw new AgentBudgetViolationException(AgentFallbackReason.GUARDRAIL, "Maximum output tokens exceeded");
        }
    }
}