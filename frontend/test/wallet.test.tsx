import WalletPage from "@/app/wallet/page";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";

describe("WalletPage", () => {
  it("shows error for invalid address without calling API", async () => {
    const user = userEvent.setup();
    render(<WalletPage />);
    await user.type(screen.getByLabelText("Wallet address"), "not-an-address");
    await user.click(screen.getByRole("button", { name: "Lookup" }));
    expect(
      screen.getByText(/Invalid EVM address. Expected 0x followed by 40 hex characters./),
    ).toBeInTheDocument();
  });
});