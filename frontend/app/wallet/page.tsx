"use client";

import { ApiError, getWalletStablecoins } from "@/lib/api";
import { formatUsd } from "@/lib/format";
import { isValidEvmAddress } from "@/lib/validation";
import { useState } from "react";

export default function WalletPage() {
  const [address, setAddress] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [balances, setBalances] = useState<
    Awaited<ReturnType<typeof getWalletStablecoins>> | null
  >(null);

  const handleLookup = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!isValidEvmAddress(address)) {
      setError("Invalid EVM address. Expected 0x followed by 40 hex characters.");
      setBalances(null);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await getWalletStablecoins(address);
      setBalances(response);
    } catch (err) {
      setBalances(null);
      if (err instanceof ApiError && err.code === "INVALID_ADDRESS") {
        setError("Invalid EVM address.");
      } else {
        setError(err instanceof Error ? err.message : "Wallet lookup failed");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <header>
        <h1 className="text-2xl font-bold">Wallet</h1>
        <p className="text-sm text-slate-400">View allowlisted stablecoin balances for an address.</p>
      </header>

      <form onSubmit={handleLookup} className="flex flex-col gap-3 sm:flex-row">
        <input
          type="text"
          value={address}
          onChange={(event) => setAddress(event.target.value)}
          placeholder="0x…"
          className="flex-1 rounded border border-slate-600 bg-slate-900 px-3 py-2"
          aria-label="Wallet address"
        />
        <button
          type="submit"
          disabled={loading}
          className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-60"
        >
          {loading ? "Loading…" : "Lookup"}
        </button>
      </form>

      {error ? <p className="text-rose-300">{error}</p> : null}

      {balances ? (
        <section className="rounded-xl border border-slate-700 bg-surface-card p-4">
          <p className="text-sm text-slate-400">
            {balances.address} · source {balances.source} · {balances.observedAt}
          </p>
          <ul className="mt-3 space-y-2">
            {balances.balances?.map((balance) => (
              <li
                key={balance.contractAddress}
                className="flex items-center justify-between rounded bg-slate-900/50 px-3 py-2"
              >
                <span>{balance.symbol}</span>
                <span>
                  {balance.balance} ({formatUsd(balance.balanceUsd)})
                </span>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  );
}