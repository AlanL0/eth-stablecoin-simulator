import type { ChartSpecV1 } from "@/lib/api";
import type { ReactNode } from "react";

type ChartPanelProps = {
  spec: ChartSpecV1;
  children: ReactNode;
};

export function ChartPanel({ spec, children }: ChartPanelProps) {
  const sourceSummary = spec.meta?.sources?.[0];
  return (
    <section
      className="rounded-xl border border-slate-700 bg-surface-card p-4 shadow-sm"
      aria-label={spec.title}
    >
      <header className="mb-3">
        <h3 className="text-lg font-semibold text-slate-100">{spec.title}</h3>
        {spec.subtitle ? (
          <p className="mt-1 text-sm text-slate-400">{spec.subtitle}</p>
        ) : null}
      </header>
      <div className="h-72 w-full">{children}</div>
      <footer className="mt-3 text-xs text-slate-500">
        {sourceSummary ? (
          <span>
            Source: {sourceSummary.source} ({sourceSummary.field}) ·{" "}
          </span>
        ) : null}
        <span>Generated {spec.generatedAt}</span>
      </footer>
    </section>
  );
}