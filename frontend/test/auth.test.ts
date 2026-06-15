import {
  listSimulations,
  resetSupabaseForTests,
  saveSimulation,
  setSupabaseOverrides,
} from "@/lib/supabase";
import { runSimulation } from "@/lib/api";
import { configureApiClient, resetApiClientForTests } from "@/lib/api";
import { beforeEach, describe, expect, it, vi } from "vitest";

describe("auth + saved state", () => {
  beforeEach(() => {
    resetSupabaseForTests();
    resetApiClientForTests();
  });

  it("allows anonymous simulation without session", async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        collateralValueUsd: 7600,
        stablecoinDebtUsd: 4222.22,
        liquidationPriceUsd: 3166.67,
        healthRatio: 1.2,
        riskTier: "HIGH",
        assumptions: { protocol: "maker_sky" },
        charts: [],
      }),
    });
    configureApiClient({ fetchImpl: mockFetch as unknown as typeof fetch });

    const result = await runSimulation({
      protocol: "maker_sky",
      ethAmount: 2,
      deployYieldPct: 5,
      years: 1,
      compoundsPerYear: 12,
      treasuryContextEnabled: true,
      stablecoinReserveModel: "usdc_style",
    });
    expect(result.healthRatio).toBe(1.2);
    expect(mockFetch).toHaveBeenCalledOnce();
  });

  it("saves and lists simulations with mocked Supabase", async () => {
    const store: Array<Record<string, unknown>> = [];
    const userId = "user-a";

    setSupabaseOverrides({
      client: {
        from: (table: string) => {
          if (table !== "simulations") throw new Error("unexpected table");
          return {
            insert: (row: Record<string, unknown>) => ({
              select: () => ({
                single: async () => {
                  const saved = {
                    id: "sim-1",
                    created_at: "2026-06-09T12:00:00Z",
                    ...row,
                  };
                  store.push(saved);
                  return { data: saved, error: null };
                },
              }),
            }),
            select: () => ({
              eq: (_col: string, value: string) => ({
                order: async () => ({
                  data: store.filter((row) => row.user_id === value),
                  error: null,
                }),
              }),
            }),
          };
        },
      } as never,
      session: { user: { id: userId } } as never,
    });

    const saved = await saveSimulation(userId, { protocol: "maker_sky" }, { healthRatio: 1.2 });
    const rows = await listSimulations(userId);

    expect(saved.id).toBe("sim-1");
    expect(rows).toHaveLength(1);
    expect(rows[0].user_id).toBe(userId);
  });

  it("scopes listed simulations to the requesting user", async () => {
    setSupabaseOverrides({
      client: {
        from: () => ({
          select: () => ({
            eq: (_col: string, value: string) => ({
              order: async () => ({
                data:
                  value === "user-a"
                    ? [{ id: "a1", user_id: "user-a", input: {}, result: {}, created_at: "" }]
                    : [],
                error: null,
              }),
            }),
          }),
        }),
      } as never,
    });

    const userARows = await listSimulations("user-a");
    const userBRows = await listSimulations("user-b");
    expect(userARows).toHaveLength(1);
    expect(userBRows).toHaveLength(0);
  });
});