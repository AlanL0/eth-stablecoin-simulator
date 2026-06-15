import { SimulatorForm } from "@/components/simulator/SimulatorForm";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

describe("SimulatorForm", () => {
  it("renders inputs and protocol select", () => {
    render(<SimulatorForm onSubmit={vi.fn()} />);
    expect(screen.getByLabelText("Simulation inputs")).toBeInTheDocument();
    expect(screen.getByText("Protocol")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Run simulation" })).toBeInTheDocument();
  });

  it("submits values on run simulation", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<SimulatorForm onSubmit={onSubmit} initialEthPrice={3800} />);
    await user.click(screen.getByRole("button", { name: "Run simulation" }));
    expect(onSubmit).toHaveBeenCalledOnce();
    expect(onSubmit.mock.calls[0][0].protocol).toBe("maker_sky");
  });

  it("updates protocol preset ratios", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<SimulatorForm onSubmit={onSubmit} />);
    await user.selectOptions(screen.getByRole("combobox"), "liquity");
    await user.click(screen.getByRole("button", { name: "Run simulation" }));
    expect(onSubmit.mock.calls[0][0].protocol).toBe("liquity");
    expect(onSubmit.mock.calls[0][0].targetCollateralRatio).toBe(2);
  });

  it("shows loading state", () => {
    render(<SimulatorForm onSubmit={vi.fn()} loading />);
    expect(screen.getByRole("button", { name: "Running simulation…" })).toBeDisabled();
  });
});