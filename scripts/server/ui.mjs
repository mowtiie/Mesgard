const ANSI = { bold: "\u001b[1m", dim: "\u001b[2m", red: "\u001b[31m", green: "\u001b[32m", yellow: "\u001b[33m", cyan: "\u001b[36m" };
const RESET = "\u001b[0m";
const UNICODE_SYMBOLS = { done: "✓", fail: "✗", warn: "!", info: "i" };
const PLAIN_SYMBOLS = { done: "[ok]", fail: "[FAIL]", warn: "[warn]", info: "[info]" };

/** Color and symbols only when a person is watching, and never when NO_COLOR is set. */
export function detectOutput({ stream = process.stdout, env = process.env } = {}) {
  const terminal = Boolean(stream.isTTY) && env.TERM !== "dumb";
  return { unicode: terminal, color: terminal && !("NO_COLOR" in env) };
}

/**
 * Every line the tools print goes through here, so the wording and the layout live in one place. The plain form
 * (no color, bracketed words) is what files, pipes and tests see.
 */
export function createUi({ log = console.log, unicode = false, color = false } = {}) {
  const paint = (style, text) => (color ? `${ANSI[style]}${text}${RESET}` : text);
  const symbols = unicode ? UNICODE_SYMBOLS : PLAIN_SYMBOLS;
  const line = (style, kind, text) => log(`  ${paint(style, symbols[kind])} ${text}`);
  let total = 0;
  let current = 0;

  return {
    banner(title, detail) {
      log(paint("bold", title));
      log(paint("dim", detail));
      log("");
    },
    section(title) {
      log("");
      log(paint("bold", title));
    },
    plan(count) {
      total = count;
      current = 0;
    },
    step(title) {
      current += 1;
      log(paint("bold", total > 1 ? `[${current}/${total}] ${title}` : title));
    },
    done: (text) => line("green", "done", text),
    info: (text) => line("cyan", "info", text),
    warn: (text) => line("yellow", "warn", text),
    detail: (text) => log(`      ${paint("dim", text)}`),
    rows(entries) {
      const width = Math.max(...entries.map(([label]) => label.length));
      for (const [label, value, note] of entries) {
        log(`  ${paint("dim", label.padEnd(width))}  ${value}${note ? `  ${paint("dim", `(${note})`)}` : ""}`);
      }
    },
    list(items) {
      items.forEach((item, index) => log(`  ${index + 1}. ${item}`));
    },
    blank: () => log(""),
    error(message) {
      const [first, ...rest] = message.split("\n");
      log(`${paint("red", symbols.fail)} ${first}`);
      rest.forEach((text) => log(`  ${text}`));
    },
  };
}
