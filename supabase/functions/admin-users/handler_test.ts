import { assertEquals, assertRejects } from "jsr:@std/assert@1";
import { placeholderEmail } from "./erasure.ts";
import { type AdminApi, handleAdminRequest } from "./handler.ts";
import type { Role } from "./validation.ts";

const ORG = "10000000-0000-4000-8000-000000000a00";
const ADMIN = "20000000-0000-4000-8000-000000000a01";
const MEMBER = "20000000-0000-4000-8000-000000000a04";
const TOKEN = "admin-token";

class FakeApi implements AdminApi {
  roles = new Map<string, Role>([[ADMIN, "org_admin"], [MEMBER, "member"]]);
  takenEmails = new Set<string>();
  memberships = new Map<string, number>([[MEMBER, 1]]);
  failAddingFor = new Set<string>();
  calls: string[] = [];
  audits: string[] = [];
  passwords = new Map<string, string>();
  validTokens = new Set([TOKEN]);
  mfaRequired = false;
  factors = new Map<string, number>();
  logins = new Map<string, { userId: string; isDisabled: boolean; membershipCount: number }>();
  reactivated: string[] = [];
  policy = "keep_messages";
  emails = new Map<string, string>([[MEMBER, "ana@school.example"]]);
  removedRecords = new Set<string>();
  order: string[] = [];
  files = new Map<string, string[]>();
  failRemovingFiles = false;
  private nextId = 1;

  getUserId(token: string) {
    return Promise.resolve(this.validTokens.has(token) ? ADMIN : null);
  }
  getMemberRole(_token: string, _orgId: string, userId: string) {
    return Promise.resolve(this.roles.get(userId) ?? null);
  }
  countAdmins() {
    return Promise.resolve([...this.roles.values()].filter((role) => role === "org_admin").length);
  }
  createAuthUser(input: { email: string; password: string; displayName: string }) {
    if (this.takenEmails.has(input.email)) return Promise.resolve("email_taken" as const);
    const id = `created-${this.nextId++}`;
    this.passwords.set(id, input.password);
    this.calls.push(`create ${input.email}`);
    return Promise.resolve(id);
  }
  deleteAuthUser(userId: string) {
    this.calls.push(`delete ${userId}`);
    return Promise.resolve();
  }
  addMember(_token: string, _orgId: string, userId: string, role: Role) {
    if (this.failAddingFor.has(userId)) return Promise.resolve(false);
    this.roles.set(userId, role);
    return Promise.resolve(true);
  }
  removeMember(_token: string, _orgId: string, userId: string) {
    const removed = this.roles.delete(userId);
    this.order.push("remove");
    this.memberships.set(userId, Math.max(0, (this.memberships.get(userId) ?? 1) - 1));
    return Promise.resolve(removed);
  }
  countMemberships(userId: string) {
    return Promise.resolve(this.memberships.get(userId) ?? 0);
  }
  setTemporaryPassword(userId: string, password: string) {
    this.passwords.set(userId, password);
    return Promise.resolve();
  }
  findLogin(email: string) {
    return Promise.resolve(this.logins.get(email) ?? null);
  }
  reactivateLogin(userId: string, password: string) {
    this.reactivated.push(userId);
    this.passwords.set(userId, password);
    return Promise.resolve();
  }
  erasurePolicy() {
    return Promise.resolve(this.policy);
  }
  getLoginEmail(userId: string) {
    return Promise.resolve(this.emails.get(userId) ?? null);
  }
  wasRemovedFrom(_token: string, _orgId: string, userId: string) {
    return Promise.resolve(this.removedRecords.has(userId));
  }
  listAttachmentPaths(userId: string) {
    return Promise.resolve(this.files.get(userId) ?? []);
  }
  removeAttachmentFiles(paths: string[]) {
    if (this.failRemovingFiles) return Promise.reject(new Error("storage is down"));
    this.order.push(`files ${paths.join(",")}`);
    return Promise.resolve();
  }
  eraseUserData(_userId: string, oldEmail: string) {
    this.order.push(`scrub ${oldEmail}`);
    return Promise.resolve();
  }
  replaceEmail(userId: string, placeholder: string) {
    this.order.push(`email ${placeholder}`);
    this.emails.set(userId, placeholder);
    return Promise.resolve();
  }
  disableLogin(userId: string) {
    this.calls.push(`disable ${userId}`);
    return Promise.resolve();
  }
  isMfaRequired() {
    return Promise.resolve(this.mfaRequired);
  }
  deleteFactors(userId: string) {
    const count = this.factors.get(userId) ?? 0;
    this.factors.delete(userId);
    return Promise.resolve(count);
  }
  recordAudit(_token: string, _orgId: string, action: string, targetUserId: string) {
    this.audits.push(`${action} ${targetUserId}`);
    return Promise.resolve();
  }
}

