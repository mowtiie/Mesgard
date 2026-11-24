import { assuranceLevel } from "./assurance.ts";
import { isErasedEmail, placeholderEmail } from "./erasure.ts";
import { generateTemporaryPassword } from "./passwords.ts";
import { type AdminRequest, parseRequest, type Role, validateNewUser } from "./validation.ts";

/** Everything the handler needs from the outside world, so the rules can be tested without a server. */
export interface AdminApi {
  /** The user a token belongs to, or null when the token is not valid. */
  getUserId(token: string): Promise<string | null>;
  /** The role of a member of the organization, read with the caller's own token, or null for a non member. */
  getMemberRole(token: string, orgId: string, userId: string): Promise<Role | null>;
  countAdmins(token: string, orgId: string): Promise<number>;
  createAuthUser(input: { email: string; password: string; displayName: string }): Promise<string | "email_taken">;
  deleteAuthUser(userId: string): Promise<void>;
  /** Writes with the caller's token, so Row Level Security and the audit triggers see the admin. */
  addMember(token: string, orgId: string, userId: string, role: Role): Promise<boolean>;
  removeMember(token: string, orgId: string, userId: string): Promise<boolean>;
  countMemberships(userId: string): Promise<number>;
  setTemporaryPassword(userId: string, password: string): Promise<void>;
  disableLogin(userId: string): Promise<void>;
  recordAudit(token: string, orgId: string, action: string, targetUserId: string): Promise<void>;
  /** Whether the account of the token must verify a second factor, answered by the database with the caller's token. */
  isMfaRequired(token: string): Promise<boolean>;
  /** Removes every factor of a login and returns how many there were. Auth ends the sessions of a verified factor. */
  deleteFactors(userId: string): Promise<number>;
  /** A login by its email, or null. Used to recognise a person who left and comes back. */
  findLogin(email: string): Promise<{ userId: string; isDisabled: boolean; membershipCount: number } | null>;
  /** Turns a disabled login on again with a new temporary password the person must replace. */
  reactivateLogin(userId: string, password: string): Promise<void>;
  /** What this server does when someone is erased: "off", "keep_messages" or "blank_messages". */
  erasurePolicy(token: string): Promise<string>;
  /** The email of a login, read with the service role. */
  getLoginEmail(userId: string): Promise<string | null>;
  /** True when the audit log shows that this organization removed the person. */
  wasRemovedFrom(token: string, orgId: string, userId: string): Promise<boolean>;
  /** True when a legal hold or an open report protects the person, so nothing about them may be erased. */
  hasLegalHold(userId: string): Promise<boolean>;
  /** The storage paths of every file the person uploaded, attached to a message or not. */
  listAttachmentPaths(userId: string): Promise<string[]>;
  /** Deletes files from Storage, which SQL cannot do. Files that are already gone are not an error. */
  removeAttachmentFiles(paths: string[]): Promise<void>;
  /** The storage paths of the profile pictures of the person, without those a report keeps as evidence. */
  listAvatarPaths(userId: string): Promise<string[]>;
  /** Deletes profile pictures from Storage. Files that are already gone are not an error. */
  removeAvatarFiles(paths: string[]): Promise<void>;
  /** Scrubs the database side of a person who is in no organization. */
  eraseUserData(userId: string, oldEmail: string): Promise<void>;
  /** Replaces the email of a login with a placeholder and keeps the login disabled. */
  replaceEmail(userId: string, placeholder: string): Promise<void>;
}

export interface HandlerDependencies {
  api: AdminApi;
  generatePassword?: () => string;
}

type UserResult =
  | { email: string; status: "created" | "returned"; user_id: string; temporary_password: string }
  | { email: string; status: "email_taken" | "duplicate" | "failed" }
  | { email: string; status: "invalid"; reason: string };

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
  });
}

function bearerToken(request: Request): string | null {
  const match = /^Bearer (.+)$/i.exec(request.headers.get("Authorization") ?? "");
  return match ? match[1] : null;
}

