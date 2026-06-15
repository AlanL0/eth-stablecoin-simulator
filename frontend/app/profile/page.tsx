"use client";

import { formatUsd } from "@/lib/format";
import {
  getCurrentUser,
  isSupabaseConfigured,
  listSimulations,
  type SavedSimulation,
} from "@/lib/supabase";
import Link from "next/link";
import { useEffect, useState } from "react";

export default function ProfilePage() {
  const [simulations, setSimulations] = useState<SavedSimulation[]>([]);
  const [loading, setLoading] = useState(true);
  const [signedIn, setSignedIn] = useState(false);

  useEffect(() => {
    let active = true;
    (async () => {
      const user = await getCurrentUser();
      if (!active) return;
      setSignedIn(Boolean(user));
      if (user) {
        const rows = await listSimulations(user.id);
        if (active) setSimulations(rows);
      }
      if (active) setLoading(false);
    })();
    return () => {
      active = false;
    };
  }, []);

  if (!isSupabaseConfigured()) {
    return (
      <div className="space-y-2">
        <h1 className="text-2xl font-bold">Profile</h1>
        <p className="text-slate-400">
          Supabase not configured. You can still run anonymous simulations on the home page.
        </p>
      </div>
    );
  }

  if (loading) {
    return <p className="text-slate-400">Loading profile…</p>;
  }

  if (!signedIn) {
    return (
      <div className="space-y-3">
        <h1 className="text-2xl font-bold">Profile</h1>
        <p className="text-slate-400">Sign in to view saved simulations.</p>
        <Link href="/login" className="text-blue-400 hover:underline">
          Go to login
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <header>
        <h1 className="text-2xl font-bold">Profile</h1>
        <p className="text-sm text-slate-400">Your saved simulations (RLS-scoped to your account).</p>
      </header>
      {simulations.length === 0 ? (
        <p className="text-slate-400">No saved simulations yet.</p>
      ) : (
        <ul className="space-y-3">
          {simulations.map((simulation) => {
            const result = simulation.result as {
              stablecoinDebtUsd?: number;
              healthRatio?: number;
              riskTier?: string;
            };
            return (
              <li
                key={simulation.id}
                className="rounded-xl border border-slate-700 bg-surface-card p-4"
              >
                <p className="text-sm text-slate-400">{simulation.created_at}</p>
                <p className="mt-1 text-slate-100">
                  Debt {formatUsd(result.stablecoinDebtUsd)} · Health{" "}
                  {result.healthRatio ?? "—"} · {result.riskTier ?? "—"}
                </p>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}