function post(body: unknown, token: string | null = TOKEN, method = "POST"): Request {
  const headers = new Headers({ "Content-Type": "application/json" });
  if (token) headers.set("Authorization", `Bearer ${token}`);
  return new Request("http://localhost/admin-users", {
    method,
    headers,
    body: method === "POST" ? JSON.stringify(body) : undefined,
  });
}

function handle(api: FakeApi, request: Request) {
  let counter = 0;
  return handleAdminRequest(request, { api, generatePassword: () => `password-${++counter}` });
}

Deno.test("refuses anything but POST, a missing token and a token that is not valid", async () => {
  const api = new FakeApi();
  assertEquals((await handle(api, post({}, TOKEN, "GET"))).status, 405);
  assertEquals((await handle(api, post({ action: "create_users" }, null))).status, 401);
  assertEquals(
    (await handle(api, post({ action: "reset_password", org_id: ORG, user_id: MEMBER }, "wrong"))).status,
    401,
  );
});

Deno.test("refuses a request that is not well formed", async () => {
  const api = new FakeApi();
  assertEquals((await handle(api, post({ action: "nothing", org_id: ORG }))).status, 400);
  assertEquals((await handle(api, post({ action: "create_users", org_id: "x", users: [{}] }))).status, 400);
  assertEquals((await handle(api, post({ action: "create_users", org_id: ORG, users: [] }))).status, 400);
  assertEquals((await handle(api, post({ action: "remove_member", org_id: ORG, user_id: "nope" }))).status, 400);
});

Deno.test("only an admin of the organization may use it", async () => {
  const api = new FakeApi();
  api.roles.set(ADMIN, "staff");

  const response = await handle(api, post({ action: "reset_password", org_id: ORG, user_id: MEMBER }));

  assertEquals(response.status, 403);
  assertEquals(api.passwords.size, 0);
});

Deno.test("creates users with one time passwords and never returns them in an error", async () => {
  const api = new FakeApi();
  const response = await handle(
    api,
    post({
      action: "create_users",
      org_id: ORG,
      users: [{ email: "Ana@School.Example", display_name: "Ana Cruz", role: "member" }, {
        email: "ben@school.example",
        role: "staff",
      }],
    }),
  );

  assertEquals(response.status, 200);
  assertEquals(response.headers.get("Cache-Control"), "no-store");
  const { results } = await response.json();
  assertEquals(results.map((result: { status: string }) => result.status), ["created", "created"]);
  assertEquals(results[0].email, "ana@school.example");
  assertEquals(results[0].temporary_password, "password-1");
  assertEquals(results[1].temporary_password, "password-2");
  assertEquals(api.roles.get(results[1].user_id), "staff");
});

Deno.test("reports each problem row without stopping the others", async () => {
  const api = new FakeApi();
  api.takenEmails.add("taken@school.example");
  const response = await handle(
    api,
    post({
      action: "create_users",
      org_id: ORG,
      users: [
        { email: "not an email", role: "member" },
        { email: "x@school.example", role: "principal" },
        { email: "taken@school.example", role: "member" },
        { email: "ok@school.example", role: "member" },
        { email: "OK@school.example", role: "member" },
      ],
    }),
  );

  const { results } = await response.json();
  assertEquals(
    results.map((result: { status: string; reason?: string }) => result.reason ?? result.status),
    ["invalid_email", "invalid_role", "email_taken", "created", "duplicate"],
  );
});

