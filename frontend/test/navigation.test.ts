import { describe, expect, it } from "vitest";

import { sanitizeReturnPath } from "@/lib/navigation";

describe("sanitizeReturnPath", () => {
  it("accepts safe same-origin paths", () => {
    expect(sanitizeReturnPath("/profile")).toBe("/profile");
    expect(sanitizeReturnPath("/wallet?tab=audit")).toBe("/wallet?tab=audit");
    expect(sanitizeReturnPath("/a/b#frag")).toBe("/a/b#frag");
  });

  it("rejects open redirects to fallback", () => {
    const fallback = "/safe";
    expect(sanitizeReturnPath(null, fallback)).toBe(fallback);
    expect(sanitizeReturnPath("", fallback)).toBe(fallback);
    expect(sanitizeReturnPath("//evil.com", fallback)).toBe(fallback);
    expect(sanitizeReturnPath("/\\evil.com", fallback)).toBe(fallback);
    expect(sanitizeReturnPath(".evil.com", fallback)).toBe(fallback);
    expect(sanitizeReturnPath(":pw@evil.com", fallback)).toBe(fallback);
    expect(sanitizeReturnPath("https://evil.com", fallback)).toBe(fallback);
    expect(sanitizeReturnPath("javascript:alert(1)", fallback)).toBe(fallback);
    expect(sanitizeReturnPath("/ok\r\nSet-Cookie:x=1", fallback)).toBe(fallback);
    expect(sanitizeReturnPath("\\evil.com", fallback)).toBe(fallback);
  });
});