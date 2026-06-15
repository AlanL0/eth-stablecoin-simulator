import { createBrowserClient } from "@supabase/ssr";
import { type Session, type SupabaseClient, type User } from "@supabase/supabase-js";

import { getSupabasePublicKey, getSupabaseUrl, isSupabaseConfigured } from "@/lib/supabase/config";

export { isSupabaseConfigured } from "@/lib/supabase/config";

export type SavedSimulation = {
  id: string;
  user_id: string | null;
  input: SimulationInputJson;
  result: SimulationResultJson;
  created_at: string;
};

export type SimulationInputJson = Record<string, unknown>;
export type SimulationResultJson = Record<string, unknown>;

export type Profile = {
  id: string;
  display_name: string | null;
  avatar_url: string | null;
  preferred_currency: string;
  created_at: string;
  updated_at: string;
};

type SupabaseOverrides = {
  client?: SupabaseClient | null;
  session?: Session | null;
};

let overrides: SupabaseOverrides = {};

export function setSupabaseOverrides(next: SupabaseOverrides): void {
  overrides = next;
}

export function resetSupabaseForTests(): void {
  overrides = {};
}

export function getSupabaseClient(): SupabaseClient | null {
  if (overrides.client !== undefined) {
    return overrides.client;
  }
  const url = getSupabaseUrl();
  const key = getSupabasePublicKey();
  if (!url || !key) {
    return null;
  }
  return createBrowserClient(url, key);
}

export async function getSession(): Promise<Session | null> {
  if (overrides.session !== undefined) {
    return overrides.session;
  }
  const client = getSupabaseClient();
  if (!client) return null;
  const { data } = await client.auth.getSession();
  return data.session;
}

export async function getCurrentUser(): Promise<User | null> {
  const session = await getSession();
  return session?.user ?? null;
}

export async function signInWithOtp(email: string): Promise<{ error: string | null }> {
  const client = getSupabaseClient();
  if (!client) {
    return { error: "Supabase is not configured" };
  }
  const { error } = await client.auth.signInWithOtp({
    email,
    options: {
      emailRedirectTo: `${window.location.origin}/auth/callback?next=/profile`,
    },
  });
  return { error: error?.message ?? null };
}

export async function signOut(): Promise<void> {
  const client = getSupabaseClient();
  if (!client) return;
  await client.auth.signOut();
}

export async function getProfile(userId: string): Promise<Profile | null> {
  const client = getSupabaseClient();
  if (!client) return null;
  const { data, error } = await client
    .from("profiles")
    .select("*")
    .eq("id", userId)
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data as Profile | null;
}

export async function saveSimulation(
  userId: string,
  input: SimulationInputJson,
  result: SimulationResultJson,
): Promise<SavedSimulation> {
  const client = getSupabaseClient();
  if (!client) {
    throw new Error("Supabase is not configured");
  }
  const { data, error } = await client
    .from("simulations")
    .insert({ user_id: userId, input, result })
    .select("*")
    .single();
  if (error) throw new Error(error.message);
  return data as SavedSimulation;
}

export async function listSimulations(userId: string): Promise<SavedSimulation[]> {
  const client = getSupabaseClient();
  if (!client) return [];
  const { data, error } = await client
    .from("simulations")
    .select("*")
    .eq("user_id", userId)
    .order("created_at", { ascending: false });
  if (error) throw new Error(error.message);
  return (data ?? []) as SavedSimulation[];
}