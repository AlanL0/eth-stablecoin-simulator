package com.ethsimulator.charts;

import com.ethsimulator.api.dto.SimulationRequest;
import com.ethsimulator.simulation.SimulationLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class ChartQueryParams {

    @Positive
    private Double ethAmount;

    @Positive
    private Double collateralUsd;

    @NotBlank
    private String protocol;

    @Positive
    @DecimalMax("10.0")
    private Double targetCollateralRatio;

    @Positive
    @DecimalMax("10.0")
    private Double liquidationRatio;

    @PositiveOrZero
    @DecimalMax("100.0")
    private Double stabilityFeePct;

    @NotNull
    @PositiveOrZero
    @DecimalMax("100.0")
    private Double deployYieldPct = 5.0;

    @Min(0)
    @Max(SimulationLimits.MAX_YEARS)
    private Integer years = 1;

    @Min(1)
    @Max(SimulationLimits.MAX_COMPOUNDS_PER_YEAR)
    private Integer compoundsPerYear = 12;

    @Positive
    private Double ethPriceUsd;

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

    public Double getEthAmount() {
        return ethAmount;
    }

    public void setEthAmount(Double ethAmount) {
        this.ethAmount = ethAmount;
    }

    public Double getCollateralUsd() {
        return collateralUsd;
    }

    public void setCollateralUsd(Double collateralUsd) {
        this.collateralUsd = collateralUsd;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Double getTargetCollateralRatio() {
        return targetCollateralRatio;
    }

    public void setTargetCollateralRatio(Double targetCollateralRatio) {
        this.targetCollateralRatio = targetCollateralRatio;
    }

    public Double getLiquidationRatio() {
        return liquidationRatio;
    }

    public void setLiquidationRatio(Double liquidationRatio) {
        this.liquidationRatio = liquidationRatio;
    }

    public Double getStabilityFeePct() {
        return stabilityFeePct;
    }

    public void setStabilityFeePct(Double stabilityFeePct) {
        this.stabilityFeePct = stabilityFeePct;
    }

    public Double getDeployYieldPct() {
        return deployYieldPct;
    }

    public void setDeployYieldPct(Double deployYieldPct) {
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

    public Double getEthPriceUsd() {
        return ethPriceUsd;
    }

    public void setEthPriceUsd(Double ethPriceUsd) {
        this.ethPriceUsd = ethPriceUsd;
    }
}