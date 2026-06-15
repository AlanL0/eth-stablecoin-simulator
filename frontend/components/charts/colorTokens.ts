export type ColorToken =
  | "primary"
  | "secondary"
  | "positive"
  | "negative"
  | "warning"
  | "neutral";

const STROKE: Record<ColorToken, string> = {
  primary: "#3b82f6",
  secondary: "#64748b",
  positive: "#10b981",
  negative: "#f43f5e",
  warning: "#f59e0b",
  neutral: "#6b7280",
};

const FILL: Record<ColorToken, string> = {
  primary: "rgba(59, 130, 246, 0.2)",
  secondary: "rgba(100, 116, 139, 0.2)",
  positive: "rgba(16, 185, 129, 0.2)",
  negative: "rgba(244, 63, 94, 0.2)",
  warning: "rgba(245, 158, 11, 0.2)",
  neutral: "rgba(107, 114, 128, 0.2)",
};

const SEVERITY: Record<string, string> = {
  info: "#3b82f6",
  low: "#6b7280",
  medium: "#f59e0b",
  high: "#ef4444",
};

export function strokeForToken(token?: string): string {
  return STROKE[(token as ColorToken) ?? "primary"] ?? STROKE.primary;
}

export function fillForToken(token?: string, opacity = 0.2): string {
  const base = FILL[(token as ColorToken) ?? "primary"] ?? FILL.primary;
  if (opacity === 0.2) return base;
  const color = strokeForToken(token);
  return color.replace("rgb", "rgba").replace(")", `, ${opacity})`).replace("#", "");
}

export function colorWithOpacity(token: string | undefined, opacity?: number): string {
  const color = strokeForToken(token);
  const alpha = opacity ?? 0.2;
  const hex = color.replace("#", "");
  const r = parseInt(hex.slice(0, 2), 16);
  const g = parseInt(hex.slice(2, 4), 16);
  const b = parseInt(hex.slice(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

export function severityColor(severity?: string): string {
  return SEVERITY[severity ?? "info"] ?? SEVERITY.info;
}