export async function handleAdminRequest(request: Request, dependencies: HandlerDependencies): Promise<Response> {
  if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const token = bearerToken(request);
  if (!token) return json({ error: "unauthorized" }, 401);

  const body = await request.json().catch(() => null);
  const parsed = parseRequest(body);
  if ("error" in parsed) return json({ error: "invalid_request", message: parsed.error }, 400);

  const { api } = dependencies;
  const callerId = await api.getUserId(token);
  if (!callerId) return json({ error: "unauthorized" }, 401);
  if (await api.isMfaRequired(token) && assuranceLevel(token) !== "aal2") {
    return json({ error: "mfa_required" }, 403);
  }
  if (await api.getMemberRole(token, parsed.request.orgId, callerId) !== "org_admin") {
    return json({ error: "forbidden" }, 403);
  }
  return await dispatch(parsed.request, token, callerId, dependencies);
}

function dispatch(
  request: AdminRequest,
  token: string,
  callerId: string,
  dependencies: HandlerDependencies,
): Promise<Response> {
  const generatePassword = dependencies.generatePassword ?? generateTemporaryPassword;
  switch (request.action) {
    case "create_users":
      return createUsers(request, token, dependencies.api, generatePassword);
    case "reset_password":
      return resetPassword(request, token, callerId, dependencies.api, generatePassword);
    case "remove_member":
      return removeMember(request, token, callerId, dependencies.api);
    case "reset_mfa":
      return resetMfa(request, token, callerId, dependencies.api);
    case "erase_member":
      return eraseMember(request, token, callerId, dependencies.api);
  }
}

async function createUsers(
  request: Extract<AdminRequest, { action: "create_users" }>,
  token: string,
  api: AdminApi,
  generatePassword: () => string,
): Promise<Response> {
  const seen = new Set<string>();
  const results: UserResult[] = [];
  for (const input of request.users) {
    const check = validateNewUser(input);
    if ("reason" in check) {
      results.push({ email: emailOf(input), status: "invalid", reason: check.reason });
      continue;
    }
    const { user } = check;
    if (seen.has(user.email)) {
      results.push({ email: user.email, status: "duplicate" });
      continue;
    }
    seen.add(user.email);
    results.push(await createUser(user, request.orgId, token, api, generatePassword()));
  }
  return json({ results });
}

function emailOf(input: unknown): string {
  const email = (input as { email?: unknown } | null)?.email;
  return typeof email === "string" ? email : "";
}

async function createUser(
  user: { email: string; displayName: string; role: Role },
  orgId: string,
  token: string,
  api: AdminApi,
  password: string,
): Promise<UserResult> {
  try {
    const created = await api.createAuthUser({ email: user.email, password, displayName: user.displayName });
    if (created === "email_taken") return await returningPerson(user, orgId, token, api, password);
    if (await api.addMember(token, orgId, created, user.role)) {
      return { email: user.email, status: "created", user_id: created, temporary_password: password };
    }
    await api.deleteAuthUser(created);
  } catch (error) {
    console.error("Creating a user failed", error instanceof Error ? error.message : "unknown error");
  }
  return { email: user.email, status: "failed" };
}

/**
 * A person who was removed and comes back has a disabled login and no organization. They get it back with a new
 * temporary password. An email that belongs to someone active somewhere else stays refused.
 */
async function returningPerson(
  user: { email: string; role: Role },
  orgId: string,
  token: string,
  api: AdminApi,
  password: string,
): Promise<UserResult> {
  const login = await api.findLogin(user.email);
  if (!login || !login.isDisabled || login.membershipCount > 0) return { email: user.email, status: "email_taken" };
  await api.reactivateLogin(login.userId, password);
  if (await api.addMember(token, orgId, login.userId, user.role)) {
    return { email: user.email, status: "returned", user_id: login.userId, temporary_password: password };
  }
  await api.disableLogin(login.userId);
  return { email: user.email, status: "failed" };
}

async function resetPassword(
  request: Extract<AdminRequest, { action: "reset_password" }>,
  token: string,
  callerId: string,
  api: AdminApi,
  generatePassword: () => string,
): Promise<Response> {
  if (request.userId === callerId) return json({ error: "cannot_reset_own_password" }, 400);
  if (await api.getMemberRole(token, request.orgId, request.userId) === null) {
    return json({ error: "not_a_member" }, 404);
  }
  const password = generatePassword();
  await api.setTemporaryPassword(request.userId, password);
  await api.recordAudit(token, request.orgId, "member.password_reset", request.userId);
  return json({ temporary_password: password });
}

