"use client";

import { AiPanel } from "@/components/ai/AiPanel";
import { ChartSpecRenderer } from "@/components/charts/ChartSpecRenderer";
import { SimulationResults } from "@/components/simulator/SimulationResults";
import { SimulatorForm, type SimulatorFormValues } from "@/components/simulator/SimulatorForm";
import { TreasuryEducationPanel } from "@/components/treasury/TreasuryEducationPanel";
import { trackEvent } from "@/lib/analytics";
import { getEthPrice, runSimulation, type SimulationResponse } from "@/lib/api";
import { getCurrentUser, isSupabaseConfigured, saveSimulation } from "@/lib/supabase";
import { useEffect, useState } from "react";

function toRequest(values: SimulatorFormValues, ethPriceUsd: number) {
  const base = {
    protocol: values.protocol,
    targetCollateralRatio: values.targetCollateralRatio,
    liquidationRatio: values.liquidationRatio,
    stabilityFeePct: values.stabilityFeePct,
    deployYieldPct: values.deployYieldPct,
    years: values.years,
    compoundsPerYear: values.compoundsPerYear,
    ethPriceUsd,
    treasuryContextEnabled: true,
    stablecoinReserveModel: "usdc_style",
  };
  if (values.collateralMode === "eth") {
    return { ...base, ethAmount: values.ethAmount };
  }
  return { ...base, collateralUsd: values.collateralUsd };
}

function chartOrder(chartId?: string): number {
  const order = [
    "liquidation_price_band",
    "simulation_yield_projection",
    "health_ratio_sweep",
    "stablecoin_treasury_context",
  ];
  const index = order.indexOf(chartId ?? "");
  return index === -1 ? 99 : index;
}

export function SimulatorPage() {
  const [ethPrice, setEthPrice] = useState<number | undefined>();
  const [result, setResult] = useState<SimulationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);

  useEffect(() => {
    getEthPrice()
      .then((quote) => setEthPrice(quote.priceUsd))
      .catch(() => setEthPrice(3800));
  }, []);

  const handleSubmit = async (values: SimulatorFormValues) => {
    setLoading(true);
    setError(null);
    setSaveMessage(null);
    try {
      const price = ethPrice ?? values.ethPriceUsd;
      const response = await runSimulation(toRequest(values, price));
      setResult(response);
      trackEvent("simulation_completed", {
        protocol: values.protocol,
        riskTier: response.riskTier ?? "UNKNOWN",
        healthRatio: response.healthRatio ?? 0,
        years: values.years,
      });
    } catch (err) {
      setResult(null);
      setError(err instanceof Error ? err.message : "Simulation failed");
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!result) return;
    const user = await getCurrentUser();
    if (!user) {
      setSaveMessage("Sign in to save simulations.");
      return;
    }
    try {
      await saveSimulation(user.id, result.assumptions ?? {}, result as unknown as Record<string, unknown>);
      setSaveMessage("Simulation saved to your profile.");
    } catch (err) {
      setSaveMessage(err instanceof Error ? err.message : "Failed to save simulation");
    }
  };

  const charts = [...(result?.charts ?? [])].sort(
    (a, b) => chartOrder(a.chartId) - chartOrder(b.chartId),
  );

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-slate-50">Simulator</h1>
        <p className="mt-1 text-sm text-slate-400">
          Anonymous simulations — no account required. ETH price:{" "}
          {ethPrice ? `$${ethPrice.toLocaleString()}` : "loading…"}
        </p>
      </header>

      <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
        <SimulatorForm initialEthPrice={ethPrice} loading={loading} onSubmit={handleSubmit} />
        <div className="space-y-4">
          {loading ? <p className="text-slate-400">Running simulation…</p> : null}
          {error ? (
            <p className="rounded border border-rose-800 bg-rose-950/40 p-3 text-rose-200">{error}</p>
          ) : null}
          {result ? (
            <>
              <SimulationResults result={result} />
              <div className="grid gap-4 lg:grid-cols-2">
                {charts
                  .filter((chart) => chart.chartId === "liquidation_price_band")
                  .map((chart) => (
                    <ChartSpecRenderer key={chart.chartId} spec={chart} />
                  ))}
              </div>
              {charts
                .filter((chart) => chart.chartId !== "liquidation_price_band")
                .map((chart) => (
                  <ChartSpecRenderer key={chart.chartId} spec={chart} />
                ))}
              <TreasuryEducationPanel treasuryContext={result.treasuryContext} />
              {isSupabaseConfigured() ? (
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={handleSave}
                    className="rounded bg-slate-700 px-3 py-2 text-sm text-white hover:bg-slate-600"
                  >
                    Save simulation
                  </button>
                  {saveMessage ? <p className="text-sm text-slate-400">{saveMessage}</p> : null}
                </div>
              ) : null}
              <AiPanel simulationResult={result} />
            </>
          ) : (
            <p className="text-slate-400">Submit the form to see metrics and charts.</p>
          )}
        </div>
      </div>
    </div>
  );
}