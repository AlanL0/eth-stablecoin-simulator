import { ChartSpecRenderer } from "@/components/charts/ChartSpecRenderer";
import type { ChartContract } from "@/lib/api";
import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import healthRatioSweep from "./fixtures/charts/health-ratio-sweep.json";
import liquidationBand from "./fixtures/charts/liquidation-price-band.json";
import yieldProjection from "./fixtures/charts/simulation-yield-projection.json";

describe("ChartSpecRenderer", () => {
  it.each([
    ["liquidation-price-band", liquidationBand],
    ["simulation-yield-projection", yieldProjection],
    ["health-ratio-sweep", healthRatioSweep],
  ])("matches snapshot for %s", (_name, fixture) => {
    const { container } = render(
      <ChartSpecRenderer spec={fixture as unknown as ChartContract} />,
    );
    expect(container).toMatchSnapshot();
  });

  it("shows empty state when series have no data", () => {
    const emptySpec: ChartContract = {
      ...(yieldProjection as unknown as ChartContract),
      series: [{ id: "empty", label: "Empty", unit: "usd", data: [] }],
    };
    const { getByText } = render(<ChartSpecRenderer spec={emptySpec} />);
    expect(getByText("No chart data from service")).toBeInTheDocument();
  });
});