import assert from "node:assert/strict";
import { test } from "node:test";
import { AUTH_SETTINGS_TOML, declaredChanges, describeChange } from "./auth-settings.mjs";
import { parseArgs } from "./args.mjs";
import { FAIL, INFO, PASS, WARN, formatReport, hasFailure, missingMigrations, runChecks } from "./checks.mjs";
import { extractJson, CliError } from "./supabase.mjs";
import { isProjectRef, publicKeyOf, secretKeyOf } from "./target.mjs";

const healthySettings = { external: { email: true }, disable_signup: true };

function context(overrides = {}) {
  const answers = {
    "supabase_migrations.schema_migrations": [{ version: "20260101000000" }],
    "public.organizations": [{ organizations: 1, admins: 1 }],
    "private.mfa_settings": [{ enforced: false }],
    "private.push_settings": [{ configured: 0 }],
  };
  return {
    url: "https://abcdefghijklmnopqrst.supabase.co",
    isLocal: false,
    publicKey: "sb_publishable_x",
    localVersions: ["20260101000000"],
    readAuthDifferences: async () => [],
    query: async (sql) => answers[Object.keys(answers).find((key) => sql.includes(key))],
    fetch: async (address) =>
      address.endsWith("/settings")
        ? { ok: true, status: 200, json: async () => healthySettings }
        : { ok: true, status: 401 },
    ...overrides,
  };
}

const statusOf = (results, label) => results.find((entry) => entry.label === label)?.status;

test("a healthy server passes every required check", async () => {
  const results = await runChecks(context());

  assert.equal(hasFailure(results), false);
  assert.equal(statusOf(results, "Server reachable"), PASS);
  assert.equal(statusOf(results, "Database is up to date"), PASS);
  assert.equal(statusOf(results, "Two-step verification required for administrators and staff"), INFO);
});

test("missing migrations fail and say how many", async () => {
  const results = await runChecks(context({ localVersions: ["20260101000000", "20260202000000"] }));

  assert.equal(statusOf(results, "Database is up to date"), FAIL);
  assert.deepEqual(missingMigrations(["1", "2", "3"], ["2"]), ["1", "3"]);
});

test("an undeployed function fails and a protected one passes", async () => {
  const missing = await runChecks(context({ fetch: async (a) => a.endsWith("/settings") ? { ok: true, json: async () => healthySettings } : { ok: false, status: 404 } }));

  assert.equal(statusOf(missing, "Account function deployed"), FAIL);
});

test("open sign-ups and a plain HTTP address are reported", async () => {
  const open = await runChecks(context({
    fetch: async () => ({ ok: true, status: 200, json: async () => ({ ...healthySettings, disable_signup: false }) }),
  }));
  assert.equal(statusOf(open, "Public sign-ups are off"), WARN);

  const plain = await runChecks(context({ url: "http://example.test" }));
  assert.equal(statusOf(plain, "Server reachable"), FAIL);
});

test("no organization is a failure and an unreachable server does not stop the other checks", async () => {
  const noOrganization = await runChecks(context({ query: async (sql) => sql.includes("organizations") ? [{ organizations: 0, admins: 0 }] : [{ version: "20260101000000" }] }));
  assert.equal(statusOf(noOrganization, "Organization and administrator exist"), FAIL);

  const down = await runChecks(context({ fetch: async () => { throw new Error("offline"); } }));
  assert.equal(statusOf(down, "Server reachable"), FAIL);
  assert.ok(down.length >= 8);
});

test("enforcement is not an error on a database that predates it", async () => {
  const results = await runChecks(context({
    query: async (sql) => {
      if (sql.includes("mfa_settings")) throw new Error("relation does not exist");
      return context().query(sql);
    },
  }));

  assert.equal(statusOf(results, "Two-step verification required for administrators and staff"), INFO);
});

test("auth setting differences are warnings and the local stack skips them", async () => {
  const differs = await runChecks(context({ readAuthDifferences: async () => ["auth.enable_signup: true -> false"] }));
  assert.equal(statusOf(differs, "Auth settings match"), WARN);

  const local = await runChecks(context({ readAuthDifferences: null }));
  assert.equal(statusOf(local, "Auth settings match"), INFO);
});

test("the report lists each check and hints only when something needs doing", () => {
  const text = formatReport([
    { status: PASS, label: "A", detail: "", hint: "ignored" },
    { status: FAIL, label: "B", detail: "broken", hint: "Fix it." },
  ]);

  assert.match(text, /\[ok\]\s+A$/m);
  assert.match(text, /\[FAIL\] B \(broken\)\n\s+Fix it\./);
  assert.ok(!text.includes("ignored"));
});

test("only declared auth settings count as differences", () => {
  const diff = { changes: [
    { path: ["auth", "site_url"], declared: false, remote: "a", local: "b" },
    { path: ["auth", "enable_signup"], declared: true, remote: true, local: false },
  ] };

  assert.deepEqual(declaredChanges(diff).map(describeChange), ["auth.enable_signup: true -> false"]);
  assert.ok(!AUTH_SETTINGS_TOML.includes("site_url"));
});

test("reads the JSON the CLI prints between its notices", () => {
  assert.deepEqual(extractJson('Target: project x\n{\n "rows": [{"a": 1}]\n}\nA new version is available'), { rows: [{ a: 1 }] });
  assert.throws(() => extractJson("nothing here"), CliError);
});

test("picks the public and the secret key and validates a project ref", () => {
  const keys = [
    { type: "legacy", id: "anon", api_key: "eyJanon" },
    { type: "publishable", id: "1", api_key: "sb_publishable_a" },
    { type: "secret", id: "2", api_key: "sb_secret_a" },
  ];

  assert.equal(publicKeyOf(keys), "sb_publishable_a");
  assert.equal(publicKeyOf([keys[0]]), "eyJanon");
  assert.equal(secretKeyOf(keys), "sb_secret_a");
  assert.ok(isProjectRef("abcdefghijklmnopqrst"));
  assert.ok(!isProjectRef("../etc"));
});

test("reads commands and options", () => {
  assert.deepEqual(parseArgs(["doctor", "--local", "--project-ref", "abc"]), { command: "doctor", options: { local: true, "project-ref": "abc" } });
  assert.throws(() => parseArgs(["doctor", "stray"]));
});
