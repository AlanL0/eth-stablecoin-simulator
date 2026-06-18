package com.ethsimulator.api.dto;

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

public class SimulationRequest {

    @Positive
    private BigDecimal ethAmount;

    @Positive
    private BigDecimal collateralUsd;

    @Positive
    private BigDecimal ethPriceUsd;

    @NotBlank
    private String protocol;

    @Positive
    @DecimalMax(value = "10.0", message = "must be <= 10")
    private BigDecimal targetCollateralRatio;

    @Positive
    @DecimalMax(value = "10.0", message = "must be <= 10")
    private BigDecimal liquidationRatio;

    @PositiveOrZero
    @DecimalMax(value = "100.0", message = "must be <= 100")
    private BigDecimal stabilityFeePct;

    @NotNull
    @PositiveOrZero
    @DecimalMax(value = "100.0", message = "must be <= 100")
    private BigDecimal deployYieldPct = FinancialMath.bd("5.0");

    @Min(0)
    @Max(value = SimulationLimits.MAX_YEARS, message = "must be <= 50")
    private Integer years = 1;

    @Min(1)
    @Max(value = SimulationLimits.MAX_COMPOUNDS_PER_YEAR, message = "must be <= 365")
    private Integer compoundsPerYear = 12;

    private Boolean treasuryContextEnabled = true;

    private String stablecoinReserveModel = "usdc_style";

    @PositiveOrZero
    @DecimalMax(value = "100.0", message = "must be <= 100")
    private BigDecimal reserveInTreasuriesPct;

    @PositiveOrZero
    @DecimalMax(value = "100.0", message = "must be <= 100")
    private BigDecimal tbillApyPct;

    @Positive
    private BigDecimal systemSupplyUsd;

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

    public BigDecimal getEthPriceUsd() {
        return ethPriceUsd;
    }

    public void setEthPriceUsd(BigDecimal ethPriceUsd) {
        this.ethPriceUsd = ethPriceUsd;
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

    public BigDecimal getReserveInTreasuriesPct() {
        return reserveInTreasuriesPct;
    }

    public void setReserveInTreasuriesPct(BigDecimal reserveInTreasuriesPct) {
        this.reserveInTreasuriesPct = reserveInTreasuriesPct;
    }

    public BigDecimal getTbillApyPct() {
        return tbillApyPct;
    }

    public void setTbillApyPct(BigDecimal tbillApyPct) {
        this.tbillApyPct = tbillApyPct;
    }

    public BigDecimal getSystemSupplyUsd() {
        return systemSupplyUsd;
    }

    public void setSystemSupplyUsd(BigDecimal systemSupplyUsd) {
        this.systemSupplyUsd = systemSupplyUsd;
    }
}