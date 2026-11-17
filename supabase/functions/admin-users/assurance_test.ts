import { assertEquals } from "jsr:@std/assert@1";
import { assuranceLevel } from "./assurance.ts";

function tokenWith(claims: unknown): string {
  const encode = (value: unknown) =>
    btoa(JSON.stringify(value)).replace(/=+$/, "").replace(/\+/g, "-").replace(/\//g, "_");
  return `${encode({ alg: "HS256" })}.${encode(claims)}.signature`;
}

Deno.test("reads the assurance level of a token", () => {
  assertEquals(assuranceLevel(tokenWith({ aal: "aal2" })), "aal2");
  assertEquals(assuranceLevel(tokenWith({ aal: "aal1", sub: "someone" })), "aal1");
});

Deno.test("returns null when the claim is missing or the token is malformed", () => {
  assertEquals(assuranceLevel(tokenWith({ sub: "someone" })), null);
  assertEquals(assuranceLevel("not-a-token"), null);
  assertEquals(assuranceLevel("a.%%%.c"), null);
});
