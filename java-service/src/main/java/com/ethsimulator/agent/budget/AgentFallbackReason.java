package com.ethsimulator.agent.budget;

public final class AgentFallbackReason {

    public static final String DISABLED = "disabled";
    public static final String TIMEOUT = "timeout";
    public static final String BUDGET = "budget";
    public static final String PROVIDER = "provider";
    public static final String GUARDRAIL = "guardrail";
    public static final String INVALID_OUTPUT = "invalid_output";
    public static final String UNSUPPORTED = "unsupported";

    private AgentFallbackReason() {
    }
}