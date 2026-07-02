package com.ethsimulator.agent;

import com.ethsimulator.agent.tools.FixedIncomeAnalyticsTools;
import com.ethsimulator.agent.tools.SimulationToolRequest;
import com.ethsimulator.market.YieldSnapshotResponse;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FixedIncomeAnalyticsToolsTest {

    @Autowired
    private FixedIncomeAnalyticsTools analyticsTools;

    @Test
    void getLatestYieldsReturnsAuthoritativeSnapshot() {
        YieldSnapshotResponse response = analyticsTools.getLatestYields("USDC");

        assertThat(response.asset()).isEqualTo("USDC");
        assertThat(response.dataMode()).isIn("live", "seed_fallback");
        assertThat(response.yields()).isNotEmpty();
        assertThat(response.yields().getFirst().apyPct()).isGreaterThan(FinancialMath.bd("0"));
    }

    @Test
    void runSimulationReturnsJavaDerivedDebt() {
        var response = analyticsTools.runSimulation(new SimulationToolRequest(
                FinancialMath.bd("2"),
                "maker_sky",
                FinancialMath.bd("5"),
                1,
                12,
                "USDC"
        ));

        assertThat(response.stablecoinDebtUsd()).isGreaterThan(FinancialMath.bd("0"));
        assertThat(response.healthRatio()).isGreaterThan(FinancialMath.bd("0"));
        assertThat(response.charts()).isNotEmpty();
    }

    @Test
    void buildProtocolRatesChartUsesChartContractV2() {
        var chart = analyticsTools.buildProtocolRatesChart("USDC");

        assertThat(chart.schemaVersion()).isEqualTo("2.0");
        assertThat(chart.chartId()).isEqualTo("protocol_rates_comparison");
    }
}