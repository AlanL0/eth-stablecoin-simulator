export function formatUsd(value: number | undefined | null): string {
  if (value == null || Number.isNaN(value)) return "—";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(value);
}

export function formatNumber(value: number | undefined | null, digits = 2): string {
  if (value == null || Number.isNaN(value)) return "—";
  return value.toFixed(digits);
}

export function formatPercent(value: number | undefined | null): string {
  if (value == null || Number.isNaN(value)) return "—";
  return `${value.toFixed(2)}%`;
}

export function formatAxisValue(
  value: number | string,
  format?: string,
): string {
  if (typeof value === "string") return value;
  switch (format) {
    case "usd":
      return formatUsd(value);
    case "percent":
      return formatPercent(value);
    case "month_index":
      return `M${Math.round(value)}`;
    default:
      return formatNumber(value);
  }
}