import assert from "node:assert/strict";
import { test } from "node:test";
import { createUi, detectOutput } from "./ui.mjs";

function capture(settings) {
  const lines = [];
  return { lines, ui: createUi({ log: (line) => lines.push(line), ...settings }) };
}

test("prints plain words without color when nobody is watching", () => {
  const { ui, lines } = capture({});
  ui.plan(2);
  ui.step("First");
  ui.done("worked");
  ui.warn("careful");
  ui.error("broken\nTry again.");

  assert.deepEqual(lines, ["[1/2] First", "  [ok]   worked", "  [warn] careful", "[FAIL] broken", "       Try again."]);
  assert.ok(!lines.join("").includes("\u001b"));
});

test("uses symbols and color on a terminal", () => {
  const { ui, lines } = capture({ unicode: true, color: true });
  ui.done("worked");

  assert.match(lines[0], /\u001b\[32m✓\u001b\[0m worked/);
});

test("a single step has no counter", () => {
  const { ui, lines } = capture({});
  ui.plan(1);
  ui.step("Only");

  assert.deepEqual(lines, ["Only"]);
});

test("aligns the labels of a table and adds the note in brackets", () => {
  const { ui, lines } = capture({});
  ui.rows([["Server", "https://x"], ["Key", "abc", "public"]]);

  assert.deepEqual(lines, ["  Server  https://x", "  Key     abc  (public)"]);
});

test("detects a terminal, NO_COLOR and a dumb terminal", () => {
  assert.deepEqual(detectOutput({ stream: { isTTY: true }, env: {} }), { unicode: true, color: true });
  assert.deepEqual(detectOutput({ stream: { isTTY: true }, env: { NO_COLOR: "1" } }), { unicode: true, color: false });
  assert.deepEqual(detectOutput({ stream: { isTTY: true }, env: { TERM: "dumb" } }), { unicode: false, color: false });
  assert.deepEqual(detectOutput({ stream: { isTTY: false }, env: {} }), { unicode: false, color: false });
});
