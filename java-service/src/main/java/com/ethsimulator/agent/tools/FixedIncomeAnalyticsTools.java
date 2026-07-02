package com.ethsimulator.agent.tools;

import com.ethsimulator.api.dto.SimulationRequest;
import com.ethsimulator.api.dto.SimulationResponse;
import com.ethsimulator.charts.ChartContract;
import com.ethsimulator.charts.ChartQueryParams;
import com.ethsimulator.charts.ChartService;
import com.ethsimulator.api.EthPriceController.EthPriceResponse;
import com.ethsimulator.market.EthPriceQuote;
import com.ethsimulator.market.EthPriceService;
import com.ethsimulator.market.YieldSnapshotResponse;
import com.ethsimulator.market.YieldService;
import com.ethsimulator.persistence.RateObservation;
import com.ethsimulator.persistence.RateReadModelRepository;
import com.ethsimulator.service.SimulationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FixedIncomeAnalyticsTools {

    private final SimulationService simulationService;
    private final YieldService yieldService;
    private final ChartService chartService;
    private final EthPriceService ethPriceService;
    private final ObjectProvider<RateReadModelRepository> rateReadModelRepository;

    public FixedIncomeAnalyticsTools(
            SimulationService simulationService,
            YieldService yieldService,
            ChartService chartService,
            EthPriceService ethPriceService,
            ObjectProvider<RateReadModelRepository> rateReadModelRepository
    ) {
        this.simulationService = simulationService;
        this.yieldService = yieldService;
        this.chartService = chartService;
        this.ethPriceService = ethPriceService;
        this.rateReadModelRepository = rateReadModelRepository;
    }

    @Tool(description = """
            Run a deterministic collateralized stablecoin simulation. Use when the user asks how much they can borrow,
            health ratio, liquidation price, or projected net yield. Values returned are authoritative Java outputs.""")
    public SimulationResponse runSimulation(SimulationToolRequest request) {
        SimulationRequest simulationRequest = new SimulationRequest();
        simulationRequest.setEthAmount(request.resolvedEthAmount());
        simulationRequest.setProtocol(request.resolvedProtocol());
        simulationRequest.setDeployYieldPct(request.resolvedDeployYieldPct());
        simulationRequest.setYears(request.resolvedYears());
        simulationRequest.setCompoundsPerYear(request.resolvedCompoundsPerYear());
        simulationRequest.setTreasuryContextEnabled(false);
        return simulationService.simulate(simulationRequest);
    }

    @Tool(description = """
            Fetch latest ingested or seed-fallback yields for a stablecoin asset (USDC, USDT, DAI, PYUSD).
            Use when comparing borrowing or savings rates across venues.""")
    public YieldSnapshotResponse getLatestYields(
            @ToolParam(description = "Stablecoin asset symbol, e.g. USDC") String asset
    ) {
        return yieldService.getYields(asset == null ? "USDC" : asset);
    }

    // TODO(T39): wrap tool outputs before exposing to the model; avoid leaking persistence entities.
    @Tool(description = """
            Fetch rate history for a protocol/product/side triple from the ingested read model.
            Returns an empty list when persistence is unavailable.""")
    public List<RateObservation> getRateHistory(
            @ToolParam(description = "Protocol id, e.g. aave") String protocol,
            @ToolParam(description = "Product symbol, e.g. GHO") String product,
            @ToolParam(description = "Rate side: borrow or savings") String side,
            @ToolParam(description = "Maximum rows (1-50)", required = false) Integer limit
    ) {
        RateReadModelRepository repository = rateReadModelRepository.getIfAvailable();
        if (repository == null) {
            return List.of();
        }
        int resolvedLimit = limit == null ? 10 : Math.min(50, Math.max(1, limit));
        return repository.findHistory(protocol, product, side, resolvedLimit);
    }

    @Tool(description = "Fetch the current authoritative ETH/USD spot quote from Java services.")
    public EthPriceResponse getEthSpotPrice() {
        EthPriceQuote quote = ethPriceService.currentPrice();
        return new EthPriceResponse(
                quote.priceUsd(),
                quote.source().name().toLowerCase(),
                quote.observedAt().toString(),
                quote.stale(),
                quote.degraded()
        );
    }

    @Tool(description = "Build a protocol return comparison chart for an asset using ChartContract v2.")
    public ChartContract buildProtocolRatesChart(
            @ToolParam(description = "Stablecoin asset symbol, e.g. USDC") String asset
    ) {
        return chartService.protocolRates(asset == null ? "USDC" : asset);
    }

    @Tool(description = "Build a collateralization risk margin sweep chart for a simulation preset.")
    public ChartContract buildHealthRatioChart(SimulationToolRequest request) {
        return chartService.healthRatioSweep(toChartParams(request));
    }

    @Tool(description = "Build a collateral recovery threshold band chart for a simulation preset.")
    public ChartContract buildLiquidationBandChart(SimulationToolRequest request) {
        return chartService.liquidationBand(toChartParams(request));
    }

    @Tool(description = "Build the ETH/USD price history chart from deterministic seed history.")
    public ChartContract getEthPriceHistoryChart() {
        return chartService.ethPriceHistory();
    }

    private ChartQueryParams toChartParams(SimulationToolRequest request) {
        ChartQueryParams params = new ChartQueryParams();
        params.setEthAmount(request.resolvedEthAmount());
        params.setProtocol(request.resolvedProtocol());
        params.setDeployYieldPct(request.resolvedDeployYieldPct());
        params.setYears(request.resolvedYears());
        params.setCompoundsPerYear(request.resolvedCompoundsPerYear());
        return params;
    }
}