package com.ethsimulator.charts;

import com.ethsimulator.api.dto.SimulationRequest;
import com.ethsimulator.simulation.SimulationLimits;
import com.ethsimulator.util.FinancialMath;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class ChartQueryParams {

    @Positive
    private BigDecimal ethAmount;

    @Positive
    private BigDecimal collateralUsd;

    @NotBlank
    private String protocol;

    @Positive
    @DecimalMax("10.0")
    private BigDecimal targetCollateralRatio;

    @Positive
    @DecimalMax("10.0")
    private BigDecimal liquidationRatio;

    @PositiveOrZero
    @DecimalMax("100.0")
    private BigDecimal stabilityFeePct;

    @NotNull
    @PositiveOrZero
    @DecimalMax("100.0")
    private BigDecimal deployYieldPct = FinancialMath.bd("5.0");

    @Min(0)
    @Max(SimulationLimits.MAX_YEARS)
    private Integer years = 1;

    @Min(1)
    @Max(SimulationLimits.MAX_COMPOUNDS_PER_YEAR)
    private Integer compoundsPerYear = 12;

    @Positive
    private BigDecimal ethPriceUsd;

    public SimulationRequest toSimulationRequest() {
        SimulationRequest request = new SimulationRequest();
        request.setEthAmount(ethAmount);
        request.setCollateralUsd(collateralUsd);
        request.setProtocol(protocol);
        request.setTargetCollateralRatio(targetCollateralRatio);
        request.setLiquidationRatio(liquidationRatio);
        request.setStabilityFeePct(stabilityFeePct);
        request.setDeployYieldPct(deployYieldPct);
        request.setYears(years);
        request.setCompoundsPerYear(compoundsPerYear);
        request.setEthPriceUsd(ethPriceUsd);
        request.setTreasuryContextEnabled(false);
        return request;
    }

    public BigDecimal getEthAmount() {
        return ethAmount;
    }

    public void setEthAmount(BigDecimal ethAmount) {
        this.ethAmount = ethAmount;
    }

    public BigDecimal getCollateralUsd() {
        return collateralUsd;
    }

    public void setCollateralUsd(BigDecimal collateralUsd) {
        this.collateralUsd = collateralUsd;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public BigDecimal getTargetCollateralRatio() {
        return targetCollateralRatio;
    }

    public void setTargetCollateralRatio(BigDecimal targetCollateralRatio) {
        this.targetCollateralRatio = targetCollateralRatio;
    }

    public BigDecimal getLiquidationRatio() {
        return liquidationRatio;
    }

    public void setLiquidationRatio(BigDecimal liquidationRatio) {
        this.liquidationRatio = liquidationRatio;
    }

    public BigDecimal getStabilityFeePct() {
        return stabilityFeePct;
    }

    public void setStabilityFeePct(BigDecimal stabilityFeePct) {
        this.stabilityFeePct = stabilityFeePct;
    }

    public BigDecimal getDeployYieldPct() {
        return deployYieldPct;
    }

    public void setDeployYieldPct(BigDecimal deployYieldPct) {
        this.deployYieldPct = deployYieldPct;
    }

    public Integer getYears() {
        return years;
    }

    public void setYears(Integer years) {
        this.years = years;
    }

    public Integer getCompoundsPerYear() {
        return compoundsPerYear;
    }

    public void setCompoundsPerYear(Integer compoundsPerYear) {
        this.compoundsPerYear = compoundsPerYear;
    }

    public BigDecimal getEthPriceUsd() {
        return ethPriceUsd;
    }

    public void setEthPriceUsd(BigDecimal ethPriceUsd) {
        this.ethPriceUsd = ethPriceUsd;
    }
}