Deno.test("removes the login again when the membership cannot be added", async () => {
  const api = new FakeApi();
  api.failAddingFor.add("created-1");

  const response = await handle(
    api,
    post({ action: "create_users", org_id: ORG, users: [{ email: "ana@school.example", role: "member" }] }),
  );

  const { results } = await response.json();
  assertEquals(results[0].status, "failed");
  assertEquals(results[0].temporary_password, undefined);
  assertEquals(api.calls, ["create ana@school.example", "delete created-1"]);
});

Deno.test("resets the password of a member and records it", async () => {
  const api = new FakeApi();

  const response = await handle(api, post({ action: "reset_password", org_id: ORG, user_id: MEMBER }));

  assertEquals(response.status, 200);
  assertEquals((await response.json()).temporary_password, "password-1");
  assertEquals(api.passwords.get(MEMBER), "password-1");
  assertEquals(api.audits, [`member.password_reset ${MEMBER}`]);
});

Deno.test("does not reset the password of the caller or of a stranger", async () => {
  const api = new FakeApi();
  assertEquals((await handle(api, post({ action: "reset_password", org_id: ORG, user_id: ADMIN }))).status, 400);
  const stranger = "20000000-0000-4000-8000-000000000b09";
  assertEquals((await handle(api, post({ action: "reset_password", org_id: ORG, user_id: stranger }))).status, 404);
  assertEquals(api.passwords.size, 0);
});

Deno.test("removing the only organization of a member also disables the login", async () => {
  const api = new FakeApi();

  const response = await handle(api, post({ action: "remove_member", org_id: ORG, user_id: MEMBER }));

  assertEquals(await response.json(), { removed: true, login_disabled: true });
  assertEquals(api.calls, [`disable ${MEMBER}`]);
  assertEquals(api.audits, [`member.login_disabled ${MEMBER}`]);
});

Deno.test("a member who belongs to another organization keeps the login", async () => {
  const api = new FakeApi();
  api.memberships.set(MEMBER, 2);

  const response = await handle(api, post({ action: "remove_member", org_id: ORG, user_id: MEMBER }));

  assertEquals(await response.json(), { removed: true, login_disabled: false });
  assertEquals(api.calls, []);
});

Deno.test("an admin cannot remove themselves", async () => {
  const api = new FakeApi();

  assertEquals((await handle(api, post({ action: "remove_member", org_id: ORG, user_id: ADMIN }))).status, 400);
  assertEquals(api.roles.get(ADMIN), "org_admin");
});

Deno.test("an admin can remove another admin", async () => {
  const api = new FakeApi();
  const otherAdmin = "20000000-0000-4000-8000-000000000a02";
  api.roles.set(otherAdmin, "org_admin");

  assertEquals((await handle(api, post({ action: "remove_member", org_id: ORG, user_id: otherAdmin }))).status, 200);
});

Deno.test("refuses to remove an admin when no other admin would be left", async () => {
  const api = new FakeApi();
  const otherAdmin = "20000000-0000-4000-8000-000000000a02";
  api.roles.set(otherAdmin, "org_admin");
  api.countAdmins = () => Promise.resolve(1);

  const response = await handle(api, post({ action: "remove_member", org_id: ORG, user_id: otherAdmin }));

  assertEquals(response.status, 409);
  assertEquals(api.roles.get(otherAdmin), "org_admin");
});

function tokenAt(level: string): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=+$/, "");
  return `${encode({ alg: "HS256" })}.${encode({ aal: level })}.signature`;
}

