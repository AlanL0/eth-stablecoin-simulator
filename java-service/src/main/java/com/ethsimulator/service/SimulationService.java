package com.ethsimulator.service;

import com.ethsimulator.api.dto.SimulationRequest;
import com.ethsimulator.api.dto.SimulationResponse;
import com.ethsimulator.api.dto.SimulationResponse.Assumptions;
import com.ethsimulator.api.error.ApiException;
import com.ethsimulator.charts.ChartBuilders;
import com.ethsimulator.charts.ChartModels.ChartSpec;
import com.ethsimulator.config.EthSimulatorProperties;
import com.ethsimulator.simulation.ProtocolPreset;
import com.ethsimulator.simulation.ProtocolPresetRegistry;
import com.ethsimulator.simulation.SimulationEngine;
import com.ethsimulator.simulation.SimulationEngine.Result;
import com.ethsimulator.treasury.StablecoinReserveModel;
import com.ethsimulator.treasury.TreasuryContext;
import com.ethsimulator.treasury.TreasuryContextBuilder;
import com.ethsimulator.util.UsdMath;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SimulationService {

    private static final BigDecimal COLLATERAL_TOLERANCE_RATE = new BigDecimal("0.005");

    private final ProtocolPresetRegistry presetRegistry;
    private final EthSimulatorProperties properties;
    private final TreasuryContextBuilder treasuryContextBuilder;
    private final Clock clock;

    public SimulationService(
            ProtocolPresetRegistry presetRegistry,
            EthSimulatorProperties properties,
            TreasuryContextBuilder treasuryContextBuilder,
            Clock clock
    ) {
        this.presetRegistry = presetRegistry;
        this.properties = properties;
        this.treasuryContextBuilder = treasuryContextBuilder;
        this.clock = clock;
    }

    public SimulationResponse simulate(SimulationRequest request) {
        BigDecimal ethPriceUsd = properties.getStaticEthPriceUsd();
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
        BigDecimal deployYieldPct = UsdMath.bd(request.getDeployYieldPct());
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

        String protocol = request.getProtocol();
        double ethAmountDouble = ethAmount.doubleValue();
        double ethPriceDouble = ethPriceUsd.doubleValue();
        Instant generatedAt = clock.instant();

        List<ChartSpec> charts = new ArrayList<>();
        charts.add(ChartBuilders.yieldProjection(
                protocol,
                result.stablecoinDebtUsd(),
                result.annualStabilityFeeUsd(),
                deployYieldPct,
                years,
                compoundsPerYear,
                ethPriceDouble,
                ethAmountDouble,
                generatedAt
        ));
        charts.add(ChartBuilders.liquidationBand(
                protocol,
                ethPriceDouble,
                result.liquidationPriceUsd().doubleValue(),
                ethAmountDouble,
                result.stablecoinDebtUsd().doubleValue(),
                generatedAt
        ));
        charts.add(ChartBuilders.healthRatioSweep(
                protocol,
                ethAmount,
                result.stablecoinDebtUsd(),
                liquidationRatio,
                ethPriceUsd,
                generatedAt
        ));

        boolean treasuryEnabled = request.getTreasuryContextEnabled() == null
                || Boolean.TRUE.equals(request.getTreasuryContextEnabled());
        TreasuryContext treasuryContext = null;
        if (treasuryEnabled) {
            StablecoinReserveModel reserveModel = StablecoinReserveModel.fromKey(
                    request.getStablecoinReserveModel() != null
                            ? request.getStablecoinReserveModel()
                            : "usdc_style");
            validateTreasuryOverrides(request, reserveModel);
            treasuryContext = treasuryContextBuilder.build(
                    result.stablecoinDebtUsd(),
                    result.projectedNetYieldUsd(),
                    reserveModel,
                    toBigDecimalOrNull(request.getReserveInTreasuriesPct()),
                    toBigDecimalOrNull(request.getTbillApyPct()),
                    toBigDecimalOrNull(request.getSystemSupplyUsd()),
                    years
            );
            charts.add(ChartBuilders.treasuryContextChart(
                    treasuryContext.yourMint().impliedTreasuryBackingUsd(),
                    treasuryContext.yourMint().annualIssuerReserveYieldUsd(),
                    treasuryContext.personalComparison().yourDeFiProjectedNetYieldUsd(),
                    protocol,
                    result.stablecoinDebtUsd().doubleValue(),
                    generatedAt
            ));
        }

        Assumptions assumptions = new Assumptions(
                protocol,
                roundEthAmount(ethAmount),
                ethPriceDouble,
                "static",
                targetCollateralRatio.doubleValue(),
                liquidationRatio.doubleValue(),
                stabilityFeePct.doubleValue(),
                deployYieldPct.doubleValue(),
                years,
                compoundsPerYear,
                "linear_annualized_v1"
        );

        return new SimulationResponse(
                UUID.randomUUID(),
                result.collateralValueUsd(),
                result.stablecoinDebtUsd(),
                result.liquidationPriceUsd(),
                result.annualStabilityFeeUsd(),
                result.projectedGrossYieldUsd(),
                result.projectedNetYieldUsd(),
                result.healthRatio(),
                result.riskTier(),
                List.of(),
                assumptions,
                treasuryContext,
                charts
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
            BigDecimal ethFromRequest = UsdMath.bd(request.getEthAmount());
            BigDecimal collateralFromRequest = UsdMath.bd(request.getCollateralUsd());
            BigDecimal impliedCollateral = ethFromRequest.multiply(ethPriceUsd);
            BigDecimal tolerance = impliedCollateral.abs().multiply(COLLATERAL_TOLERANCE_RATE);
            BigDecimal diff = impliedCollateral.subtract(collateralFromRequest).abs();
            if (diff.compareTo(tolerance) > 0) {
                throw new ApiException(
                        "INVALID_SIMULATION_INPUT",
                        "ethAmount and collateralUsd disagree beyond 0.5% tolerance",
                        HttpStatus.BAD_REQUEST);
            }
            return ethFromRequest;
        }

        if (hasCollateral) {
            return UsdMath.bd(request.getCollateralUsd())
                    .divide(ethPriceUsd, 10, RoundingMode.HALF_UP);
        }

        return UsdMath.bd(request.getEthAmount());
    }

    private static void validateTreasuryOverrides(SimulationRequest request, StablecoinReserveModel reserveModel) {
        boolean hasReserveOverride = request.getReserveInTreasuriesPct() != null;
        boolean hasTbillOverride = request.getTbillApyPct() != null;

        if ((hasReserveOverride || hasTbillOverride) && reserveModel != StablecoinReserveModel.GENERIC) {
            throw new ApiException(
                    "INVALID_SIMULATION_INPUT",
                    "reserveInTreasuriesPct and tbillApyPct overrides are only allowed for generic reserve model",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private static BigDecimal overrideOrDefault(Double override, BigDecimal presetValue) {
        return override != null ? UsdMath.bd(override) : presetValue;
    }

    private static BigDecimal toBigDecimalOrNull(Double value) {
        return value != null ? UsdMath.bd(value) : null;
    }

    private static double roundEthAmount(BigDecimal ethAmount) {
        return ethAmount.setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}