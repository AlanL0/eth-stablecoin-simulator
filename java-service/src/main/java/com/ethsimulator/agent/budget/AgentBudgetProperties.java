package com.ethsimulator.agent.budget;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "eth-simulator.agent.budget")
public class AgentBudgetProperties {

    private static final int CEILING_REQUEST_TIMEOUT_MS = 60_000;
    private static final int CEILING_MODEL_CALL_TIMEOUT_MS = 30_000;
    private static final int CEILING_TOOL_CALL_TIMEOUT_MS = 15_000;
    private static final int CEILING_MAX_TURNS = 6;
    private static final int CEILING_MAX_TOOL_EXECUTIONS = 12;
    private static final int CEILING_MAX_CHARTS = 3;
    private static final int CEILING_MAX_OUTPUT_TOKENS = 4_096;
    private static final int CEILING_DAILY_MODEL_CALL_CAP = 2_000;
    private static final int CEILING_MAX_CONCURRENT_MODEL_CALLS = 32;

    private int requestTimeoutMs = 20_000;
    private int modelCallTimeoutMs = 12_000;
    private int toolCallTimeoutMs = 5_000;
    private int maxTurns = 3;
    private int maxToolExecutions = 4;
    private int maxCharts = 1;
    private int maxOutputTokens = 1_200;
    private int dailyModelCallCap = 200;
    private BigDecimal dailyUsdBudget = new BigDecimal("5.000000");
    private BigDecimal inputTokenUsdPer1k = new BigDecimal("0.001000");
    private BigDecimal outputTokenUsdPer1k = new BigDecimal("0.003000");
    private int estimatedInputTokensPerCall = 1_500;
    private int maxConcurrentModelCalls = 8;

    @PostConstruct
    public void validateAndClamp() {
        requestTimeoutMs = clamp(requestTimeoutMs, 1_000, CEILING_REQUEST_TIMEOUT_MS);
        modelCallTimeoutMs = clamp(modelCallTimeoutMs, 250, CEILING_MODEL_CALL_TIMEOUT_MS);
        toolCallTimeoutMs = clamp(toolCallTimeoutMs, 500, CEILING_TOOL_CALL_TIMEOUT_MS);
        maxTurns = clamp(maxTurns, 1, CEILING_MAX_TURNS);
        maxToolExecutions = clamp(maxToolExecutions, 1, CEILING_MAX_TOOL_EXECUTIONS);
        maxCharts = clamp(maxCharts, 0, CEILING_MAX_CHARTS);
        maxOutputTokens = clamp(maxOutputTokens, 64, CEILING_MAX_OUTPUT_TOKENS);
        dailyModelCallCap = clamp(dailyModelCallCap, 1, CEILING_DAILY_MODEL_CALL_CAP);
        maxConcurrentModelCalls = clamp(maxConcurrentModelCalls, 1, CEILING_MAX_CONCURRENT_MODEL_CALLS);
        if (dailyUsdBudget.compareTo(BigDecimal.ZERO) < 0) {
            dailyUsdBudget = BigDecimal.ZERO;
        }
        if (inputTokenUsdPer1k.compareTo(BigDecimal.ZERO) < 0) {
            inputTokenUsdPer1k = BigDecimal.ZERO;
        }
        if (outputTokenUsdPer1k.compareTo(BigDecimal.ZERO) < 0) {
            outputTokenUsdPer1k = BigDecimal.ZERO;
        }
        estimatedInputTokensPerCall = clamp(estimatedInputTokensPerCall, 64, CEILING_MAX_OUTPUT_TOKENS);
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getModelCallTimeoutMs() {
        return modelCallTimeoutMs;
    }

    public void setModelCallTimeoutMs(int modelCallTimeoutMs) {
        this.modelCallTimeoutMs = modelCallTimeoutMs;
    }

    public int getToolCallTimeoutMs() {
        return toolCallTimeoutMs;
    }

    public void setToolCallTimeoutMs(int toolCallTimeoutMs) {
        this.toolCallTimeoutMs = toolCallTimeoutMs;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    public int getMaxToolExecutions() {
        return maxToolExecutions;
    }

    public void setMaxToolExecutions(int maxToolExecutions) {
        this.maxToolExecutions = maxToolExecutions;
    }

    public int getMaxCharts() {
        return maxCharts;
    }

    public void setMaxCharts(int maxCharts) {
        this.maxCharts = maxCharts;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public int getDailyModelCallCap() {
        return dailyModelCallCap;
    }

    public void setDailyModelCallCap(int dailyModelCallCap) {
        this.dailyModelCallCap = dailyModelCallCap;
    }

    public BigDecimal getDailyUsdBudget() {
        return dailyUsdBudget;
    }

    public void setDailyUsdBudget(BigDecimal dailyUsdBudget) {
        this.dailyUsdBudget = dailyUsdBudget;
    }

    public BigDecimal getInputTokenUsdPer1k() {
        return inputTokenUsdPer1k;
    }

    public void setInputTokenUsdPer1k(BigDecimal inputTokenUsdPer1k) {
        this.inputTokenUsdPer1k = inputTokenUsdPer1k;
    }

    public BigDecimal getOutputTokenUsdPer1k() {
        return outputTokenUsdPer1k;
    }

    public void setOutputTokenUsdPer1k(BigDecimal outputTokenUsdPer1k) {
        this.outputTokenUsdPer1k = outputTokenUsdPer1k;
    }

    public int getEstimatedInputTokensPerCall() {
        return estimatedInputTokensPerCall;
    }

    public void setEstimatedInputTokensPerCall(int estimatedInputTokensPerCall) {
        this.estimatedInputTokensPerCall = estimatedInputTokensPerCall;
    }

    public int getMaxConcurrentModelCalls() {
        return maxConcurrentModelCalls;
    }

    public void setMaxConcurrentModelCalls(int maxConcurrentModelCalls) {
        this.maxConcurrentModelCalls = maxConcurrentModelCalls;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}