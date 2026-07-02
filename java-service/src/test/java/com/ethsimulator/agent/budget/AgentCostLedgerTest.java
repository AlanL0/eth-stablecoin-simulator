package com.ethsimulator.agent.budget;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.DefaultUsage;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AgentCostLedgerTest {

    @Test
    void worstCaseReservationUsesConfiguredTokenPrices() {
        AgentBudgetProperties properties = new AgentBudgetProperties();
        properties.setEstimatedInputTokensPerCall(1_000);
        properties.setMaxOutputTokens(1_200);
        properties.setInputTokenUsdPer1k(new BigDecimal("0.001000"));
        properties.setOutputTokenUsdPer1k(new BigDecimal("0.003000"));
        properties.validateAndClamp();

        AgentCostLedger ledger = new AgentCostLedger();
        BigDecimal reservation = ledger.worstCaseReservation(properties);

        assertThat(reservation).isEqualByComparingTo(new BigDecimal("0.004600"));
    }

    @Test
    void missingUsageFallsBackToWorstCaseCost() {
        AgentBudgetProperties properties = new AgentBudgetProperties();
        properties.validateAndClamp();
        AgentCostLedger ledger = new AgentCostLedger();

        assertThat(ledger.actualCost(null, properties)).isEqualByComparingTo(ledger.worstCaseReservation(properties));
    }

    @Test
    void reportedUsageReconcilesBelowWorstCase() {
        AgentBudgetProperties properties = new AgentBudgetProperties();
        properties.setInputTokenUsdPer1k(new BigDecimal("0.001000"));
        properties.setOutputTokenUsdPer1k(new BigDecimal("0.003000"));
        properties.validateAndClamp();

        AgentCostLedger ledger = new AgentCostLedger();
        BigDecimal actual = ledger.actualCost(new DefaultUsage(100, 50), properties);

        assertThat(actual).isEqualByComparingTo(new BigDecimal("0.000250"));
        assertThat(actual).isLessThan(ledger.worstCaseReservation(properties));
    }
}