Deno.test("an account that must verify a second factor needs an aal2 session", async () => {
  const api = new FakeApi();
  api.mfaRequired = true;
  api.validTokens.add(tokenAt("aal1")).add(tokenAt("aal2"));
  const request = { action: "reset_password", org_id: ORG, user_id: MEMBER };

  const first = await handle(api, post(request, tokenAt("aal1")));
  assertEquals(first.status, 403);
  assertEquals((await first.json()).error, "mfa_required");
  assertEquals(api.passwords.size, 0);

  assertEquals((await handle(api, post(request, tokenAt("aal2")))).status, 200);
});

Deno.test("an administrator without a required second factor works at aal1", async () => {
  const api = new FakeApi();
  api.validTokens.add(tokenAt("aal1"));

  const response = await handle(api, post({ action: "reset_password", org_id: ORG, user_id: MEMBER }, tokenAt("aal1")));

  assertEquals(response.status, 200);
});

Deno.test("resets the two-step verification of a member and records it", async () => {
  const api = new FakeApi();
  api.factors.set(MEMBER, 1);

  const response = await handle(api, post({ action: "reset_mfa", org_id: ORG, user_id: MEMBER }));

  assertEquals(response.status, 200);
  assertEquals((await response.json()).factors_removed, 1);
  assertEquals(api.factors.has(MEMBER), false);
  assertEquals(api.audits, [`member.mfa_reset ${MEMBER}`]);
});

Deno.test("an administrator cannot reset their own two-step verification", async () => {
  const api = new FakeApi();
  api.factors.set(ADMIN, 1);

  const response = await handle(api, post({ action: "reset_mfa", org_id: ORG, user_id: ADMIN }));

  assertEquals(response.status, 400);
  assertEquals(api.factors.get(ADMIN), 1);
  assertEquals(api.audits, []);
});

Deno.test("resetting the two-step verification of a non member is refused", async () => {
  const api = new FakeApi();

  const response = await handle(
    api,
    post({ action: "reset_mfa", org_id: ORG, user_id: "20000000-0000-4000-8000-000000000b01" }),
  );

  assertEquals(response.status, 404);
});

const RETURNING = "20000000-0000-4000-8000-000000000a07";

function returning(api: FakeApi, overrides: Partial<{ isDisabled: boolean; membershipCount: number }> = {}) {
  api.takenEmails.add("back@school.example");
  api.logins.set("back@school.example", { userId: RETURNING, isDisabled: true, membershipCount: 0, ...overrides });
}

function addBack(api: FakeApi) {
  return handle(
    api,
    post({ action: "create_users", org_id: ORG, users: [{ email: "back@school.example", role: "member" }] }),
  );
}

Deno.test("a person who left comes back with a new temporary password", async () => {
  const api = new FakeApi();
  returning(api);

  const { results } = await (await addBack(api)).json();

  assertEquals(results[0].status, "returned");
  assertEquals(results[0].user_id, RETURNING);
  assertEquals(results[0].temporary_password, "password-1");
  assertEquals(api.reactivated, [RETURNING]);
  assertEquals(api.roles.get(RETURNING), "member");
});

Deno.test("an email that belongs to someone active, or to another organization, stays refused", async () => {
  for (const overrides of [{ isDisabled: false }, { membershipCount: 1 }]) {
    const api = new FakeApi();
    returning(api, overrides);

    const { results } = await (await addBack(api)).json();

    assertEquals(results[0].status, "email_taken");
    assertEquals(api.reactivated, []);
  }
  const unknown = new FakeApi();
  unknown.takenEmails.add("back@school.example");
  assertEquals((await (await addBack(unknown)).json()).results[0].status, "email_taken");
});

Deno.test("a returning person is disabled again when adding them fails", async () => {
  const api = new FakeApi();
  returning(api);
  api.failAddingFor.add(RETURNING);

  const { results } = await (await addBack(api)).json();

  assertEquals(results[0].status, "failed");
  assertEquals(api.calls.includes(`disable ${RETURNING}`), true);
});

function erase(api: FakeApi, userId = MEMBER) {
  return handle(api, post({ action: "erase_member", org_id: ORG, user_id: userId }));
}

