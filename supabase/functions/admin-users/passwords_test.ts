import { assert, assertEquals, assertNotEquals } from "jsr:@std/assert@1";
import { generateTemporaryPassword } from "./passwords.ts";

Deno.test("a password has twelve characters without look alikes", () => {
  for (let attempt = 0; attempt < 200; attempt++) {
    const password = generateTemporaryPassword();
    assertEquals(password.length, 12);
    assert(/^[A-HJ-NP-Za-km-z2-9]+$/.test(password), `unexpected character in ${password}`);
  }
});

Deno.test("two passwords differ", () => {
  assertNotEquals(generateTemporaryPassword(), generateTemporaryPassword());
});

Deno.test("bytes that would favor some characters are skipped", () => {
  // 252 is the first byte that does not fill a whole round of the 56 characters, so these 12 are skipped.
  const bytes = [...new Array(12).fill(255), ...new Array(24).fill(0)];
  const password = generateTemporaryPassword(() => Uint8Array.from(bytes));
  assertEquals(password, "AAAAAAAAAAAA");
});
