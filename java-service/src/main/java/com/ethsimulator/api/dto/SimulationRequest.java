package com.ethsimulator.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class SimulationRequest {

    @Positive
    private Double ethAmount;

    @Positive
    private Double collateralUsd;

    @NotBlank
    private String protocol;

    @Positive
    private Double targetCollateralRatio;

    @Positive
    private Double liquidationRatio;

    @PositiveOrZero
    private Double stabilityFeePct;

    @NotNull
    @PositiveOrZero
    private Double deployYieldPct = 5.0;

    @Min(0)
    private Integer years = 1;

    @Min(1)
    private Integer compoundsPerYear = 12;

    private Boolean treasuryContextEnabled = true;

    private String stablecoinReserveModel = "usdc_style";

    @PositiveOrZero
    private Double reserveInTreasuriesPct;

    @PositiveOrZero
    private Double tbillApyPct;

    @Positive
    private Double systemSupplyUsd;

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

    public Boolean getTreasuryContextEnabled() {
        return treasuryContextEnabled;
    }

    public void setTreasuryContextEnabled(Boolean treasuryContextEnabled) {
        this.treasuryContextEnabled = treasuryContextEnabled;
    }

    public String getStablecoinReserveModel() {
        return stablecoinReserveModel;
    }

    public void setStablecoinReserveModel(String stablecoinReserveModel) {
        this.stablecoinReserveModel = stablecoinReserveModel;
    }

    public Double getReserveInTreasuriesPct() {
        return reserveInTreasuriesPct;
    }

    public void setReserveInTreasuriesPct(Double reserveInTreasuriesPct) {
        this.reserveInTreasuriesPct = reserveInTreasuriesPct;
    }

    public Double getTbillApyPct() {
        return tbillApyPct;
    }

    public void setTbillApyPct(Double tbillApyPct) {
        this.tbillApyPct = tbillApyPct;
    }

    public Double getSystemSupplyUsd() {
        return systemSupplyUsd;
    }

    public void setSystemSupplyUsd(Double systemSupplyUsd) {
        this.systemSupplyUsd = systemSupplyUsd;
    }
}