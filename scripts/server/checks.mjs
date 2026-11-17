import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

import { checkPushFunction } from "./push.mjs";

const MIGRATION_VERSION = /^(\d{14})_.+\.sql$/;
const ALLOWED_FUNCTION_ANSWERS = [400, 401, 403];

export const PASS = "pass";
export const WARN = "warn";
export const FAIL = "fail";
export const INFO = "info";

export function localMigrationVersions(repositoryRoot) {
  return readdirSync(join(repositoryRoot, "supabase", "migrations"))
    .map((name) => MIGRATION_VERSION.exec(name)?.[1])
    .filter(Boolean)
    .sort();
}

export function missingMigrations(localVersions, remoteVersions) {
  const applied = new Set(remoteVersions);
  return localVersions.filter((version) => !applied.has(version));
}

const result = (status, label, detail = "", hint = "") => ({ status, label, detail, hint });

/**
 * Every check answers with a status instead of throwing, so one broken thing does not hide the rest. [context] carries
 * the server address, the public key, a function that runs read-only SQL and the other readers, which tests replace.
 */
export async function runChecks(context) {
  const settings = await readSettings(context);
  const checks = [
    () => checkReachable(context, settings),
    () => checkSignups(settings),
    () => checkMigrations(context),
    () => checkApiLevel(context),
    () => checkFunction(context),
    () => checkAuthSettings(context),
    () => checkOrganizations(context),
    () => checkEnforcement(context),
    () => checkErasure(context),
    () => checkPush(context),
  ];
  const results = [];
  for (const check of checks) {
    results.push(await attempt(check));
  }
  return results;
}

async function attempt(check) {
  try {
    return await check();
  } catch (error) {
    return result(FAIL, "Check could not run", error.message);
  }
}

async function readSettings({ url, publicKey, fetch }) {
  try {
    const response = await fetch(`${url}/auth/v1/settings`, { headers: { apikey: publicKey } });
    return response.ok ? await response.json() : null;
  } catch {
    return null;
  }
}

function checkReachable({ url, isLocal }, settings) {
  if (!settings) {
    return result(FAIL, "Server reachable", `No answer from ${url}`, "Check the project is running and the address is right.");
  }
  if (!isLocal && !url.startsWith("https://")) {
    return result(FAIL, "Server reachable", "The address is not HTTPS", "Release builds of the app only accept HTTPS.");
  }
  return result(PASS, "Server reachable", url);
}

function checkSignups(settings) {
  if (!settings) return result(WARN, "Public sign-ups are off", "Could not read the Auth settings");
  if (!settings.external?.email) {
    return result(FAIL, "Email sign in enabled", "The Email provider is off", "Enable Email under Authentication, Sign In / Providers.");
  }
  return settings.disable_signup
    ? result(PASS, "Public sign-ups are off")
    : result(WARN, "Public sign-ups are off", "Anyone can create an account", "Run setup to turn sign-ups off.");
}

async function checkMigrations(context) {
  const remote = (await context.query("select version from supabase_migrations.schema_migrations")).map((row) => row.version);
  const missing = missingMigrations(context.localVersions, remote);
  return missing.length === 0
    ? result(PASS, "Database is up to date", `${context.localVersions.length} migrations applied`)
    : result(FAIL, "Database is up to date", `${missing.length} migration(s) missing`, "Run setup to apply them.");
}

/** The level the app needs is read from the app's source, so the check and the app cannot disagree. */
export function requiredApiLevel(repositoryRoot) {
  const source = readFileSync(join(repositoryRoot, "app", "src", "main", "java", "com", "mowtiie", "mesgard", "domain", "server", "ServerCompatibility.kt"), "utf8");
  return Number(/REQUIRED_API_LEVEL = (\d+)/.exec(source)?.[1]);
}

