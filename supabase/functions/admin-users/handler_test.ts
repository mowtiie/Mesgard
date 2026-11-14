import { assertEquals } from "jsr:@std/assert@1";
import { type AdminApi, handleAdminRequest } from "./handler.ts";
import type { Role } from "./validation.ts";

const ORG = "10000000-0000-4000-8000-000000000a00";
const ADMIN = "20000000-0000-4000-8000-000000000a01";
const MEMBER = "20000000-0000-4000-8000-000000000a04";
const TOKEN = "admin-token";

class FakeApi implements AdminApi {
  roles = new Map<string, Role>([[ADMIN, "org_admin"], [MEMBER, "student"]]);
  takenEmails = new Set<string>();
  memberships = new Map<string, number>([[MEMBER, 1]]);
  failAddingFor = new Set<string>();
  calls: string[] = [];
  audits: string[] = [];
  passwords = new Map<string, string>();
  private nextId = 1;

  getUserId(token: string) {
    return Promise.resolve(token === TOKEN ? ADMIN : null);
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
  disableLogin(userId: string) {
    this.calls.push(`disable ${userId}`);
    return Promise.resolve();
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
      users: [{ email: "Ana@School.Example", display_name: "Ana Cruz", role: "student" }, {
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
        { email: "not an email", role: "student" },
        { email: "x@school.example", role: "principal" },
        { email: "taken@school.example", role: "student" },
        { email: "ok@school.example", role: "student" },
        { email: "OK@school.example", role: "student" },
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
    post({ action: "create_users", org_id: ORG, users: [{ email: "ana@school.example", role: "student" }] }),
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
