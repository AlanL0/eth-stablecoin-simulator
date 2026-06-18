"use client";

import { ChartSpecRenderer } from "@/components/charts/ChartSpecRenderer";
import type { ChartContract, RecommendYieldResponse, SimulationResponse } from "@/lib/api";
import { recommendYield } from "@/lib/api";
import { useState } from "react";

type AiPanelProps = {
  simulationResult: SimulationResponse | null;
};

export function AiPanel({ simulationResult }: AiPanelProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<RecommendYieldResponse | null>(null);

  const handleAsk = async () => {
    if (!simulationResult) return;
    setLoading(true);
    setError(null);
    try {
      const result = await recommendYield({
        simulationResult: simulationResult as unknown as Record<string, unknown>,
        riskPreference: "balanced",
        message: "Explain this simulation and suggest yield deployment options.",
      });
      setResponse(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "AI request failed");
      setResponse(null);
    } finally {
      setLoading(false);
    }
  };

  const escalated = response?.feedback?.some((entry) => entry.backlogEscalated) ?? false;
  const chartSpecs = (response?.chartSpecs ?? []) as ChartContract[];

  return (
    <section
      className="rounded-xl border border-slate-700 bg-surface-card p-4"
      aria-label="AI explanation"
    >
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-lg font-semibold text-slate-100">AI explanation (optional)</h3>
        <button
          type="button"
          onClick={handleAsk}
          disabled={!simulationResult || loading}
          className="rounded bg-slate-700 px-3 py-1.5 text-sm text-white hover:bg-slate-600 disabled:opacity-50"
        >
          {loading ? "Thinking…" : "Ask AI"}
        </button>
      </div>

      {!simulationResult ? (
        <p className="mt-2 text-sm text-slate-400">Run a simulation to enable AI insights.</p>
      ) : null}

      {error ? (
        <p className="mt-3 rounded border border-rose-800 bg-rose-950/40 p-3 text-sm text-rose-200">
          AI unavailable: {error}. Simulation charts remain visible above.
        </p>
      ) : null}

      {response ? (
        <div className="mt-3 space-y-3 text-sm text-slate-200">
          <p>{response.summary}</p>
          {response.recommendations?.length ? (
            <div>
              <p className="font-medium">Recommendations</p>
              <ul className="mt-1 list-disc pl-5">
                {response.recommendations.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {escalated ? (
            <p className="rounded border border-amber-700 bg-amber-950/30 p-2 text-amber-100">
              Chart request escalated to backlog — we will prioritize based on demand.
            </p>
          ) : null}
          {chartSpecs.map((spec) => (
            <ChartSpecRenderer key={spec.chartId} spec={spec} />
          ))}
        </div>
      ) : null}
    </section>
  );
}