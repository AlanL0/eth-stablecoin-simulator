package com.ethsimulator.charts;

import com.ethsimulator.util.FinancialMath;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.ethsimulator.charts.ChartContract.DataPoint;

final class ChartPoints {

    private ChartPoints() {
    }

    static DataPoint xy(Object x, BigDecimal exact, int plotScale) {
        BigDecimal plot = exact.setScale(plotScale, FinancialMath.INTERMEDIATE.getRoundingMode());
        return new DataPoint(x, plot, exact.toPlainString(), null, null);
    }

    static DataPoint xyUsd(Object x, BigDecimal exactUsd) {
        BigDecimal scaled = FinancialMath.scaleUsd(exactUsd);
        return new DataPoint(x, scaled, scaled.toPlainString(), null, null);
    }

    static DataPoint xyUsdPreserveExact(Object x, BigDecimal exactUsd) {
        BigDecimal plot = FinancialMath.scaleUsd(exactUsd);
        return new DataPoint(x, plot, exactUsd.toPlainString(), null, null);
    }

    static DataPoint xyRatio(Object x, BigDecimal exactRatio) {
        return xy(x, FinancialMath.scaleRatio(exactRatio), FinancialMath.RATIO_SCALE);
    }

    static DataPoint band(Object x, BigDecimal lowerExact, BigDecimal upperExact) {
        BigDecimal lowerPlot = FinancialMath.scaleUsd(lowerExact);
        BigDecimal upperPlot = FinancialMath.scaleUsd(upperExact);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("plotValueEnd", upperPlot);
        metadata.put("displayValueEnd", upperExact.toPlainString());
        metadata.put("geometry", "band");
        return new DataPoint(x, lowerPlot, lowerExact.toPlainString(), null, metadata);
    }

    static ChartContract.ChartAnnotation annotation(
            String id,
            String kind,
            String axis,
            BigDecimal exact,
            int plotScale,
            String label,
            String severity
    ) {
        BigDecimal plot = exact.setScale(plotScale, FinancialMath.INTERMEDIATE.getRoundingMode());
        return new ChartContract.ChartAnnotation(
                id,
                kind,
                axis,
                plot,
                exact.toPlainString(),
                null,
                null,
                label,
                severity
        );
    }

    static ChartContract.ChartAnnotation annotationBand(
            String id,
            String axis,
            BigDecimal lowerExact,
            BigDecimal upperExact,
            int plotScale,
            String label,
            String severity
    ) {
        BigDecimal lowerPlot = lowerExact.setScale(plotScale, FinancialMath.INTERMEDIATE.getRoundingMode());
        BigDecimal upperPlot = upperExact.setScale(plotScale, FinancialMath.INTERMEDIATE.getRoundingMode());
        return new ChartContract.ChartAnnotation(
                id,
                "band",
                axis,
                lowerPlot,
                lowerExact.toPlainString(),
                upperPlot,
                upperExact.toPlainString(),
                label,
                severity
        );
    }
}