async function checkApiLevel({ url, publicKey, fetch, requiredApiLevel: required }) {
  const label = "Server is new enough for the app";
  const response = await fetch(`${url}/rest/v1/rpc/server_info`, {
    method: "POST",
    headers: { apikey: publicKey, "Content-Type": "application/json" },
    body: "{}",
  });
  if (response.status === 404) {
    return result(FAIL, label, "The server has no server_info function", "Run setup to update the server.");
  }
  const level = (await response.json())?.api_level;
  return level >= required
    ? result(PASS, label, `API level ${level}`)
    : result(FAIL, label, `API level ${level}, the app needs ${required}`, "Run setup to update the server.");
}

async function checkFunction({ url, publicKey, fetch }) {
  const response = await fetch(`${url}/functions/v1/admin-users`, {
    method: "POST",
    headers: { apikey: publicKey, "Content-Type": "application/json" },
    body: "{}",
  });
  return ALLOWED_FUNCTION_ANSWERS.includes(response.status)
    ? result(PASS, "Account function deployed", "admin-users")
    : result(FAIL, "Account function deployed", `admin-users answered ${response.status}`, "Run setup to deploy it.");
}

async function checkAuthSettings({ readAuthDifferences }) {
  if (!readAuthDifferences) return result(INFO, "Auth settings match", "Skipped for the local stack");
  const differences = await readAuthDifferences();
  return differences.length === 0
    ? result(PASS, "Auth settings match", "Sign-ups off, TOTP enabled")
    : result(WARN, "Auth settings match", differences.join("; "), "Run setup to apply them.");
}

async function checkOrganizations({ query }) {
  const [counts] = await query(
    "select (select count(*) from public.organizations)::int as organizations, " +
      "(select count(*) from public.org_members where role = 'org_admin')::int as admins",
  );
  if (counts.organizations === 0) {
    return result(FAIL, "Organization and administrator exist", "No organization yet", "Run setup to create the first one.");
  }
  if (counts.admins === 0) {
    return result(FAIL, "Organization and administrator exist", "No administrator", "Add one as described in README.md, Setting up the backend by hand.");
  }
  return result(PASS, "Organization and administrator exist", `${counts.organizations} organization(s), ${counts.admins} administrator(s)`);
}

async function checkEnforcement({ query }) {
  const label = TWO_STEP_LABEL;
  let row;
  try {
    [row] = await query("select enforced from private.mfa_settings");
  } catch {
    return result(INFO, label, "Unavailable until the database is up to date");
  }
  return row?.enforced
    ? result(PASS, label, "Required")
    : result(INFO, label, "Off, a password alone is enough", "Turn it on with: update private.mfa_settings set enforced = true; (README.md, Two-step verification)");
}

const TWO_STEP_LABEL = "Two-step verification for administrators and staff";

const ERASURE_MODES = {
  keep_messages: "Messages stay as Former member",
  blank_messages: "Messages are erased too",
  off: "Erasing is not offered",
};

async function checkErasure({ query }) {
  const label = "What erasing a person does on this server";
  let row;
  try {
    [row] = await query("select mode from private.erasure_settings");
  } catch {
    return result(INFO, label, "Unavailable until the database is up to date");
  }
  return result(INFO, label, ERASURE_MODES[row?.mode] ?? "Unknown mode", "See README.md, Removing and erasing people, to change it.");
}

async function checkPush({ query, fetch }) {
  const label = "Push notifications";
  let row;
  try {
    [row] = await query("select function_url, webhook_secret from private.push_settings");
  } catch {
    return result(INFO, label, "Unavailable until the database is up to date");
  }
  if (!row) return result(INFO, label, "Optional, not set up", "Set it up with: node scripts/server/index.mjs push (README.md, Push notifications)");
  let outcome;
  try {
    outcome = await checkPushFunction({ functionUrl: row.function_url, secret: row.webhook_secret, fetch });
  } catch {
    return result(WARN, label, "The push function could not be reached from here");
  }
  return outcome.ok
    ? result(PASS, label, "Working")
    : result(FAIL, label, outcome.reason, "Run the push command again, with --rotate if the secrets differ.");
}

export function hasFailure(results) {
  return results.some((entry) => entry.status === FAIL);
}
