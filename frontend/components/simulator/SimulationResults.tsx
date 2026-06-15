import type { SimulationResponse } from "@/lib/api";
import { formatNumber, formatUsd } from "@/lib/format";

type SimulationResultsProps = {
  result: SimulationResponse;
};

const RISK_COLORS: Record<string, string> = {
  LOW: "text-emerald-400",
  MEDIUM: "text-amber-400",
  HIGH: "text-rose-400",
};

export function SimulationResults({ result }: SimulationResultsProps) {
  const metrics = [
    { label: "Collateral value", value: formatUsd(result.collateralValueUsd) },
    { label: "Stablecoin debt", value: formatUsd(result.stablecoinDebtUsd) },
    { label: "Liquidation price", value: formatUsd(result.liquidationPriceUsd) },
    { label: "Health ratio", value: formatNumber(result.healthRatio) },
    { label: "Projected net yield", value: formatUsd(result.projectedNetYieldUsd) },
    { label: "Annual stability fee", value: formatUsd(result.annualStabilityFeeUsd) },
  ];

  return (
    <section className="space-y-4" aria-label="Simulation results">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {metrics.map((metric) => (
          <article
            key={metric.label}
            className="rounded-lg border border-slate-700 bg-slate-900/60 p-3"
          >
            <p className="text-xs uppercase tracking-wide text-slate-400">{metric.label}</p>
            <p className="mt-1 text-lg font-semibold text-slate-100">{metric.value}</p>
          </article>
        ))}
        <article className="rounded-lg border border-slate-700 bg-slate-900/60 p-3">
          <p className="text-xs uppercase tracking-wide text-slate-400">Risk tier</p>
          <p className={`mt-1 text-lg font-semibold ${RISK_COLORS[result.riskTier ?? "MEDIUM"]}`}>
            {result.riskTier}
          </p>
        </article>
      </div>

      {result.warnings && result.warnings.length > 0 ? (
        <div className="rounded-lg border border-amber-700/60 bg-amber-950/30 p-3 text-sm text-amber-200">
          <p className="font-medium">Warnings</p>
          <ul className="mt-1 list-disc pl-5">
            {result.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {result.assumptions ? (
        <details className="rounded-lg border border-slate-700 bg-slate-900/40 p-3 text-sm text-slate-300">
          <summary className="cursor-pointer font-medium text-slate-200">Assumptions & sources</summary>
          <dl className="mt-2 grid gap-2 sm:grid-cols-2">
            {Object.entries(result.assumptions).map(([key, value]) => (
              <div key={key}>
                <dt className="text-xs uppercase text-slate-500">{key}</dt>
                <dd>{String(value)}</dd>
              </div>
            ))}
          </dl>
        </details>
      ) : null}
    </section>
  );
}