async function removeMember(
  request: Extract<AdminRequest, { action: "remove_member" }>,
  token: string,
  callerId: string,
  api: AdminApi,
): Promise<Response> {
  if (request.userId === callerId) return json({ error: "cannot_remove_self" }, 400);
  const role = await api.getMemberRole(token, request.orgId, request.userId);
  if (role === null) return json({ error: "not_a_member" }, 404);
  if (role === "org_admin" && await api.countAdmins(token, request.orgId) <= 1) {
    return json({ error: "last_admin" }, 409);
  }
  if (!await api.removeMember(token, request.orgId, request.userId)) return json({ error: "failed" }, 500);

  // A login that belongs to another organization as well stays active.
  const loginDisabled = await api.countMemberships(request.userId) === 0;
  if (loginDisabled) {
    await api.disableLogin(request.userId);
    await api.recordAudit(token, request.orgId, "member.login_disabled", request.userId);
  }
  return json({ removed: true, login_disabled: loginDisabled });
}

async function resetMfa(
  request: Extract<AdminRequest, { action: "reset_mfa" }>,
  token: string,
  callerId: string,
  api: AdminApi,
): Promise<Response> {
  if (request.userId === callerId) return json({ error: "cannot_reset_own_mfa" }, 400);
  if (await api.getMemberRole(token, request.orgId, request.userId) === null) {
    return json({ error: "not_a_member" }, 404);
  }
  const removed = await api.deleteFactors(request.userId);
  await api.recordAudit(token, request.orgId, "member.mfa_reset", request.userId);
  return json({ factors_removed: removed });
}

/**
 * Removes the person from the organization when they are still in it, erases what the database holds about them, and
 * replaces the email of the login. Every step can be repeated, and a person this organization already removed can be
 * erased later, so a failure halfway is finished by running it again.
 */
async function eraseMember(
  request: Extract<AdminRequest, { action: "erase_member" }>,
  token: string,
  callerId: string,
  api: AdminApi,
): Promise<Response> {
  const policy = await api.erasurePolicy(token);
  if (policy === "off") return json({ error: "erasure_off" }, 409);
  if (request.userId === callerId) return json({ error: "cannot_erase_self" }, 400);
  // Asked first, before the person is removed or any file is deleted, because none of that can be taken back.
  if (await api.hasLegalHold(request.userId)) return json({ error: "legal_hold" }, 409);

  const refusal = await removeBeforeErasing(request, token, api);
  if (refusal) return refusal;

  const email = await api.getLoginEmail(request.userId);
  if (email !== null && !isErasedEmail(email)) {
    // The files go first: the database still knows where they are, so a failure here can be repeated.
    if (policy === "blank_messages") await api.removeAttachmentFiles(await api.listAttachmentPaths(request.userId));
    const pictures = await api.listAvatarPaths(request.userId);
    if (pictures.length > 0) await api.removeAvatarFiles(pictures);
    await api.eraseUserData(request.userId, email);
    await api.replaceEmail(request.userId, placeholderEmail(request.userId));
    await api.recordAudit(token, request.orgId, "member.erased", request.userId);
  }
  return json({ erased: true, messages: policy === "blank_messages" ? "blanked" : "kept" });
}

async function removeBeforeErasing(
  request: Extract<AdminRequest, { action: "erase_member" }>,
  token: string,
  api: AdminApi,
): Promise<Response | null> {
  const role = await api.getMemberRole(token, request.orgId, request.userId);
  if (role === null) {
    const isFormerMember = await api.countMemberships(request.userId) === 0 &&
      await api.wasRemovedFrom(token, request.orgId, request.userId);
    return isFormerMember ? null : json({ error: "not_a_member" }, 404);
  }
  if (role === "org_admin" && await api.countAdmins(token, request.orgId) <= 1) {
    return json({ error: "last_admin" }, 409);
  }
  if (await api.countMemberships(request.userId) > 1) return json({ error: "member_of_other_organizations" }, 409);
  return await api.removeMember(token, request.orgId, request.userId) ? null : json({ error: "failed" }, 500);
}
