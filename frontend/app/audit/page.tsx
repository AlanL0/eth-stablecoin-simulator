"use client";

import { getAudit } from "@/lib/api";
import { isValidEvmAddress } from "@/lib/validation";
import { useState } from "react";

export default function AuditPage() {
  const [address, setAddress] = useState("");
  const [hideValues, setHideValues] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [audit, setAudit] = useState<Awaited<ReturnType<typeof getAudit>> | null>(null);

  const handleLookup = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!isValidEvmAddress(address)) {
      setError("Invalid EVM address.");
      setAudit(null);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await getAudit(address, { hideValues });
      setAudit(response);
    } catch (err) {
      setAudit(null);
      setError(err instanceof Error ? err.message : "Audit lookup failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <header>
        <h1 className="text-2xl font-bold">Audit</h1>
        <p className="text-sm text-slate-400">Transfer events for allowlisted stablecoins.</p>
      </header>

      <form onSubmit={handleLookup} className="space-y-3 rounded-xl border border-slate-700 bg-surface-card p-4">
        <input
          type="text"
          value={address}
          onChange={(event) => setAddress(event.target.value)}
          placeholder="0x…"
          className="w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
          aria-label="Wallet address"
        />
        <label className="flex items-center gap-2 text-sm text-slate-300">
          <input
            type="checkbox"
            checked={hideValues}
            onChange={(event) => setHideValues(event.target.checked)}
          />
          Hide values (privacy mode)
        </label>
        <button
          type="submit"
          disabled={loading}
          className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-60"
        >
          {loading ? "Loading…" : "Fetch audit"}
        </button>
      </form>

      {error ? <p className="text-rose-300">{error}</p> : null}

      {audit ? (
        <section className="rounded-xl border border-slate-700 bg-surface-card p-4">
          <p className="text-sm text-slate-400">
            {audit.events?.length ?? 0} events · hideValues={String(audit.hideValues)}
          </p>
          <div className="mt-3 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="text-slate-400">
                <tr>
                  <th className="px-2 py-1">Token</th>
                  <th className="px-2 py-1">From</th>
                  <th className="px-2 py-1">To</th>
                  <th className="px-2 py-1">Amount</th>
                  <th className="px-2 py-1">When</th>
                </tr>
              </thead>
              <tbody>
                {audit.events?.map((event) => (
                  <tr key={`${event.txHash}-${event.logIndex}`} className="border-t border-slate-800">
                    <td className="px-2 py-2">{event.token}</td>
                    <td className="px-2 py-2 font-mono text-xs">{event.fromAddress}</td>
                    <td className="px-2 py-2 font-mono text-xs">{event.toAddress}</td>
                    <td className="px-2 py-2">
                      {audit.hideValues ? "hidden" : event.amount}
                    </td>
                    <td className="px-2 py-2">{event.occurredAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </div>
  );
}