import assert from "node:assert/strict";
import { test } from "node:test";
import { hostedResetArguments, localResetArguments, runReset } from "./reset.mjs";

const REF = "abcdefghijklmnopqrst";

function fakeDeps(answer = "") {
  const calls = [];
  return {
    calls,
    ask: async (question) => {
      calls.push(["ask", question]);
      return answer;
    },
    resetLocal: (settings) => calls.push(["resetLocal", settings]),
    resetHosted: (ref) => calls.push(["resetHosted", ref]),
  };
}

const hosted = { kind: "hosted", ref: REF, query: async () => [{ logins: 4, organizations: 1, messages: 120 }] };
const local = { kind: "local", query: async () => [] };

test("a hosted reset never seeds and always names the project", () => {
  const args = hostedResetArguments(REF);

  assert.ok(args.includes("--no-seed"));
  assert.ok(args.includes(`--remote=${REF}`));
  assert.ok(args.includes("--yes"));
});

test("a local reset seeds unless asked not to", () => {
  assert.deepEqual(localResetArguments({ seed: true }), ["db", "reset"]);
  assert.deepEqual(localResetArguments({ seed: false }), ["db", "reset", "--no-seed"]);
});

test("nothing is reset until the person says what", async () => {
  const deps = fakeDeps();

  await assert.rejects(runReset({ target: hosted, options: {}, deps, isExplicit: false, log: () => {} }), /--local.*--project-ref/);
  assert.deepEqual(deps.calls, []);
});

test("the local stack is reset with the seed, or without it when asked", async () => {
  const seeded = fakeDeps();
  await runReset({ target: local, options: {}, deps: seeded, isExplicit: true, log: () => {} });
  assert.deepEqual(seeded.calls, [["resetLocal", { seed: true }]]);

  const bare = fakeDeps();
  await runReset({ target: local, options: { "no-seed": true }, deps: bare, isExplicit: true, log: () => {} });
  assert.deepEqual(bare.calls, [["resetLocal", { seed: false }]]);
});

test("a hosted project is erased only after its ref is typed, and says what it holds", async () => {
  const lines = [];
  const deps = fakeDeps(REF);

  await runReset({ target: hosted, options: {}, deps, isExplicit: true, log: (line) => lines.push(line) });

  assert.deepEqual(deps.calls.map(([name]) => name), ["ask", "resetHosted"]);
  assert.match(lines.join("\n"), /4 logins, 1 organizations and 120 messages/);
  assert.match(lines.join("\n"), new RegExp(`setup --project-ref ${REF}`));
});

test("a wrong ref erases nothing", async () => {
  const deps = fakeDeps("abcdefghijklmnopqrsu");

  await assert.rejects(runReset({ target: hosted, options: {}, deps, isExplicit: true, log: () => {} }), /nothing was erased/);
  assert.ok(!deps.calls.some(([name]) => name === "resetHosted"));
});

test("the ref can be given as an option when nothing can be asked, and it must still match", async () => {
  const deps = fakeDeps();
  await runReset({ target: hosted, options: { confirm: REF }, deps, isExplicit: true, log: () => {} });
  assert.deepEqual(deps.calls, [["resetHosted", REF]]);

  const wrong = fakeDeps();
  await assert.rejects(runReset({ target: hosted, options: { confirm: "other" }, deps: wrong, isExplicit: true, log: () => {} }), /nothing was erased/);
  assert.deepEqual(wrong.calls, []);
});

test("when chained to setup, the reset does not print next steps that setup is about to do", async () => {
  const lines = [];
  await runReset({ target: hosted, options: { confirm: REF, chained: true }, deps: fakeDeps(), isExplicit: true, log: (line) => lines.push(line) });
  assert.doesNotMatch(lines.join("\n"), /Next steps/);

  const localLines = [];
  await runReset({ target: local, options: { chained: true, "no-seed": true }, deps: fakeDeps(), isExplicit: true, log: (line) => localLines.push(line) });
  assert.doesNotMatch(localLines.join("\n"), /admin@rnhs/);
});
