"use client";

import { isSupabaseConfigured, signInWithOtp } from "@/lib/supabase";
import { useState } from "react";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setMessage(null);
    const { error } = await signInWithOtp(email);
    setLoading(false);
    setMessage(error ? error : "Check your email for the magic link.");
  };

  if (!isSupabaseConfigured()) {
    return (
      <div className="space-y-2">
        <h1 className="text-2xl font-bold">Login</h1>
        <p className="text-slate-400">
          Supabase is not configured. Anonymous simulation still works without signing in.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md space-y-4">
      <header>
        <h1 className="text-2xl font-bold">Login</h1>
        <p className="text-sm text-slate-400">Magic-link sign in to save simulations.</p>
      </header>
      <form onSubmit={handleSubmit} className="space-y-3 rounded-xl border border-slate-700 bg-surface-card p-4">
        <label className="block text-sm">
          <span className="text-slate-300">Email</span>
          <input
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="mt-1 w-full rounded border border-slate-600 bg-slate-900 px-3 py-2"
          />
        </label>
        <button
          type="submit"
          disabled={loading}
          className="w-full rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-60"
        >
          {loading ? "Sending…" : "Send magic link"}
        </button>
      </form>
      {message ? <p className="text-sm text-slate-300">{message}</p> : null}
    </div>
  );
}