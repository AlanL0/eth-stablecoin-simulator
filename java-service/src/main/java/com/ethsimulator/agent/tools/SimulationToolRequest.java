package com.ethsimulator.agent.tools;

import com.ethsimulator.util.FinancialMath;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;

public record SimulationToolRequest(
        @ToolParam(description = "ETH collateral amount; must be positive", required = false)
        BigDecimal ethAmount,
        @ToolParam(description = "Protocol preset: maker_sky, liquity, aave_gho, or custom", required = false)
        String protocol,
        @ToolParam(description = "Deploy yield percent (human percent, 0-100)", required = false)
        BigDecimal deployYieldPct,
        @ToolParam(description = "Horizon in years (0-50)", required = false)
        Integer years,
        @ToolParam(description = "Compounding periods per year (1-365)", required = false)
        Integer compoundsPerYear,
        @ToolParam(description = "Stablecoin asset symbol for rate charts, e.g. USDC", required = false)
        String asset
) {
    public BigDecimal resolvedEthAmount() {
        return ethAmount == null ? FinancialMath.bd("2") : ethAmount;
    }

    public String resolvedProtocol() {
        return protocol == null || protocol.isBlank() ? "maker_sky" : protocol;
    }

    public BigDecimal resolvedDeployYieldPct() {
        return deployYieldPct == null ? FinancialMath.bd("5") : deployYieldPct;
    }

    public int resolvedYears() {
        return years == null ? 1 : years;
    }

    public int resolvedCompoundsPerYear() {
        return compoundsPerYear == null ? 12 : compoundsPerYear;
    }

    public String resolvedAsset() {
        return asset == null || asset.isBlank() ? "USDC" : asset.trim().toUpperCase();
    }
}