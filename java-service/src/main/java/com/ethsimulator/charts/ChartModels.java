package com.ethsimulator.charts;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public final class ChartModels {

    private ChartModels() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChartSpec(
            String schemaVersion,
            String chartId,
            String chartType,
            String title,
            String subtitle,
            Axis xAxis,
            Axis yAxis,
            List<Series> series,
            List<Annotation> annotations,
            Legend legend,
            Meta meta,
            String source,
            String generatedAt
    ) {
    }

    public record Axis(String type, String label, String unit, String format, List<Double> domain, Integer tickCount) {
    }

    public record Series(String id, String name, String geometry, List<Point> points, SeriesStyle style) {
    }

    public record Point(Object x, Double y, Double y0, Double y1, String label) {
        public static Point ofXy(Object x, double y) {
            return new Point(x, y, null, null, null);
        }

        public static Point band(Object x, double y0, double y1) {
            return new Point(x, null, y0, y1, null);
        }
    }

    public record SeriesStyle(String colorToken, String strokeDash, Double fillOpacity) {
    }

    public record Annotation(String id, String kind, String axis, Object value, Object valueEnd, String label, String severity) {
    }

    public record Legend(String position, Boolean show) {
    }

    public record Meta(String simulationId, String protocol, Double ethPriceUsd, Double ethAmount,
                       Double stablecoinDebtUsd, List<Source> sources) {
    }

    public record Source(String field, String source, String observedAt, Boolean stale) {
    }
}