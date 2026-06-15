import { AiPanel } from "@/components/ai/AiPanel";
import type { SimulationResponse } from "@/lib/api";
import * as api from "@/lib/api";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mockResult: SimulationResponse = {
  collateralValueUsd: 7600,
  stablecoinDebtUsd: 4222.22,
  liquidationPriceUsd: 3166.67,
  healthRatio: 1.2,
  riskTier: "HIGH",
  assumptions: { protocol: "maker_sky" },
  charts: [
    {
      schemaVersion: "1.0",
      chartId: "liquidation_price_band",
      chartType: "band",
      title: "ETH Spot vs Liquidation Price",
      xAxis: { type: "category", label: "Price marker" },
      yAxis: { type: "linear", label: "USD per ETH", format: "usd" },
      series: [
        {
          id: "safe_band",
          name: "Collateral buffer",
          geometry: "band",
          points: [{ x: "range", y0: 3166.67, y1: 3800 }],
        },
      ],
      source: "test",
      generatedAt: "2026-06-09T12:00:01Z",
    },
  ],
};

describe("AiPanel", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("keeps simulation visible when AI fails", async () => {
    vi.spyOn(api, "recommendYield").mockRejectedValue(new Error("agent down"));
    const user = userEvent.setup();

    render(
      <div>
        <p>Simulation charts visible</p>
        <AiPanel simulationResult={mockResult} />
      </div>,
    );

    await user.click(screen.getByRole("button", { name: "Ask AI" }));

    await waitFor(() => {
      expect(screen.getByText(/Simulation charts visible/)).toBeInTheDocument();
      expect(screen.getByText(/AI unavailable: agent down/)).toBeInTheDocument();
    });
  });

  it("shows backlog notice when feedback is escalated", async () => {
    vi.spyOn(api, "recommendYield").mockResolvedValue({
      summary: "Test summary",
      recommendations: [],
      risks: [],
      assumptions: [],
      feedback: [
        {
          kind: "chart_request",
          normalizedLabel: "custom_chart",
          requestCount: 3,
          backlogEscalated: true,
          message: "Escalated",
        },
      ],
      chartSpecs: [],
      model: "test",
      fallbackUsed: false,
    });
    const user = userEvent.setup();
    render(<AiPanel simulationResult={mockResult} />);
    await user.click(screen.getByRole("button", { name: "Ask AI" }));
    await waitFor(() => {
      expect(screen.getByText(/Chart request escalated to backlog/)).toBeInTheDocument();
    });
  });
});