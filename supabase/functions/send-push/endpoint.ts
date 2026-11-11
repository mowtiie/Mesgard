// A subscription endpoint is chosen by the client, so the function only posts to endpoints that cannot reach internal
// services: an allowlist of push server hosts when one is configured, otherwise public HTTPS hosts only.

export type Resolver = (hostname: string) => Promise<string[]>;

export function parseAllowedHosts(value: string | undefined): string[] {
  return (value ?? "").split(",").map((host) => host.trim().toLowerCase()).filter((host) => host.length > 0);
}

function matchesAllowedHost(hostname: string, allowedHosts: string[]): boolean {
  return allowedHosts.some((pattern) =>
    pattern.startsWith("*.")
      ? hostname.endsWith(pattern.slice(1)) && hostname.length > pattern.length - 1
      : hostname === pattern
  );
}

function ipv4Octets(address: string): number[] | null {
  const parts = address.split(".");
  if (parts.length !== 4) return null;
  const octets = parts.map((part) => (/^\d{1,3}$/.test(part) ? Number(part) : NaN));
  return octets.every((octet) => octet >= 0 && octet <= 255) ? octets : null;
}

export function isPrivateAddress(address: string): boolean {
  const octets = ipv4Octets(address);
  if (octets) {
    const [first, second] = octets;
    return first === 0 || first === 10 || first === 127 ||
      (first === 100 && second >= 64 && second <= 127) ||
      (first === 169 && second === 254) ||
      (first === 172 && second >= 16 && second <= 31) ||
      (first === 192 && second === 168) ||
      (first === 192 && second === 0) ||
      first >= 224;
  }
  const lowered = address.toLowerCase().replace(/^\[|\]$/g, "");
  if (lowered.includes(":")) {
    const mappedV4 = lowered.match(/^::ffff:(\d+\.\d+\.\d+\.\d+)$/);
    if (mappedV4) return isPrivateAddress(mappedV4[1]);
    return lowered === "::" || lowered === "::1" || lowered.startsWith("fc") || lowered.startsWith("fd") ||
      lowered.startsWith("fe8") || lowered.startsWith("fe9") || lowered.startsWith("fea") ||
      lowered.startsWith("feb");
  }
  return false;
}

function isInternalName(hostname: string): boolean {
  return hostname === "localhost" || hostname.endsWith(".localhost") || hostname.endsWith(".local") ||
    hostname.endsWith(".internal") || !hostname.includes(".");
}

export function createEndpointGuard(allowedHosts: string[], resolve: Resolver) {
  return async function isAllowed(endpoint: string): Promise<boolean> {
    let url: URL;
    try {
      url = new URL(endpoint);
    } catch {
      return false;
    }
    if (url.username || url.password) return false;
    if (url.protocol !== "https:" && url.protocol !== "http:") return false;
    const hostname = url.hostname.toLowerCase();

    if (allowedHosts.length > 0) return matchesAllowedHost(hostname, allowedHosts);

    if (url.protocol !== "https:" || isInternalName(hostname) || isPrivateAddress(hostname)) return false;
    try {
      const addresses = await resolve(hostname);
      return addresses.length > 0 && !addresses.some(isPrivateAddress);
    } catch {
      return false;
    }
  };
}
