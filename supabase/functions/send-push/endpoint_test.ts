import { assertEquals } from "jsr:@std/assert@1";
import { createEndpointGuard, isPrivateAddress, parseAllowedHosts } from "./endpoint.ts";

const publicResolver = () => Promise.resolve(["93.184.216.34"]);

Deno.test("allows a public HTTPS endpoint", async () => {
  const guard = createEndpointGuard([], publicResolver);
  assertEquals(await guard("https://ntfy.example.org/abc123"), true);
});

Deno.test("rejects plain HTTP, other schemes and credentials without an allowlist", async () => {
  const guard = createEndpointGuard([], publicResolver);
  assertEquals(await guard("http://ntfy.example.org/abc"), false);
  assertEquals(await guard("ftp://ntfy.example.org/abc"), false);
  assertEquals(await guard("https://user:secret@ntfy.example.org/abc"), false);
  assertEquals(await guard("not a url"), false);
});

Deno.test("rejects internal names and private address literals", async () => {
  const guard = createEndpointGuard([], publicResolver);
  const endpoints = [
    "https://localhost/x",
    "https://service.internal/x",
    "https://printer.local/x",
    "https://intranet/x",
    "https://127.0.0.1/x",
    "https://10.1.2.3/x",
    "https://169.254.169.254/latest/meta-data",
    "https://192.168.1.10/x",
    "https://[::1]/x",
    "https://2130706433/x",
  ];
  for (const endpoint of endpoints) {
    assertEquals(await guard(endpoint), false, endpoint);
  }
});

Deno.test("rejects a host name that resolves to a private address", async () => {
  const guard = createEndpointGuard([], () => Promise.resolve(["93.184.216.34", "10.0.0.5"]));
  assertEquals(await guard("https://rebind.example.org/x"), false);
});

Deno.test("rejects a host name that does not resolve", async () => {
  assertEquals(await createEndpointGuard([], () => Promise.resolve([]))("https://nothing.example.org/x"), false);
  const failing = createEndpointGuard([], () => Promise.reject(new Error("dns")));
  assertEquals(await failing("https://x.example.org/x"), false);
});

Deno.test("with an allowlist only the listed hosts are allowed, over HTTP or HTTPS", async () => {
  const guard = createEndpointGuard(
    ["ntfy.school.edu", "*.push.school.edu"],
    () => Promise.reject(new Error("unused")),
  );
  assertEquals(await guard("http://ntfy.school.edu/abc"), true);
  assertEquals(await guard("https://eu.push.school.edu/abc"), true);
  assertEquals(await guard("https://push.school.edu/abc"), false);
  assertEquals(await guard("https://evil.example.org/abc"), false);
  assertEquals(await guard("https://ntfy.school.edu.evil.org/abc"), false);
});

Deno.test("parses the allowlist from a comma separated value", () => {
  assertEquals(parseAllowedHosts(" Ntfy.School.edu, *.push.school.edu ,,"), ["ntfy.school.edu", "*.push.school.edu"]);
  assertEquals(parseAllowedHosts(undefined), []);
});

Deno.test("classifies private and public addresses", () => {
  const privateAddresses = [
    "10.0.0.1",
    "127.0.0.1",
    "172.16.0.1",
    "172.31.255.255",
    "192.168.0.1",
    "169.254.1.1",
    "100.64.0.1",
    "0.0.0.0",
    "::1",
    "fd00::1",
    "fe80::1",
    "::ffff:10.0.0.1",
  ];
  for (const address of privateAddresses) assertEquals(isPrivateAddress(address), true, address);

  const publicAddresses = ["8.8.8.8", "93.184.216.34", "172.32.0.1", "2606:2800:220:1:248:1893:25c8:1946"];
  for (const address of publicAddresses) assertEquals(isPrivateAddress(address), false, address);
});
