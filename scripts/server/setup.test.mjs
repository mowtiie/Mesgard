import assert from "node:assert/strict";
import { test } from "node:test";
import { findProblems, generatePassword, organizationSql, setupLink, slugFrom } from "./organization.mjs";
import { runSetup } from "./setup.mjs";

const answers = { name: "Rizal National High School", slug: "rnhs", email: "admin@school.example", displayName: "Maria Santos" };

function fakeTarget(overrides = {}) {
  return {
    kind: "hosted",
    ref: "abcdefghijklmnopqrst",
    url: "https://abcdefghijklmnopqrst.supabase.co",
    publicKey: () => "sb_publishable_key",
    query: async () => [{ organizations: 0 }],
    ...overrides,
  };
}

function fakeDeps(overrides = {}) {
  const calls = [];
  const record = (name) => (...args) => { calls.push([name, ...args]); };
  return {
    calls,
    linkedRef: () => "abcdefghijklmnopqrst",
    link: record("link"),
    pushMigrations: record("push"),
    deployFunction: record("deploy"),
    authDifferences: () => [],
    applyAuthSettings: record("applyAuth"),
    confirm: async () => true,
    ask: async () => "",
    secretKey: () => "sb_secret_hidden",
    createLogin: async (input) => { calls.push(["createLogin", input.email]); return { created: true }; },
    execute: record("execute"),
    ...overrides,
  };
}

const flags = { "org-name": answers.name, "org-slug": answers.slug, "admin-email": answers.email, "admin-name": answers.displayName };

test("sets up a new project in order and prints the link and the temporary password once", async () => {
  const deps = fakeDeps({ linkedRef: () => null, authDifferences: () => ["auth.enable_signup: true -> false"] });
  const lines = [];

  const created = await runSetup({ target: fakeTarget(), options: flags, deps, log: (line) => lines.push(line) });

  assert.deepEqual(deps.calls.map(([name]) => name), ["link", "push", "deploy", "applyAuth", "createLogin", "execute"]);
  assert.equal(created.email, answers.email);
  assert.equal(created.password.length, 16);
  const output = lines.join("\n");
  assert.match(output, /mesgard:\/\/setup\?url=https%3A%2F%2Fabcdefghijklmnopqrst\.supabase\.co&key=sb_publishable_key/);
  assert.equal(output.split(created.password).length - 1, 1);
  assert.ok(!output.includes("sb_secret_hidden"));
});

test("does not link again, and leaves the Auth settings alone when they are right", async () => {
  const deps = fakeDeps();

  await runSetup({ target: fakeTarget(), options: flags, deps, log: () => {} });

  assert.ok(!deps.calls.some(([name]) => name === "link" || name === "applyAuth"));
});

test("asks before changing Auth settings and respects a no", async () => {
  const deps = fakeDeps({ authDifferences: () => ["auth.enable_signup: true -> false"], confirm: async () => false });

  await runSetup({ target: fakeTarget(), options: flags, deps, log: () => {} });

  assert.ok(!deps.calls.some(([name]) => name === "applyAuth"));
});

test("skips the first organization when one exists, unless another is asked for", async () => {
  const existing = fakeTarget({ query: async () => [{ organizations: 2 }] });

  const skipped = fakeDeps();
  assert.equal(await runSetup({ target: existing, options: flags, deps: skipped, log: () => {} }), null);
  assert.ok(!skipped.calls.some(([name]) => name === "createLogin"));

  const added = fakeDeps();
  await runSetup({ target: existing, options: { ...flags, "add-organization": true }, deps: added, log: () => {} });
  assert.ok(added.calls.some(([name]) => name === "createLogin"));
});

test("reuses a login that exists and does not print a password for it", async () => {
  const deps = fakeDeps({ createLogin: async () => ({ created: false }) });

  const created = await runSetup({ target: fakeTarget(), options: flags, deps, log: () => {} });

  assert.equal(created.password, null);
  assert.ok(deps.calls.some(([name]) => name === "execute"));
});

test("stops before creating anything when the answers are not valid", async () => {
  const deps = fakeDeps();

  await assert.rejects(
    runSetup({ target: fakeTarget(), options: { ...flags, "admin-email": "not an email" }, deps, log: () => {} }),
    /email is not valid/,
  );
  assert.ok(!deps.calls.some(([name]) => name === "createLogin"));
});

test("asks for what the flags do not give, and proposes a short name", async () => {
  const asked = [];
  const deps = fakeDeps({
    ask: async (question) => {
      asked.push(question);
      return question.startsWith("Short name") ? "" : question.includes("email") ? answers.email : "Some Name";
    },
  });

  await runSetup({ target: fakeTarget(), options: {}, deps, log: () => {} });

  assert.ok(asked.some((question) => question.includes("[some-name]")));
  assert.ok(deps.calls.some(([name, email]) => name === "createLogin" && email === answers.email));
});

test("the local stack skips migrations, functions and Auth settings", async () => {
  const deps = fakeDeps();

  await runSetup({ target: fakeTarget({ kind: "local", ref: null }), options: flags, deps, log: () => {} });

  assert.deepEqual(deps.calls.map(([name]) => name), ["createLogin", "execute"]);
});

test("builds a slug from a name", () => {
  assert.equal(slugFrom("Rizal National High School"), "rizal-national-high-school");
  assert.equal(slugFrom("  Colegio de San José! "), "colegio-de-san-jose");
  assert.equal(slugFrom("***"), "");
});

test("refuses answers that could break out of the SQL", () => {
  assert.deepEqual(findProblems(answers), []);
  assert.ok(findProblems({ ...answers, email: "a'b@c.d" }).length > 0);
  assert.ok(findProblems({ ...answers, slug: "Bad Slug" }).length > 0);
  assert.ok(findProblems({ ...answers, name: "line\nbreak" }).length > 0);
  assert.ok(findProblems({ ...answers, displayName: "" }).length > 0);
});

test("doubles quotes in the organization name", () => {
  const sql = organizationSql({ ...answers, name: "St. Mary's School" });

  assert.match(sql, /values \('St\. Mary''s School', 'rnhs'\)/);
  assert.match(sql, /lower\('admin@school\.example'\)/);
});

test("generates passwords with every kind of character and no repeats between runs", () => {
  const first = generatePassword();

  assert.match(first, /[a-z]/);
  assert.match(first, /[A-Z]/);
  assert.match(first, /[2-9]/);
  assert.notEqual(first, generatePassword());
  assert.equal(setupLink("https://x.supabase.co", "a b"), "mesgard://setup?url=https%3A%2F%2Fx.supabase.co&key=a%20b");
});
