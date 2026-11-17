import { assertEquals } from "jsr:@std/assert@1";
import { isErasedEmail, placeholderEmail } from "./erasure.ts";

const USER = "20000000-0000-4000-8000-000000000a04";

Deno.test("the placeholder email belongs to one person and is recognised again", () => {
  assertEquals(placeholderEmail(USER), `erased-${USER}@erased.invalid`);
  assertEquals(isErasedEmail(placeholderEmail(USER)), true);
});

Deno.test("an ordinary email is not taken for an erased one", () => {
  assertEquals(isErasedEmail("ana@school.example"), false);
  assertEquals(isErasedEmail("erased-nobody@school.example"), false);
  assertEquals(isErasedEmail(null), false);
});
