package com.ethsimulator.service;

import com.ethsimulator.api.dto.SimulationRequest;
import com.ethsimulator.api.error.ApiException;
import com.ethsimulator.market.EthPriceQuote;
import com.ethsimulator.market.EthPriceService;
import com.ethsimulator.simulation.ProtocolPreset;
import com.ethsimulator.simulation.ProtocolPresetRegistry;
import com.ethsimulator.simulation.SimulationEngine;
import com.ethsimulator.simulation.SimulationEngine.Result;
import com.ethsimulator.util.FinancialMath;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SimulationInputResolver {

    private static final BigDecimal COLLATERAL_TOLERANCE_RATE = FinancialMath.bd("0.005");

    private final ProtocolPresetRegistry presetRegistry;
    private final EthPriceService ethPriceService;

    public SimulationInputResolver(ProtocolPresetRegistry presetRegistry, EthPriceService ethPriceService) {
        this.presetRegistry = presetRegistry;
        this.ethPriceService = ethPriceService;
    }

    public ResolvedSimulation resolve(SimulationRequest request) {
        EthPriceQuote ethPrice = ethPriceService.resolvePrice(request.getEthPriceUsd());
        BigDecimal ethPriceUsd = ethPrice.priceUsd();
        if (ethPriceUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("INVALID_SIMULATION_INPUT", "ETH price must be positive", HttpStatus.BAD_REQUEST);
        }

        BigDecimal ethAmount = resolveEthAmount(request, ethPriceUsd);
        if (ethAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("INVALID_SIMULATION_INPUT", "ETH amount must be positive", HttpStatus.BAD_REQUEST);
        }

        ProtocolPreset preset = presetRegistry.find(request.getProtocol())
                .orElseThrow(() -> new ApiException(
                        "INVALID_SIMULATION_INPUT",
                        "Unknown protocol: " + request.getProtocol(),
                        HttpStatus.BAD_REQUEST));

        BigDecimal targetCollateralRatio = overrideOrDefault(
                request.getTargetCollateralRatio(), preset.collateralRatio());
        BigDecimal liquidationRatio = overrideOrDefault(
                request.getLiquidationRatio(), preset.liquidationRatio());
        BigDecimal stabilityFeePct = overrideOrDefault(
                request.getStabilityFeePct(), preset.stabilityFeePct());
        BigDecimal deployYieldPct = request.getDeployYieldPct();
        int years = request.getYears();
        int compoundsPerYear = request.getCompoundsPerYear();

        Result result = SimulationEngine.compute(
                ethAmount,
                ethPriceUsd,
                targetCollateralRatio,
                liquidationRatio,
                stabilityFeePct,
                deployYieldPct,
                years,
                compoundsPerYear
        );

        return new ResolvedSimulation(
                ethPrice,
                ethAmount,
                result,
                request.getProtocol(),
                targetCollateralRatio,
                liquidationRatio,
                stabilityFeePct,
                deployYieldPct,
                years,
                compoundsPerYear
        );
    }

    private static BigDecimal resolveEthAmount(SimulationRequest request, BigDecimal ethPriceUsd) {
        boolean hasEth = request.getEthAmount() != null;
        boolean hasCollateral = request.getCollateralUsd() != null;

        if (!hasEth && !hasCollateral) {
            throw new ApiException(
                    "INVALID_SIMULATION_INPUT",
                    "Either ethAmount or collateralUsd is required",
                    HttpStatus.BAD_REQUEST);
        }

        if (hasEth && hasCollateral) {
            BigDecimal ethFromRequest = request.getEthAmount();
            BigDecimal collateralFromRequest = request.getCollateralUsd();
            BigDecimal impliedCollateral = FinancialMath.multiply(ethFromRequest, ethPriceUsd);
            BigDecimal tolerance = FinancialMath.multiply(impliedCollateral.abs(), COLLATERAL_TOLERANCE_RATE);
            BigDecimal diff = FinancialMath.subtract(impliedCollateral, collateralFromRequest).abs();
            if (diff.compareTo(tolerance) > 0) {
                throw new ApiException(
                        "INVALID_SIMULATION_INPUT",
                        "ethAmount and collateralUsd disagree beyond 0.5% tolerance",
                        HttpStatus.BAD_REQUEST);
            }
            return ethFromRequest;
        }

        if (hasCollateral) {
            return FinancialMath.divide(request.getCollateralUsd(), ethPriceUsd, FinancialMath.ETH_SCALE);
        }

        return request.getEthAmount();
    }

    private static BigDecimal overrideOrDefault(BigDecimal override, BigDecimal presetValue) {
        return override != null ? override : presetValue;
    }

    public record ResolvedSimulation(
            EthPriceQuote ethPrice,
            BigDecimal ethAmount,
            Result result,
            String protocol,
            BigDecimal targetCollateralRatio,
            BigDecimal liquidationRatio,
            BigDecimal stabilityFeePct,
            BigDecimal deployYieldPct,
            int years,
            int compoundsPerYear
    ) {
        public String ethPriceSourceKey() {
            return ethPrice.source().name().toLowerCase();
        }

        public BigDecimal scaledEthAmount() {
            return FinancialMath.scaleEth(ethAmount);
        }
    }
}