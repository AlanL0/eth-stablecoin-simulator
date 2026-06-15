import type { SimulationResponse } from "@/lib/api";
import { formatUsd } from "@/lib/format";

type TreasuryEducationPanelProps = {
  treasuryContext: SimulationResponse["treasuryContext"];
};

export function TreasuryEducationPanel({ treasuryContext }: TreasuryEducationPanelProps) {
  if (!treasuryContext) {
    return null;
  }

  return (
    <section
      className="rounded-xl border border-slate-700 bg-surface-card p-4"
      aria-label="Treasury education"
    >
      <h3 className="text-lg font-semibold text-slate-100">Treasury context (educational)</h3>
      <p className="mt-2 text-sm text-slate-400">{treasuryContext.disclaimer}</p>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <article className="rounded-lg border border-slate-700 bg-slate-900/50 p-3">
          <p className="text-xs uppercase text-slate-500">Your mint</p>
          <p className="mt-1 text-base font-medium text-slate-100">
            {formatUsd(treasuryContext.yourMintUsd)}
          </p>
          {treasuryContext.yourMint ? (
            <ul className="mt-2 space-y-1 text-sm text-slate-300">
              <li>
                Implied T-bill backing:{" "}
                {formatUsd(treasuryContext.yourMint.impliedTreasuryBackingUsd)}
              </li>
              <li>
                Issuer reserve yield (annual):{" "}
                {formatUsd(treasuryContext.yourMint.annualIssuerReserveYieldUsd)}
              </li>
            </ul>
          ) : null}
        </article>

        <article className="rounded-lg border border-slate-700 bg-slate-900/50 p-3">
          <p className="text-xs uppercase text-slate-500">System context</p>
          {treasuryContext.systemContext ? (
            <ul className="mt-2 space-y-1 text-sm text-slate-300">
              <li>
                Implied backing:{" "}
                {formatUsd(treasuryContext.systemContext.impliedTreasuryBackingUsd)}
              </li>
              <li>
                Annual issuer yield:{" "}
                {formatUsd(treasuryContext.systemContext.annualIssuerReserveYieldUsd)}
              </li>
              <li>
                Treasury demand proxy:{" "}
                {formatUsd(treasuryContext.systemContext.treasuryDemandProxyUsd)}
              </li>
            </ul>
          ) : null}
        </article>
      </div>

      {treasuryContext.personalComparison ? (
        <p className="mt-3 text-sm text-slate-300">
          Your DeFi net yield:{" "}
          {formatUsd(treasuryContext.personalComparison.yourDeFiProjectedNetYieldUsd)} —{" "}
          {treasuryContext.personalComparison.note}
        </p>
      ) : null}
    </section>
  );
}