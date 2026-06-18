package com.ethsimulator.charts;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Authoritative chart contract emitted by Java for dumb frontend rendering.
 * <p>
 * {@code plotValue} fields are server-rounded JSON numbers for coordinates only.
 * {@code displayValue} fields are exact authoritative decimal strings for labels and tooltips.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ChartContract", description = "ChartContract v2 — Java-owned chart payload for Recharts rendering")
public record ChartContract(
        @NotBlank
        @Schema(description = "Contract schema version", example = "2.0", allowableValues = {"2.0"})
        String schemaVersion,
        @NotBlank
        @Schema(description = "Stable chart identifier", example = "simulation_yield_projection")
        String chartId,
        @NotBlank
        @Schema(description = "Human-readable chart title")
        String title,
        @Schema(description = "Optional chart description / subtitle")
        String description,
        @NotNull @Valid
        @Schema(description = "X-axis definition")
        ChartAxis xAxis,
        @NotNull @Valid
        @Schema(description = "Y-axis definition")
        ChartAxis yAxis,
        @NotEmpty @Valid
        @Schema(description = "Renderable series")
        List<ChartSeries> series,
        @Schema(description = "Reference lines, bands, and markers")
        List<ChartAnnotation> annotations,
        @Schema(description = "Model inputs and context as exact decimal strings where applicable")
        Map<String, String> assumptions,
        @Schema(description = "Non-fatal chart warnings (stale data, degraded sources)")
        List<String> warnings,
        @NotNull @Valid
        @Schema(description = "Builder identity, timestamps, and data provenance")
        ChartProvenance provenance
) {
    public static final String SCHEMA_VERSION = "2.0";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "ChartAxis")
    public record ChartAxis(
            @NotBlank
            @Schema(allowableValues = {"linear", "time", "category"})
            String type,
            @NotBlank
            String label,
            @Schema(description = "Axis unit token", example = "usd")
            String unit,
            @Schema(description = "Display format token", example = "usd")
            String format,
            @Schema(description = "Plot domain [min, max] as JSON numbers")
            List<PlotNumber> domain,
            @Schema(description = "Suggested tick count")
            Integer tickCount
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "ChartSeries")
    public record ChartSeries(
            @NotBlank
            String id,
            @NotBlank
            @Schema(description = "TradFi display label for the series")
            String label,
            @NotBlank
            @Schema(description = "Series unit token", example = "usd")
            String unit,
            @Valid
            ChartSeriesStyle style,
            @NotEmpty
            List<@Valid DataPoint> data
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "DataPoint")
    public record DataPoint(
            @NotNull
            @Schema(description = "X coordinate (category label, month index, timestamp token, etc.)")
            Object x,
            @NotNull
            @JsonSerialize(using = PlotValueSerializer.class)
            @Schema(description = "Server-rounded Y coordinate for plotting", implementation = Double.class, example = "216.02")
            BigDecimal plotValue,
            @NotBlank
            @Schema(description = "Exact authoritative decimal string for labels/tooltips", example = "216.02")
            String displayValue,
            @Schema(description = "Optional point label")
            String label,
            @Schema(description = "Band upper bounds and auxiliary presentation metadata")
            Map<String, Object> metadata
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "ChartSeriesStyle")
    public record ChartSeriesStyle(
            @Schema(allowableValues = {"line", "area", "band", "bar"})
            String geometry,
            @Schema(allowableValues = {"primary", "secondary", "positive", "negative", "warning", "neutral"})
            String colorToken,
            @Schema(allowableValues = {"solid", "dashed", "dotted"})
            String strokeDash,
            @JsonSerialize(using = PlotValueSerializer.class)
            @Schema(description = "Fill opacity for area/band geometry", implementation = Double.class, example = "0.2")
            BigDecimal fillOpacity
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "ChartAnnotation")
    public record ChartAnnotation(
            @NotBlank
            String id,
            @NotBlank
            @Schema(allowableValues = {"horizontal_line", "vertical_line", "band", "point", "label"})
            String kind,
            @Schema(allowableValues = {"x", "y"})
            String axis,
            @JsonSerialize(using = PlotValueSerializer.class)
            @Schema(description = "Primary plot coordinate", implementation = Double.class)
            BigDecimal plotValue,
            @Schema(description = "Exact display string for primary coordinate")
            String displayValue,
            @JsonSerialize(using = PlotValueSerializer.class)
            @Schema(description = "Secondary plot coordinate (band end)", implementation = Double.class)
            BigDecimal plotValueEnd,
            @Schema(description = "Exact display string for secondary coordinate")
            String displayValueEnd,
            String label,
            @Schema(allowableValues = {"info", "low", "medium", "high"})
            String severity
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "ChartProvenance")
    public record ChartProvenance(
            @NotBlank
            @Schema(description = "Builder identifier", example = "java-service/simulation-chart-builder")
            String builder,
            @NotBlank
            @Schema(description = "ISO-8601 generation timestamp", format = "date-time")
            String generatedAt,
            @Schema(description = "Methodology or model identifier")
            String methodology,
            @NotEmpty
            List<@Valid ChartSource> sources
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "ChartSource")
    public record ChartSource(
            @NotBlank
            String field,
            @NotBlank
            String source,
            @NotBlank
            String observedAt,
            boolean stale
    ) {
    }
}