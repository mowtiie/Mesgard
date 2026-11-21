import assert from "node:assert/strict";
import { test } from "node:test";
import { deleteFiles, parseBatch, runRetention, storageHeaders } from "./retention.mjs";
import { CliError } from "./supabase.mjs";

const URL = "http://127.0.0.1:54321";

/** A server with `waiting` messages to erase, two files per round, answering like the database does. */
function fakeServer({ organizations = 1, rounds = [{ waiting: 2, paths: ["a/b/1", "a/b/2"] }, { waiting: 0, paths: [] }] } = {}) {
  const calls = [];
  const queue = [...rounds];
  const target = {
    kind: "local",
    url: URL,
    revealSecretKey: () => {
      calls.push(["reveal"]);
      return "sb_secret_test";
    },
    query: async (sql) => {
      if (sql.includes("message_retention_days")) return [{ organizations }];
      if (sql.includes("expired_message_ids")) return [{ waiting: queue[0].waiting }];
      calls.push(["list"]);
      return queue[0].paths.map((path) => ({ path }));
    },
  };
  const deps = {
    execute: (sql) => {
      calls.push(["expire", sql]);
      queue.shift();
    },
    fetch: async (url, init) => {
      calls.push(["delete", url, JSON.parse(init.body).prefixes, init.headers.apikey]);
      return { ok: true, status: 200 };
    },
  };
  return { target, deps, calls };
}

const run = (server, options = {}, extra = {}) =>
  runRetention({ target: server.target, options, deps: server.deps, isExplicit: true, log: () => {}, ...extra });

test("deletes the files first and then the rows, round after round", async () => {
  const server = fakeServer();

  const result = await run(server);

  assert.deepEqual(result, { messages: 2, files: 2 });
  assert.deepEqual(server.calls.map(([name]) => name), ["list", "reveal", "delete", "expire"]);
  assert.deepEqual(server.calls[2].slice(1), [`${URL}/storage/v1/object/message-attachments`, ["a/b/1", "a/b/2"], "sb_secret_test"]);
});

test("does nothing when no organization has a limit", async () => {
  const server = fakeServer({ organizations: 0 });

  assert.deepEqual(await run(server), { messages: 0, files: 0 });
  assert.deepEqual(server.calls, []);
});

test("a dry run changes nothing and asks for no key", async () => {
  const server = fakeServer();

  const result = await run(server, { "dry-run": true });

  assert.deepEqual(result, { messages: 0, files: 0 });
  assert.deepEqual(server.calls, []);
});

test("a round without files never asks for the secret key", async () => {
  const server = fakeServer({ rounds: [{ waiting: 3, paths: [] }, { waiting: 0, paths: [] }] });

  const result = await run(server);

  assert.deepEqual(result, { messages: 3, files: 0 });
  assert.equal(server.calls.some(([name]) => name === "reveal"), false);
});

test("a failed file deletion stops before any row is touched, so it can be run again", async () => {
  const server = fakeServer();
  server.deps.fetch = async () => ({ ok: false, status: 500 });

  await assert.rejects(() => run(server), (error) => error instanceof CliError && /storage service answered 500/.test(error.message));
  assert.equal(server.calls.some(([name]) => name === "expire"), false);
});

test("refuses to run without a named target", async () => {
  const server = fakeServer();

  await assert.rejects(
    () => runRetention({ target: server.target, options: {}, deps: server.deps, isExplicit: false, log: () => {} }),
    /--local .* --project-ref/,
  );
});

test("the batch is a whole number the database accepts", () => {
  assert.equal(parseBatch(undefined), 200);
  assert.equal(parseBatch("50"), 50);
  assert.throws(() => parseBatch("0"), CliError);
  assert.throws(() => parseBatch("1001"), CliError);
  assert.throws(() => parseBatch("many"), CliError);
});

test("the secret goes in the apikey header, and a legacy key in the authorization header too", () => {
  assert.equal(storageHeaders("sb_secret_abc").Authorization, undefined);
  assert.equal(storageHeaders("a.b.c").Authorization, "Bearer a.b.c");
});

test("files are deleted a hundred at a time", async () => {
  const sizes = [];
  const paths = Array.from({ length: 230 }, (_, index) => `p/${index}`);

  await deleteFiles({
    url: URL,
    secretKey: "sb_secret_test",
    paths,
    fetch: async (_url, init) => {
      sizes.push(JSON.parse(init.body).prefixes.length);
      return { ok: true, status: 200 };
    },
  });

  assert.deepEqual(sizes, [100, 100, 30]);
});
