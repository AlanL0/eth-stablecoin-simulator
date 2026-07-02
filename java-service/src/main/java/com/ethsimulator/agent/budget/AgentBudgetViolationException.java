package com.ethsimulator.agent.budget;

public class AgentBudgetViolationException extends RuntimeException {

    private final String fallbackReason;

    public AgentBudgetViolationException(String fallbackReason, String message) {
        super(message);
        this.fallbackReason = fallbackReason;
    }

    public String fallbackReason() {
        return fallbackReason;
    }
}