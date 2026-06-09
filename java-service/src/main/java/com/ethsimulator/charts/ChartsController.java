package com.ethsimulator.charts;

import com.ethsimulator.charts.ChartModels.ChartSpec;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/charts")
public class ChartsController {

    private final ChartService chartService;

    public ChartsController(ChartService chartService) {
        this.chartService = chartService;
    }

    @GetMapping("/simulation-projection")
    public ChartSpec simulationProjection(@Valid @ModelAttribute ChartQueryParams params) {
        return chartService.simulationProjection(params);
    }

    @GetMapping("/liquidation-band")
    public ChartSpec liquidationBand(@Valid @ModelAttribute ChartQueryParams params) {
        return chartService.liquidationBand(params);
    }

    @GetMapping("/health-ratio")
    public ChartSpec healthRatio(@Valid @ModelAttribute ChartQueryParams params) {
        return chartService.healthRatioSweep(params);
    }
}