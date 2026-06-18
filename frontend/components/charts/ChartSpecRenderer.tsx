"use client";

import type { ChartContract } from "@/lib/api";
import { formatAxisValue } from "@/lib/format";
import {
  Area,
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ReferenceArea,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ChartPanel } from "./ChartPanel";
import { colorWithOpacity, severityColor, strokeForToken } from "./colorTokens";

type ChartPointRow = Record<string, string | number | null>;

function plotDomain(domain?: number[]): [number, number] | undefined {
  if (!domain || domain.length < 2) return undefined;
  return [domain[0], domain[1]];
}

function hasRenderablePoints(spec: ChartContract): boolean {
  return spec.series.some((series) => series.data.length > 0);
}

function toRows(spec: ChartContract): ChartPointRow[] {
  const xValues = new Set<string | number>();
  for (const series of spec.series) {
    for (const point of series.data) {
      xValues.add(point.x as string | number);
    }
  }
  const rows: ChartPointRow[] = Array.from(xValues).map((x) => ({ x }));
  for (const series of spec.series) {
    const geometry = series.style?.geometry ?? "line";
    for (const point of series.data) {
      const row = rows.find((entry) => entry.x === point.x);
      if (!row) continue;
      if (geometry === "band") {
        const metadata = point.metadata as { plotValueEnd?: number } | undefined;
        row[`${series.id}_y0`] = Number(point.plotValue) ?? null;
        row[`${series.id}_y1`] = metadata?.plotValueEnd ?? null;
      } else {
        row[series.id] = Number(point.plotValue) ?? null;
      }
    }
  }
  return rows;
}

function strokeDash(style?: { strokeDash?: string }): string | undefined {
  switch (style?.strokeDash) {
    case "dashed":
      return "4 4";
    case "dotted":
      return "2 2";
    default:
      return undefined;
  }
}

function renderAnnotations(spec: ChartContract) {
  return (spec.annotations ?? []).map((annotation) => {
    const color = severityColor(annotation.severity);
    if (annotation.kind === "horizontal_line" && annotation.plotValue != null) {
      return (
        <ReferenceLine
          key={annotation.id}
          y={Number(annotation.plotValue)}
          stroke={color}
          strokeDasharray="4 4"
          label={annotation.label}
        />
      );
    }
    if (annotation.kind === "vertical_line" && annotation.plotValue != null) {
      return (
        <ReferenceLine
          key={annotation.id}
          x={Number(annotation.plotValue)}
          stroke={color}
          strokeDasharray="4 4"
          label={annotation.label}
        />
      );
    }
    if (annotation.kind === "band" && annotation.plotValue != null && annotation.plotValueEnd != null) {
      return (
        <ReferenceArea
          key={annotation.id}
          y1={Number(annotation.plotValue)}
          y2={Number(annotation.plotValueEnd)}
          fill={color}
          fillOpacity={0.12}
          label={annotation.label}
        />
      );
    }
    return null;
  });
}

function renderSeries(spec: ChartContract) {
  return spec.series.map((series) => {
    const geometry = series.style?.geometry ?? "line";
    const color = strokeForToken(series.style?.colorToken);
    const dash = strokeDash(series.style);
    if (geometry === "area") {
      return (
        <Area
          key={series.id}
          type="monotone"
          dataKey={series.id}
          name={series.label}
          stroke={color}
          fill={colorWithOpacity(
            series.style?.colorToken,
            Number(series.style?.fillOpacity ?? 0.2),
          )}
          strokeDasharray={dash}
        />
      );
    }
    if (geometry === "bar") {
      return (
        <Bar
          key={series.id}
          dataKey={series.id}
          name={series.label}
          fill={color}
          fillOpacity={Number(series.style?.fillOpacity ?? 0.85)}
        />
      );
    }
    if (geometry === "band") {
      const point = series.data[0];
      const metadata = point?.metadata as { plotValueEnd?: number } | undefined;
      return (
        <ReferenceArea
          key={series.id}
          y1={point ? Number(point.plotValue) : undefined}
          y2={metadata?.plotValueEnd}
          fill={colorWithOpacity(series.style?.colorToken, Number(series.style?.fillOpacity ?? 0.15))}
          label={series.label}
        />
      );
    }
    return (
      <Line
        key={series.id}
        type="monotone"
        dataKey={series.id}
        name={series.label}
        stroke={color}
        strokeDasharray={dash}
        dot={false}
      />
    );
  });
}

export function ChartSpecRenderer({ spec }: { spec: ChartContract }) {
  if (!hasRenderablePoints(spec)) {
    return (
      <ChartPanel spec={spec}>
        <p className="flex h-full items-center justify-center text-sm text-slate-400">
          No chart data from service
        </p>
      </ChartPanel>
    );
  }

  const rows = toRows(spec);
  const isCategory = spec.xAxis.type === "category";
  const chartType = spec.assumptions?.chartType;
  const showLegend = chartType === "composed" || chartType === "bar";

  return (
    <ChartPanel spec={spec}>
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={rows} margin={{ top: 8, right: 16, left: 8, bottom: 8 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
          <XAxis
            dataKey="x"
            type={isCategory ? "category" : "number"}
            domain={plotDomain(spec.xAxis.domain)}
            tickFormatter={(value) => formatAxisValue(value, spec.xAxis.format)}
            stroke="#94a3b8"
          />
          <YAxis
            domain={plotDomain(spec.yAxis.domain)}
            tickFormatter={(value) => formatAxisValue(value, spec.yAxis.format)}
            stroke="#94a3b8"
          />
          <Tooltip
            formatter={(value: number, _name, item) => {
              const series = spec.series.find((entry) => entry.id === String(item.dataKey));
              const point = series?.data.find((entry) => Number(entry.plotValue) === value);
              return point?.displayValue ?? formatAxisValue(value, spec.yAxis.format);
            }}
            labelFormatter={(label) => formatAxisValue(label, spec.xAxis.format)}
          />
          {showLegend ? <Legend /> : null}
          {renderAnnotations(spec)}
          {renderSeries(spec)}
        </ComposedChart>
      </ResponsiveContainer>
      <table className="sr-only">
        <caption>{spec.title}</caption>
        <thead>
          <tr>
            <th>{spec.xAxis.label}</th>
            {spec.series.map((series) => (
              <th key={series.id}>{series.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={String(row.x)}>
              <td>{String(row.x)}</td>
              {spec.series.map((series) => (
                <td key={series.id}>{String(row[series.id] ?? "")}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </ChartPanel>
  );
}