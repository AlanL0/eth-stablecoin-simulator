import type { ChartContract } from "@/lib/api";
import type { ReactNode } from "react";

type ChartPanelProps = {
  spec: ChartContract;
  children: ReactNode;
};

export function ChartPanel({ spec, children }: ChartPanelProps) {
  const sourceSummary = spec.provenance?.sources?.[0];
  return (
    <section
      className="rounded-xl border border-slate-700 bg-surface-card p-4 shadow-sm"
      aria-label={spec.title}
    >
      <header className="mb-3">
        <h3 className="text-lg font-semibold text-slate-100">{spec.title}</h3>
        {spec.description ? (
          <p className="mt-1 text-sm text-slate-400">{spec.description}</p>
        ) : null}
        {spec.warnings?.length ? (
          <ul className="mt-2 text-xs text-amber-400">
            {spec.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        ) : null}
      </header>
      <div className="h-72 w-full">{children}</div>
      <footer className="mt-3 text-xs text-slate-500">
        {sourceSummary ? (
          <span>
            Source: {sourceSummary.source} ({sourceSummary.field}) ·{" "}
          </span>
        ) : null}
        <span>Generated {spec.provenance?.generatedAt}</span>
      </footer>
    </section>
  );
}