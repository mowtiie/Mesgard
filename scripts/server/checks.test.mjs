import assert from "node:assert/strict";
import { test } from "node:test";
import { AUTH_SETTINGS_TOML, declaredChanges, describeChange } from "./auth-settings.mjs";
import { parseArgs } from "./args.mjs";
import { FAIL, INFO, PASS, WARN, formatReport, hasFailure, missingMigrations, requiredApiLevel, runChecks } from "./checks.mjs";
import { extractJson, rowsOf, CliError } from "./supabase.mjs";
import { isProjectRef, publicKeyOf, secretKeyOf } from "./target.mjs";

const healthySettings = { external: { email: true }, disable_signup: true };

function context(overrides = {}) {
  const answers = {
    "supabase_migrations.schema_migrations": [{ version: "20260101000000" }],
    "public.organizations": [{ organizations: 1, admins: 1 }],
    "private.mfa_settings": [{ enforced: false }],
    "private.erasure_settings": [{ mode: "keep_messages" }],
    "private.push_settings": [],
  };
  return {
    url: "https://abcdefghijklmnopqrst.supabase.co",
    isLocal: false,
    publicKey: "sb_publishable_x",
    localVersions: ["20260101000000"],
    requiredApiLevel: 1,
    readAuthDifferences: async () => [],
    query: async (sql) => answers[Object.keys(answers).find((key) => sql.includes(key))],
    fetch: async (address) =>
      address.endsWith("/settings")
        ? { ok: true, status: 200, json: async () => healthySettings }
        : address.endsWith("/server_info")
          ? { ok: true, status: 200, json: async () => ({ api_level: 1 }) }
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

test("a server without server_info, or with a lower level, is too old for the app", async () => {
  const older = (status, level) => async (address) => {
    if (address.endsWith("/settings")) return { ok: true, status: 200, json: async () => healthySettings };
    if (address.endsWith("/server_info")) return { ok: status === 200, status, json: async () => ({ api_level: level }) };
    return { ok: true, status: 401 };
  };

  assert.equal(statusOf(await runChecks(context({ fetch: older(404) })), "Server is new enough for the app"), FAIL);
  assert.equal(statusOf(await runChecks(context({ fetch: older(200, 0), requiredApiLevel: 1 })), "Server is new enough for the app"), FAIL);
  assert.equal(statusOf(await runChecks(context({ fetch: older(200, 2), requiredApiLevel: 1 })), "Server is new enough for the app"), PASS);
});

test("push is optional when not set up, and a mismatched secret is a failure", async () => {
  const label = "Push notifications work";
  assert.equal(statusOf(await runChecks(context()), label), INFO);

  const withRow = (status) => context({
    query: async (sql) => sql.includes("push_settings")
      ? [{ function_url: "https://x/functions/v1/send-push", webhook_secret: "secret-0123456789abc" }]
      : context().query(sql),
    fetch: async (address) => address.endsWith("/send-push")
      ? { ok: status < 300, status }
      : context().fetch(address),
  });
  assert.equal(statusOf(await runChecks(withRow(200)), label), PASS);
  assert.equal(statusOf(await runChecks(withRow(401)), label), FAIL);
  assert.equal(statusOf(await runChecks(withRow(404)), label), FAIL);
});

test("the erasure mode is shown, and a database that predates it is not an error", async () => {
  const label = "What erasing a person does on this server";
  const shown = await runChecks(context());
  assert.equal(statusOf(shown, label), INFO);
  assert.match(shown.find((entry) => entry.label === label).detail, /Former member/);

  const blank = await runChecks(context({ query: async (sql) => sql.includes("erasure_settings") ? [{ mode: "blank_messages" }] : context().query(sql) }));
  assert.match(blank.find((entry) => entry.label === label).detail, /erased too/);

  const old = await runChecks(context({ query: async (sql) => { if (sql.includes("erasure_settings")) throw new Error("no table"); return context().query(sql); } }));
  assert.equal(statusOf(old, label), INFO);
  assert.match(old.find((entry) => entry.label === label).detail, /Unavailable/);
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

test("reads query rows whether the CLI prints an object with rows or a bare array", () => {
  const agentOutput = 'Target: project x\n{\n "rows": [{"organizations": 0}],\n "warning": "untrusted [data]"\n}\n';
  const personOutput = 'Target: project x\n[\n {"organizations": 0}\n]\nA new version is available';
  assert.deepEqual(rowsOf(extractJson(agentOutput)), [{ organizations: 0 }]);
  assert.deepEqual(rowsOf(extractJson(personOutput)), [{ organizations: 0 }]);
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
test("reads the API level the app needs from its source", () => {
  assert.ok(requiredApiLevel(new URL("../..", import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1")) >= 1);
});
