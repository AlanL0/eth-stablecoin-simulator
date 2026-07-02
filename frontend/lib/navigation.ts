/**
 * Return a same-origin path or the fallback. Rejects protocol-relative and backslash forms.
 */
export function sanitizeReturnPath(
  next: string | null | undefined,
  fallback = "/profile",
): string {
  if (next == null || next === "") {
    return fallback;
  }
  if (!next.startsWith("/")) {
    return fallback;
  }
  if (next.startsWith("//") || next.startsWith("/\\")) {
    return fallback;
  }
  if (next.includes("\\") || next.includes("://")) {
    return fallback;
  }
  if (/[\r\n]/.test(next)) {
    return fallback;
  }
  return next;
}