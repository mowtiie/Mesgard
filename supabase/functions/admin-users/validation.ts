export const ROLES = ["org_admin", "staff", "student"] as const;
export type Role = typeof ROLES[number];

export const MAX_USERS_PER_REQUEST = 200;
const MAX_EMAIL_LENGTH = 254;
const MAX_DISPLAY_NAME_LENGTH = 100;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export interface NewUser {
  email: string;
  displayName: string;
  role: Role;
}

export type AdminRequest =
  | { action: "create_users"; orgId: string; users: unknown[] }
  | { action: "reset_password"; orgId: string; userId: string }
  | { action: "remove_member"; orgId: string; userId: string }
  | { action: "reset_mfa"; orgId: string; userId: string };

export type ParseResult = { request: AdminRequest } | { error: string };

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isUuid(value: unknown): value is string {
  return typeof value === "string" && UUID_PATTERN.test(value);
}

/** Checks the shape of a request. Each user of a batch is checked later, so one bad row does not stop the others. */
export function parseRequest(body: unknown): ParseResult {
  if (!isRecord(body)) return { error: "The request must be a JSON object" };
  if (!isUuid(body.org_id)) return { error: "org_id must be a UUID" };
  const orgId = body.org_id;
  switch (body.action) {
    case "create_users":
      if (!Array.isArray(body.users) || body.users.length === 0) return { error: "users must be a non empty array" };
      if (body.users.length > MAX_USERS_PER_REQUEST) {
        return { error: `At most ${MAX_USERS_PER_REQUEST} users per request` };
      }
      return { request: { action: "create_users", orgId, users: body.users } };
    case "reset_password":
    case "remove_member":
    case "reset_mfa":
      if (!isUuid(body.user_id)) return { error: "user_id must be a UUID" };
      return { request: { action: body.action, orgId, userId: body.user_id } };
    default:
      return { error: "Unknown action" };
  }
}

export type UserCheck = { user: NewUser } | { reason: string };

export function validateNewUser(input: unknown): UserCheck {
  if (!isRecord(input)) return { reason: "invalid_user" };
  const email = typeof input.email === "string" ? input.email.trim().toLowerCase() : "";
  if (email.length > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.test(email)) return { reason: "invalid_email" };
  const role = input.role;
  if (!ROLES.includes(role as Role)) return { reason: "invalid_role" };
  const given = typeof input.display_name === "string" ? input.display_name.trim() : "";
  const displayName = given || email.split("@")[0];
  if (displayName.length > MAX_DISPLAY_NAME_LENGTH) return { reason: "invalid_display_name" };
  return { user: { email, displayName, role: role as Role } };
}
