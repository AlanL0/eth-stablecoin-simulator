package com.ethsimulator.service;

import com.ethsimulator.api.dto.SimulationRequest;
import com.ethsimulator.api.dto.SimulationResponse;
import com.ethsimulator.api.dto.SimulationResponse.Assumptions;
import com.ethsimulator.api.error.ApiException;
import com.ethsimulator.charts.ChartBuilders;
import com.ethsimulator.charts.ChartModels.ChartSpec;
import com.ethsimulator.charts.LiquidationBandChartBuilder;
import com.ethsimulator.service.SimulationInputResolver.ResolvedSimulation;
import com.ethsimulator.treasury.StablecoinReserveModel;
import com.ethsimulator.treasury.TreasuryContext;
import com.ethsimulator.treasury.TreasuryContextBuilder;
import com.ethsimulator.util.UsdMath;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SimulationService {

    private final SimulationInputResolver simulationInputResolver;
    private final TreasuryContextBuilder treasuryContextBuilder;
    private final LiquidationBandChartBuilder liquidationBandChartBuilder;
    private final Clock clock;

    public SimulationService(
            SimulationInputResolver simulationInputResolver,
            TreasuryContextBuilder treasuryContextBuilder,
            LiquidationBandChartBuilder liquidationBandChartBuilder,
            Clock clock
    ) {
        this.simulationInputResolver = simulationInputResolver;
        this.treasuryContextBuilder = treasuryContextBuilder;
        this.liquidationBandChartBuilder = liquidationBandChartBuilder;
        this.clock = clock;
    }

    public SimulationResponse simulate(SimulationRequest request) {
        ResolvedSimulation resolved = simulationInputResolver.resolve(request);
        Instant generatedAt = clock.instant();

        List<ChartSpec> charts = new ArrayList<>();
        charts.add(ChartBuilders.yieldProjection(
                resolved.protocol(),
                resolved.result().stablecoinDebtUsd(),
                resolved.result().annualStabilityFeeUsd(),
                resolved.deployYieldPct(),
                resolved.years(),
                resolved.compoundsPerYear(),
                resolved.ethPriceDouble(),
                resolved.ethAmountDouble(),
                resolved.ethPriceSourceKey(),
                resolved.ethPrice().stale(),
                generatedAt
        ));
        charts.add(liquidationBandChartBuilder.build(resolved, generatedAt));
        charts.add(ChartBuilders.healthRatioSweep(
                resolved.protocol(),
                resolved.ethAmount(),
                resolved.result().stablecoinDebtUsd(),
                resolved.liquidationRatio(),
                resolved.ethPrice().priceUsd(),
                resolved.ethPriceSourceKey(),
                resolved.ethPrice().stale(),
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
                    resolved.result().stablecoinDebtUsd(),
                    resolved.result().projectedNetYieldUsd(),
                    reserveModel,
                    toBigDecimalOrNull(request.getReserveInTreasuriesPct()),
                    toBigDecimalOrNull(request.getTbillApyPct()),
                    toBigDecimalOrNull(request.getSystemSupplyUsd()),
                    resolved.years()
            );
            charts.add(ChartBuilders.treasuryContextChart(
                    treasuryContext.yourMint().impliedTreasuryBackingUsd(),
                    treasuryContext.yourMint().annualIssuerReserveYieldUsd(),
                    treasuryContext.personalComparison().yourDeFiProjectedNetYieldUsd(),
                    resolved.protocol(),
                    resolved.result().stablecoinDebtUsd().doubleValue(),
                    generatedAt
            ));
        }

        Assumptions assumptions = new Assumptions(
                resolved.protocol(),
                resolved.roundEthAmount(),
                resolved.ethPriceDouble(),
                resolved.ethPriceSourceKey(),
                resolved.targetCollateralRatio().doubleValue(),
                resolved.liquidationRatio().doubleValue(),
                resolved.stabilityFeePct().doubleValue(),
                resolved.deployYieldPct().doubleValue(),
                resolved.years(),
                resolved.compoundsPerYear(),
                "linear_annualized_v1"
        );

        var result = resolved.result();
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

    private static BigDecimal toBigDecimalOrNull(Double value) {
        return value != null ? UsdMath.bd(value) : null;
    }

}