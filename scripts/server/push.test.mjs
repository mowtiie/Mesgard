import assert from "node:assert/strict";
import { test } from "node:test";
import { CliError } from "./supabase.mjs";
import { checkPushFunction, functionUrlFor, generateWebhookSecret, parseHosts, pushSettingsSql, runPush } from "./push.mjs";

const REF = "abcdefghijklmnopqrst";
const URL = `https://${REF}.supabase.co`;

function fakeTarget(rows = {}) {
  return {
    kind: "hosted",
    ref: REF,
    url: URL,
    query: async (sql) => (sql.includes("webhook_secret") ? rows.secret ?? [] : rows.existing ?? []),
  };
}

function fakeDeps({ answers = [200], ...overrides } = {}) {
  const calls = [];
  const queue = [...answers];
  return {
    calls,
    deployFunction: (ref, name) => calls.push(["deploy", name]),
    setSecrets: (ref, values) => calls.push(["setSecrets", values]),
    execute: (sql) => calls.push(["execute", sql]),
    fetch: async (url, init) => {
      calls.push(["fetch", url, init.headers["x-webhook-secret"]]);
      const status = queue.length > 1 ? queue.shift() : queue[0];
      return { ok: status >= 200 && status < 300, status };
    },
    wait: async () => {},
    ...overrides,
  };
}

test("sets push up once: deploys, shares one secret with the function and the database, then checks", async () => {
  const deps = fakeDeps();
  const lines = [];

  await runPush({ target: fakeTarget(), options: {}, deps, log: (line) => lines.push(line) });

  assert.deepEqual(deps.calls.map(([name]) => name), ["deploy", "setSecrets", "execute", "fetch"]);
  const secret = deps.calls[1][1].PUSH_WEBHOOK_SECRET;
  assert.match(secret, /^[0-9a-f]{64}$/);
  assert.ok(deps.calls[2][1].includes(secret));
  assert.equal(deps.calls[3][2], secret);
  assert.ok(deps.calls[2][1].includes(`${URL}/functions/v1/send-push`));
  assert.ok(!lines.join("\n").includes(secret));
});

test("keeps the secret of a push that is already set up, and verifies with the one in the database", async () => {
  const deps = fakeDeps();
  const target = fakeTarget({ existing: [{ function_url: "x" }], secret: [{ webhook_secret: "kept-secret-0123456789" }] });

  await runPush({ target, options: {}, deps, log: () => {} });

  assert.deepEqual(deps.calls.map(([name]) => name), ["deploy", "fetch"]);
  assert.equal(deps.calls[1][2], "kept-secret-0123456789");
});

test("replaces the secret when asked to rotate it", async () => {
  const deps = fakeDeps();
  const target = fakeTarget({ existing: [{ function_url: "x" }] });

  await runPush({ target, options: { rotate: true }, deps, log: () => {} });

  assert.deepEqual(deps.calls.map(([name]) => name), ["deploy", "setSecrets", "execute", "fetch"]);
});

test("stores the allowed push servers next to the secret", async () => {
  const deps = fakeDeps();

  await runPush({ target: fakeTarget(), options: { "push-hosts": "ntfy.school.edu, *.push.school.edu" }, deps, log: () => {} });

  assert.equal(deps.calls[1][1].PUSH_ALLOWED_ENDPOINT_HOSTS, "ntfy.school.edu,*.push.school.edu");
});

test("waits for a new secret to reach the function before giving up", async () => {
  const deps = fakeDeps({ answers: [401, 401, 200] });

  await runPush({ target: fakeTarget(), options: {}, deps, log: () => {} });

  assert.equal(deps.calls.filter(([name]) => name === "fetch").length, 3);
});

test("says what is wrong when the function never accepts the secret", async () => {
  await assert.rejects(
    runPush({ target: fakeTarget(), options: {}, deps: fakeDeps({ answers: [401] }), log: () => {} }),
    /different secret than the database/,
  );
  await assert.rejects(
    runPush({ target: fakeTarget(), options: {}, deps: fakeDeps({ answers: [404] }), log: () => {} }),
    /not deployed/,
  );
});

test("the local stack is left alone", async () => {
  const deps = fakeDeps();

  await runPush({ target: { kind: "local", query: async () => [] }, options: {}, deps, log: () => {} });

  assert.deepEqual(deps.calls, []);
});

test("refuses a push server list that is not made of host names", () => {
  assert.deepEqual(parseHosts("ntfy.school.edu,*.push.school.edu,10.0.0.5:8085"), ["ntfy.school.edu", "*.push.school.edu", "10.0.0.5:8085"]);
  assert.deepEqual(parseHosts(""), []);
  assert.throws(() => parseHosts("ok.example, bad host!"), CliError);
  assert.throws(() => parseHosts("https://ntfy.sh"), CliError);
});

test("builds the settings statement with doubled quotes and the project's function address", () => {
  assert.match(pushSettingsSql("https://x/functions/v1/send-push", "it's-a-secret-0123456789"), /'it''s-a-secret-0123456789'/);
  assert.match(pushSettingsSql("a", "b"), /on conflict \(singleton\) do update/);
  assert.equal(functionUrlFor(URL), `${URL}/functions/v1/send-push`);
  assert.notEqual(generateWebhookSecret(), generateWebhookSecret());
});

test("tells apart a missing function, a different secret and a healthy one", async () => {
  const answer = (status) => async () => ({ ok: status < 300, status });
  const base = { functionUrl: "u", secret: "s" };

  assert.equal((await checkPushFunction({ ...base, fetch: answer(200) })).ok, true);
  assert.match((await checkPushFunction({ ...base, fetch: answer(404) })).reason, /not deployed/);
  assert.match((await checkPushFunction({ ...base, fetch: answer(401) })).reason, /different secret/);
  assert.match((await checkPushFunction({ ...base, fetch: answer(500) })).reason, /500/);
});
