import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  getRecordedEvents,
  resetAnalyticsForTests,
  setAnalyticsSink,
  trackEvent,
} from "@/lib/analytics";

describe("analytics", () => {
  beforeEach(() => {
    resetAnalyticsForTests();
  });

  it("records simulation_completed event", () => {
    const sink = vi.fn();
    setAnalyticsSink(sink);

    trackEvent("simulation_completed", {
      protocol: "maker_sky",
      riskTier: "HIGH",
      healthRatio: 1.2,
      years: 1,
    });

    expect(getRecordedEvents()).toHaveLength(1);
    expect(getRecordedEvents()[0]).toEqual({
      event: "simulation_completed",
      properties: {
        protocol: "maker_sky",
        riskTier: "HIGH",
        healthRatio: 1.2,
        years: 1,
      },
    });
    expect(sink).toHaveBeenCalledOnce();
  });
});