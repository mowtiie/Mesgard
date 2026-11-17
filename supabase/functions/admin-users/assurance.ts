/** The authenticator assurance level claim of a JWT. Auth has already verified the token, so the payload is only read. */
export function assuranceLevel(token: string): string | null {
  const payload = token.split(".")[1];
  if (!payload) return null;
  try {
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const claims = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, "=")));
    return typeof claims.aal === "string" ? claims.aal : null;
  } catch {
    return null;
  }
}