Deno.test("erasing removes the member, scrubs the database, then replaces the email, in that order", async () => {
  const api = new FakeApi();

  const response = await erase(api);

  assertEquals(response.status, 200);
  assertEquals(await response.json(), { erased: true, messages: "kept" });
  assertEquals(api.order, ["remove", "scrub ana@school.example", `email ${placeholderEmail(MEMBER)}`]);
  assertEquals(api.audits.includes(`member.erased ${MEMBER}`), true);
});

Deno.test("a server that blanks messages says so", async () => {
  const api = new FakeApi();
  api.policy = "blank_messages";

  assertEquals((await (await erase(api)).json()).messages, "blanked");
});

Deno.test("nothing is erased when the server turned erasure off", async () => {
  const api = new FakeApi();
  api.policy = "off";

  const response = await erase(api);

  assertEquals(response.status, 409);
  assertEquals((await response.json()).error, "erasure_off");
  assertEquals(api.order, []);
});

Deno.test("an administrator cannot erase themselves", async () => {
  const api = new FakeApi();

  const response = await erase(api, ADMIN);

  assertEquals(response.status, 400);
  assertEquals((await response.json()).error, "cannot_erase_self");
  assertEquals(api.order, []);
});

Deno.test("a person who belongs to another organization is only removed, never erased", async () => {
  const api = new FakeApi();
  api.memberships.set(MEMBER, 2);

  const response = await erase(api);

  assertEquals(response.status, 409);
  assertEquals((await response.json()).error, "member_of_other_organizations");
  assertEquals(api.order, []);
});

Deno.test("someone who was never in the organization cannot be erased", async () => {
  const api = new FakeApi();
  const stranger = "20000000-0000-4000-8000-000000000b01";
  api.emails.set(stranger, "stranger@other.example");

  assertEquals((await erase(api, stranger)).status, 404);
  assertEquals(api.order, []);
});

Deno.test("a person removed earlier can be erased later, and a half finished erase can be finished", async () => {
  const api = new FakeApi();
  api.roles.delete(MEMBER);
  api.memberships.set(MEMBER, 0);
  api.removedRecords.add(MEMBER);

  const response = await erase(api);

  assertEquals(response.status, 200);
  assertEquals(api.order, ["scrub ana@school.example", `email ${placeholderEmail(MEMBER)}`]);
});

Deno.test("erasing someone who is already erased changes nothing", async () => {
  const api = new FakeApi();
  api.roles.delete(MEMBER);
  api.memberships.set(MEMBER, 0);
  api.removedRecords.add(MEMBER);
  api.emails.set(MEMBER, placeholderEmail(MEMBER));

  const response = await erase(api);

  assertEquals(response.status, 200);
  assertEquals(api.order, []);
  assertEquals(api.audits, []);
});

Deno.test("a server that blanks messages removes the files of the person before it scrubs the database", async () => {
  const api = new FakeApi();
  api.policy = "blank_messages";
  api.files.set(MEMBER, ["conversation/message/report", "conversation/message/photo"]);

  const response = await erase(api);

  assertEquals(response.status, 200);
  assertEquals(api.order, [
    "remove",
    "files conversation/message/report,conversation/message/photo",
    "scrub ana@school.example",
    `email ${placeholderEmail(MEMBER)}`,
  ]);
});

Deno.test("a server that keeps messages leaves the files alone", async () => {
  const api = new FakeApi();
  api.files.set(MEMBER, ["conversation/message/report"]);

  await erase(api);

  assertEquals(api.order.some((step) => step.startsWith("files")), false);
});

Deno.test("when the files cannot be deleted nothing else is scrubbed, so it can be run again", async () => {
  const api = new FakeApi();
  api.policy = "blank_messages";
  api.failRemovingFiles = true;

  await assertRejects(() => erase(api));

  assertEquals(api.order, ["remove"]);
  api.failRemovingFiles = false;
  api.roles.delete(MEMBER);
  api.memberships.set(MEMBER, 0);
  api.removedRecords.add(MEMBER);
  assertEquals((await erase(api)).status, 200);
  assertEquals(api.order.at(-1), `email ${placeholderEmail(MEMBER)}`);
});
