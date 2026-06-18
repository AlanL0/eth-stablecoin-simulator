package com.ethsimulator.charts;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/charts")
@Tag(name = "Charts", description = "Deterministic ChartContract v2 builders")
public class ChartsController {

    private final ChartService chartService;

    public ChartsController(ChartService chartService) {
        this.chartService = chartService;
    }

    @GetMapping("/simulation-projection")
    @Operation(summary = "Yield projection chart", operationId = "chartSimulationProjection")
    @ApiResponse(responseCode = "200", description = "ChartContract v2",
            content = @Content(schema = @Schema(implementation = ChartContract.class)))
    public ChartContract simulationProjection(@Valid @ModelAttribute ChartQueryParams params) {
        return chartService.simulationProjection(params);
    }

    @GetMapping("/liquidation-band")
    @Operation(summary = "Collateral recovery threshold band chart", operationId = "chartLiquidationBand")
    @ApiResponse(responseCode = "200", description = "ChartContract v2",
            content = @Content(schema = @Schema(implementation = ChartContract.class)))
    public ChartContract liquidationBand(@Valid @ModelAttribute ChartQueryParams params) {
        return chartService.liquidationBand(params);
    }

    @GetMapping("/health-ratio")
    @Operation(summary = "Collateralization risk margin sweep chart", operationId = "chartHealthRatio")
    @ApiResponse(responseCode = "200", description = "ChartContract v2",
            content = @Content(schema = @Schema(implementation = ChartContract.class)))
    public ChartContract healthRatio(@Valid @ModelAttribute ChartQueryParams params) {
        return chartService.healthRatioSweep(params);
    }

    @GetMapping("/protocol-rates")
    @Operation(summary = "Protocol return comparison chart", operationId = "chartProtocolRates")
    @ApiResponse(responseCode = "200", description = "ChartContract v2",
            content = @Content(schema = @Schema(implementation = ChartContract.class)))
    public ChartContract protocolRates(@RequestParam String asset) {
        return chartService.protocolRates(asset);
    }

    @GetMapping("/eth-price-history")
    @Operation(summary = "ETH/USD price history chart", operationId = "chartEthPriceHistory")
    @ApiResponse(responseCode = "200", description = "ChartContract v2",
            content = @Content(schema = @Schema(implementation = ChartContract.class)))
    public ChartContract ethPriceHistory() {
        return chartService.ethPriceHistory();
    }
}