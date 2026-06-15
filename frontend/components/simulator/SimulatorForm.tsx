"use client";

import { getProtocolPreset, PROTOCOL_PRESETS } from "@/lib/protocols";
import { useEffect, useState } from "react";

export type CollateralMode = "eth" | "usd";

export type SimulatorFormValues = {
  collateralMode: CollateralMode;
  ethAmount: number;
  collateralUsd: number;
  ethPriceUsd: number;
  protocol: string;
  targetCollateralRatio: number;
  liquidationRatio: number;
  stabilityFeePct: number;
  deployYieldPct: number;
  years: number;
  compoundsPerYear: number;
};

type SimulatorFormProps = {
  initialEthPrice?: number;
  loading?: boolean;
  onSubmit: (values: SimulatorFormValues) => void;
};

const DEFAULT_VALUES: SimulatorFormValues = {
  collateralMode: "eth",
  ethAmount: 2,
  collateralUsd: 7600,
  ethPriceUsd: 3800,
  protocol: "maker_sky",
  targetCollateralRatio: 1.8,
  liquidationRatio: 1.5,
  stabilityFeePct: 5,
  deployYieldPct: 5,
  years: 1,
  compoundsPerYear: 12,
};

export function SimulatorForm({ initialEthPrice, loading, onSubmit }: SimulatorFormProps) {
  const [values, setValues] = useState<SimulatorFormValues>(DEFAULT_VALUES);

  useEffect(() => {
    if (initialEthPrice) {
      setValues((current) => ({
        ...current,
        ethPriceUsd: initialEthPrice,
        collateralUsd: current.ethAmount * initialEthPrice,
      }));
    }
  }, [initialEthPrice]);

  const update = <K extends keyof SimulatorFormValues>(key: K, value: SimulatorFormValues[K]) => {
    setValues((current) => ({ ...current, [key]: value }));
  };

  const handleProtocolChange = (protocol: string) => {
    const preset = getProtocolPreset(protocol);
    setValues((current) => ({
      ...current,
      protocol,
      targetCollateralRatio: preset.targetCollateralRatio,
      liquidationRatio: preset.liquidationRatio,
      stabilityFeePct: preset.stabilityFeePct,
    }));
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    onSubmit(values);
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-4 rounded-xl border border-slate-700 bg-surface-card p-4"
      aria-label="Simulation inputs"
    >
      <div className="flex gap-2">
        <button
          type="button"
          className={`rounded px-3 py-1 text-sm ${
            values.collateralMode === "eth" ? "bg-blue-600 text-white" : "bg-slate-700 text-slate-200"
          }`}
          onClick={() => update("collateralMode", "eth")}
        >
          ETH collateral
        </button>
        <button
          type="button"
          className={`rounded px-3 py-1 text-sm ${
            values.collateralMode === "usd" ? "bg-blue-600 text-white" : "bg-slate-700 text-slate-200"
          }`}
          onClick={() => update("collateralMode", "usd")}
        >
          USD collateral
        </button>
      </div>

      {values.collateralMode === "eth" ? (
        <label className="block text-sm">
          <span className="text-slate-300">ETH amount</span>
          <input
            type="number"
            min="0.01"
            step="0.01"
            className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
            value={values.ethAmount}
            onChange={(event) => update("ethAmount", Number(event.target.value))}
          />
        </label>
      ) : (
        <label className="block text-sm">
          <span className="text-slate-300">Collateral USD</span>
          <input
            type="number"
            min="1"
            step="1"
            className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
            value={values.collateralUsd}
            onChange={(event) => update("collateralUsd", Number(event.target.value))}
          />
        </label>
      )}

      <label className="block text-sm">
        <span className="text-slate-300">Protocol</span>
        <select
          className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
          value={values.protocol}
          onChange={(event) => handleProtocolChange(event.target.value)}
        >
          {PROTOCOL_PRESETS.map((preset) => (
            <option key={preset.name} value={preset.name}>
              {preset.displayName}
            </option>
          ))}
        </select>
      </label>

      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block text-sm">
          <span className="text-slate-300">Target collateral ratio</span>
          <input
            type="number"
            step="0.01"
            className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
            value={values.targetCollateralRatio}
            onChange={(event) => update("targetCollateralRatio", Number(event.target.value))}
          />
        </label>
        <label className="block text-sm">
          <span className="text-slate-300">Liquidation ratio</span>
          <input
            type="number"
            step="0.01"
            className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
            value={values.liquidationRatio}
            onChange={(event) => update("liquidationRatio", Number(event.target.value))}
          />
        </label>
        <label className="block text-sm">
          <span className="text-slate-300">Stability fee %</span>
          <input
            type="number"
            step="0.1"
            className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
            value={values.stabilityFeePct}
            onChange={(event) => update("stabilityFeePct", Number(event.target.value))}
          />
        </label>
        <label className="block text-sm">
          <span className="text-slate-300">Deploy yield %</span>
          <input
            type="number"
            step="0.1"
            className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
            value={values.deployYieldPct}
            onChange={(event) => update("deployYieldPct", Number(event.target.value))}
          />
        </label>
        <label className="block text-sm">
          <span className="text-slate-300">Years</span>
          <input
            type="number"
            min="0"
            max="50"
            className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
            value={values.years}
            onChange={(event) => update("years", Number(event.target.value))}
          />
        </label>
        <label className="block text-sm">
          <span className="text-slate-300">Compounds / year</span>
          <input
            type="number"
            min="1"
            max="365"
            className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
            value={values.compoundsPerYear}
            onChange={(event) => update("compoundsPerYear", Number(event.target.value))}
          />
        </label>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full rounded bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-500 disabled:opacity-60"
      >
        {loading ? "Running simulation…" : "Run simulation"}
      </button>
    </form>
  );
}