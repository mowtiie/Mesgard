import { assertEquals } from "jsr:@std/assert@1";
import { MAX_USERS_PER_REQUEST, parseRequest, validateNewUser } from "./validation.ts";

const ORG = "10000000-0000-4000-8000-000000000a00";
const USER = "20000000-0000-4000-8000-000000000a04";

Deno.test("accepts the three actions", () => {
  assertEquals("request" in parseRequest({ action: "create_users", org_id: ORG, users: [{}] }), true);
  assertEquals("request" in parseRequest({ action: "reset_password", org_id: ORG, user_id: USER }), true);
  assertEquals("request" in parseRequest({ action: "remove_member", org_id: ORG, user_id: USER }), true);
  assertEquals("request" in parseRequest({ action: "erase_member", org_id: ORG, user_id: USER }), true);
});

Deno.test("refuses what is not a request", () => {
  for (const body of [null, "text", [], {}, { action: "create_users" }, { action: "x", org_id: ORG }]) {
    assertEquals("error" in parseRequest(body), true);
  }
  const tooMany = new Array(MAX_USERS_PER_REQUEST + 1).fill({});
  assertEquals("error" in parseRequest({ action: "create_users", org_id: ORG, users: tooMany }), true);
});

Deno.test("normalizes a user and takes the name from the address when none is given", () => {
  assertEquals(validateNewUser({ email: " Ana@School.Example ", role: "student" }), {
    user: { email: "ana@school.example", displayName: "ana", role: "student" },
  });
  assertEquals(validateNewUser({ email: "ben@school.example", display_name: " Ben Cruz ", role: "staff" }), {
    user: { email: "ben@school.example", displayName: "Ben Cruz", role: "staff" },
  });
});

Deno.test("explains what is wrong with a user", () => {
  assertEquals(validateNewUser("text"), { reason: "invalid_user" });
  assertEquals(validateNewUser({ email: "nobody", role: "student" }), { reason: "invalid_email" });
  assertEquals(validateNewUser({ email: "a@b.example", role: "owner" }), { reason: "invalid_role" });
  assertEquals(validateNewUser({ email: "a@b.example", role: "student", display_name: "x".repeat(101) }), {
    reason: "invalid_display_name",
  });
});
