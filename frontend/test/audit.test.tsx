import AuditPage from "@/app/audit/page";
import * as api from "@/lib/api";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const VALID_ADDRESS = "0x1234567890123456789012345678901234567890";

describe("AuditPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("passes hideValues to audit API and renders hidden amounts", async () => {
    const getAudit = vi.spyOn(api, "getAudit").mockResolvedValue({
      address: VALID_ADDRESS,
      hideValues: true,
      assumptions: [],
      events: [
        {
          token: "USDC",
          txHash: "0xabc",
          logIndex: 0,
          fromAddress: "0xfrom",
          toAddress: "0xto",
          amount: "1000000",
          blockNumber: 1,
          occurredAt: "2026-06-09T12:00:00Z",
        },
      ],
    });

    const user = userEvent.setup();
    render(<AuditPage />);
    await user.type(screen.getByLabelText("Wallet address"), VALID_ADDRESS);
    await user.click(screen.getByLabelText("Hide values (privacy mode)"));
    await user.click(screen.getByRole("button", { name: "Fetch audit" }));

    await waitFor(() => {
      expect(getAudit).toHaveBeenCalledWith(VALID_ADDRESS, { hideValues: true });
      expect(screen.getByText("hidden")).toBeInTheDocument();
    });
  });
});