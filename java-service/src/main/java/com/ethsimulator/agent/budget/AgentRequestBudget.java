package com.ethsimulator.agent.budget;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AgentRequestBudget {

    private final String traceId;
    private final long startedAtNanos;
    private final long deadlineNanos;
    private int modelTurns;
    private int toolExecutions;
    private int chartsEmitted;
    private int outputTokens;
    private final AtomicBoolean retryUsed = new AtomicBoolean(false);

    public AgentRequestBudget(String traceId, long startedAtNanos, long deadlineNanos) {
        this.traceId = traceId;
        this.startedAtNanos = startedAtNanos;
        this.deadlineNanos = deadlineNanos;
    }

    public String traceId() {
        return traceId;
    }

    public long startedAtNanos() {
        return startedAtNanos;
    }

    public long deadlineNanos() {
        return deadlineNanos;
    }

    public int modelTurns() {
        return modelTurns;
    }

    public int toolExecutions() {
        return toolExecutions;
    }

    public int chartsEmitted() {
        return chartsEmitted;
    }

    public int outputTokens() {
        return outputTokens;
    }

    public boolean retryUsed() {
        return retryUsed.get();
    }

    public void markRetryUsed() {
        retryUsed.set(true);
    }

    public void incrementModelTurn() {
        modelTurns++;
    }

    public void incrementToolExecution() {
        toolExecutions++;
    }

    public void incrementChartsEmitted() {
        chartsEmitted++;
    }

    public void addOutputTokens(int tokens) {
        if (tokens > 0) {
            outputTokens += tokens;
        }
    }

    public boolean isExpired(long nowNanos) {
        return nowNanos >= deadlineNanos;
    }

    public long remainingNanos(long nowNanos) {
        return Math.max(0L, deadlineNanos - nowNanos);
    }
}