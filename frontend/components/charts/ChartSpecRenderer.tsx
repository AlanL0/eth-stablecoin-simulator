"use client";

import type { ChartSpecV1 } from "@/lib/api";
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

function hasRenderablePoints(spec: ChartSpecV1): boolean {
  return spec.series.some((series) => series.points.length > 0);
}

function toRows(spec: ChartSpecV1): ChartPointRow[] {
  const xValues = new Set<string | number>();
  for (const series of spec.series) {
    for (const point of series.points) {
      xValues.add(point.x as string | number);
    }
  }
  const rows: ChartPointRow[] = Array.from(xValues).map((x) => ({ x }));
  for (const series of spec.series) {
    for (const point of series.points) {
      const row = rows.find((entry) => entry.x === point.x);
      if (!row) continue;
      if (series.geometry === "band") {
        row[`${series.id}_y0`] = point.y0 ?? null;
        row[`${series.id}_y1`] = point.y1 ?? null;
      } else {
        row[series.id] = point.y ?? null;
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

function renderAnnotations(spec: ChartSpecV1) {
  return (spec.annotations ?? []).map((annotation) => {
    const color = severityColor(annotation.severity);
    if (annotation.kind === "horizontal_line" && typeof annotation.value === "number") {
      return (
        <ReferenceLine
          key={annotation.id}
          y={annotation.value}
          stroke={color}
          strokeDasharray="4 4"
          label={annotation.label}
        />
      );
    }
    if (annotation.kind === "vertical_line" && typeof annotation.value === "number") {
      return (
        <ReferenceLine
          key={annotation.id}
          x={annotation.value}
          stroke={color}
          strokeDasharray="4 4"
          label={annotation.label}
        />
      );
    }
    if (
      annotation.kind === "band" &&
      typeof annotation.value === "number" &&
      typeof annotation.valueEnd === "number"
    ) {
      return (
        <ReferenceArea
          key={annotation.id}
          y1={annotation.value}
          y2={annotation.valueEnd}
          fill={color}
          fillOpacity={0.12}
          label={annotation.label}
        />
      );
    }
    return null;
  });
}

function renderSeries(spec: ChartSpecV1) {
  return spec.series.map((series) => {
    const color = strokeForToken(series.style?.colorToken);
    const dash = strokeDash(series.style);
    if (series.geometry === "area") {
      return (
        <Area
          key={series.id}
          type="monotone"
          dataKey={series.id}
          name={series.name}
          stroke={color}
          fill={colorWithOpacity(series.style?.colorToken, series.style?.fillOpacity ?? 0.2)}
          strokeDasharray={dash}
        />
      );
    }
    if (series.geometry === "bar") {
      return (
        <Bar
          key={series.id}
          dataKey={series.id}
          name={series.name}
          fill={color}
          fillOpacity={series.style?.fillOpacity ?? 0.85}
        />
      );
    }
    if (series.geometry === "band") {
      return (
        <ReferenceArea
          key={series.id}
          y1={series.points[0]?.y0}
          y2={series.points[0]?.y1}
          fill={colorWithOpacity(series.style?.colorToken, series.style?.fillOpacity ?? 0.15)}
          label={series.name}
        />
      );
    }
    return (
      <Line
        key={series.id}
        type="monotone"
        dataKey={series.id}
        name={series.name}
        stroke={color}
        strokeDasharray={dash}
        dot={false}
      />
    );
  });
}

export function ChartSpecRenderer({ spec }: { spec: ChartSpecV1 }) {
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
  const showLegend = spec.legend?.show !== false && spec.legend?.position !== "hidden";

  return (
    <ChartPanel spec={spec}>
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={rows} margin={{ top: 8, right: 16, left: 8, bottom: 8 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
          <XAxis
            dataKey="x"
            type={isCategory ? "category" : "number"}
            domain={spec.xAxis.domain as [number, number] | undefined}
            tickFormatter={(value) => formatAxisValue(value, spec.xAxis.format)}
            stroke="#94a3b8"
          />
          <YAxis
            domain={spec.yAxis.domain as [number, number] | undefined}
            tickFormatter={(value) => formatAxisValue(value, spec.yAxis.format)}
            stroke="#94a3b8"
          />
          <Tooltip
            formatter={(value: number) => formatAxisValue(value, spec.yAxis.format)}
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
              <th key={series.id}>{series.name}</th>
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