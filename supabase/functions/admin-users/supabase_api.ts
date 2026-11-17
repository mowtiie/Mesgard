import type { AdminApi } from "./handler.ts";
import type { Role } from "./validation.ts";

const LOGIN_DISABLED_DURATION = "876000h";

export interface SupabaseApiSettings {
  url: string;
  anonKey: string;
  serviceRoleKey: string;
}

/** Plain fetch calls to Auth and the REST API, so the function has no dependencies to download. */
export function createSupabaseApi({ url, anonKey, serviceRoleKey }: SupabaseApiSettings): AdminApi {
  const asUser = (token: string, extra: Record<string, string> = {}) => ({
    apikey: anonKey,
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
    ...extra,
  });
  const asService = {
    apikey: serviceRoleKey,
    Authorization: `Bearer ${serviceRoleKey}`,
    "Content-Type": "application/json",
  };
  const memberFilter = (orgId: string, userId: string) =>
    `org_id=eq.${encodeURIComponent(orgId)}&user_id=eq.${encodeURIComponent(userId)}`;

  async function expectOk(response: Response, what: string): Promise<Response> {
    if (!response.ok) throw new Error(`${what} failed with status ${response.status}`);
    return response;
  }

  return {
    async getUserId(token) {
      const response = await fetch(`${url}/auth/v1/user`, { headers: asUser(token) });
      return response.ok ? (await response.json()).id ?? null : null;
    },

    async getMemberRole(token, orgId, userId) {
      const response = await expectOk(
        await fetch(`${url}/rest/v1/org_members?${memberFilter(orgId, userId)}&select=role`, {
          headers: asUser(token),
        }),
        "Reading a member",
      );
      const rows: { role: Role }[] = await response.json();
      return rows[0]?.role ?? null;
    },

    async countAdmins(token, orgId) {
      const response = await expectOk(
        await fetch(
          `${url}/rest/v1/org_members?org_id=eq.${encodeURIComponent(orgId)}&role=eq.org_admin&select=user_id`,
          {
            headers: asUser(token),
          },
        ),
        "Counting admins",
      );
      return (await response.json()).length;
    },

    async createAuthUser({ email, password, displayName }) {
      const response = await fetch(`${url}/auth/v1/admin/users`, {
        method: "POST",
        headers: asService,
        body: JSON.stringify({
          email,
          password,
          email_confirm: true,
          user_metadata: { display_name: displayName },
          app_metadata: { must_change_password: true },
        }),
      });
      if (response.status === 422 && (await response.json().catch(() => ({}))).error_code === "email_exists") {
        return "email_taken";
      }
      return (await (await expectOk(response, "Creating a login")).json()).id;
    },

    async deleteAuthUser(userId) {
      await expectOk(
        await fetch(`${url}/auth/v1/admin/users/${userId}`, { method: "DELETE", headers: asService }),
        "Deleting a login",
      );
    },

    async addMember(token, orgId, userId, role) {
      const response = await fetch(`${url}/rest/v1/org_members`, {
        method: "POST",
        headers: asUser(token),
        body: JSON.stringify({ org_id: orgId, user_id: userId, role }),
      });
      return response.ok;
    },

    async removeMember(token, orgId, userId) {
      const response = await expectOk(
        await fetch(`${url}/rest/v1/org_members?${memberFilter(orgId, userId)}`, {
          method: "DELETE",
          headers: asUser(token, { Prefer: "return=representation" }),
        }),
        "Removing a member",
      );
      return (await response.json()).length > 0;
    },

    async countMemberships(userId) {
      const response = await expectOk(
        await fetch(`${url}/rest/v1/org_members?user_id=eq.${encodeURIComponent(userId)}&select=org_id`, {
          headers: asService,
        }),
        "Counting memberships",
      );
      return (await response.json()).length;
    },

    async setTemporaryPassword(userId, password) {
      await expectOk(
        await fetch(`${url}/auth/v1/admin/users/${userId}`, {
          method: "PUT",
          headers: asService,
          body: JSON.stringify({ password, app_metadata: { must_change_password: true } }),
        }),
        "Setting a password",
      );
    },

    async disableLogin(userId) {
      await expectOk(
        await fetch(`${url}/auth/v1/admin/users/${userId}`, {
          method: "PUT",
          headers: asService,
          body: JSON.stringify({ ban_duration: LOGIN_DISABLED_DURATION }),
        }),
        "Disabling a login",
      );
    },

    async isMfaRequired(token) {
      const response = await expectOk(
        await fetch(`${url}/rest/v1/rpc/is_mfa_required`, { method: "POST", headers: asUser(token), body: "{}" }),
        "Reading the two-step verification status",
      );
      return (await response.json()) === true;
    },

    async deleteFactors(userId) {
      const listed = await expectOk(
        await fetch(`${url}/auth/v1/admin/users/${userId}/factors`, { headers: asService }),
        "Listing factors",
      );
      const factors: { id: string }[] = await listed.json();
      for (const factor of factors) {
        await expectOk(
          await fetch(`${url}/auth/v1/admin/users/${userId}/factors/${factor.id}`, {
            method: "DELETE",
            headers: asService,
          }),
          "Deleting a factor",
        );
      }
      return factors.length;
    },

    async recordAudit(token, orgId, action, targetUserId) {
      await expectOk(
        await fetch(`${url}/rest/v1/audit_logs`, {
          method: "POST",
          headers: asUser(token),
          body: JSON.stringify({ org_id: orgId, action, target_type: "user", target_id: targetUserId }),
        }),
        "Recording an audit entry",
      );
    },
  };
}
