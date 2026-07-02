package com.ethsimulator.agent.budget;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AgentCostLedger {

    private static final int SCALE = 6;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public BigDecimal worstCaseReservation(AgentBudgetProperties properties) {
        BigDecimal inputCost = tokenCost(properties.getEstimatedInputTokensPerCall(), properties.getInputTokenUsdPer1k());
        BigDecimal outputCost = tokenCost(properties.getMaxOutputTokens(), properties.getOutputTokenUsdPer1k());
        return inputCost.add(outputCost);
    }

    public BigDecimal actualCost(Usage usage, AgentBudgetProperties properties) {
        if (usage == null) {
            return worstCaseReservation(properties);
        }
        int promptTokens = safeTokens(usage.getPromptTokens());
        int completionTokens = safeTokens(usage.getCompletionTokens());
        return tokenCost(promptTokens, properties.getInputTokenUsdPer1k())
                .add(tokenCost(completionTokens, properties.getOutputTokenUsdPer1k()));
    }

    private static BigDecimal tokenCost(int tokens, BigDecimal usdPer1k) {
        if (tokens <= 0 || usdPer1k == null || usdPer1k.signum() <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        return usdPer1k
                .multiply(BigDecimal.valueOf(tokens))
                .divide(BigDecimal.valueOf(1_000), SCALE, ROUNDING);
    }

    private static int safeTokens(Integer tokens) {
        return tokens == null ? 0 : Math.max(0, tokens);
    }
}