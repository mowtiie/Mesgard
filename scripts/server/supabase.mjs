import { spawnSync } from "node:child_process";

export class CliError extends Error {}

const MAX_OUTPUT_BYTES = 16 * 1024 * 1024;

/** Runs the Supabase CLI. With `inherit` its prompts and progress go straight to the terminal. */
export function runSupabase(args, { workdir, inherit = false } = {}) {
  const fullArgs = workdir ? ["--workdir", workdir, ...args] : args;
  const result = spawnSync("supabase", fullArgs, {
    encoding: "utf8",
    stdio: inherit ? "inherit" : ["ignore", "pipe", "pipe"],
    maxBuffer: MAX_OUTPUT_BYTES,
  });
  if (result.error?.code === "ENOENT") {
    throw new CliError('The Supabase CLI was not found. Install it and make sure "supabase" runs in this terminal.');
  }
  return { status: result.status ?? 1, stdout: result.stdout ?? "", stderr: result.stderr ?? "" };
}

/** The CLI prints a target line and update notices around its JSON, so the object or array is cut out of the text. */
export function extractJson(text) {
  const objectStart = text.indexOf("{");
  const arrayStart = text.indexOf("[");
  const isArray = arrayStart !== -1 && (objectStart === -1 || arrayStart < objectStart);
  const start = isArray ? arrayStart : objectStart;
  const end = text.lastIndexOf(isArray ? "]" : "}");
  if (start === -1 || end < start) throw new CliError("The Supabase CLI did not return JSON.");
  return JSON.parse(text.slice(start, end + 1));
}

/** A query answers with an object that has `rows` when an agent runs it and with the bare array otherwise. */
export function rowsOf(json) {
  return Array.isArray(json) ? json : json.rows;
}

export function runSupabaseJson(args, options) {
  const result = runSupabase([...args, "--output-format", "json"], options);
  if (result.status !== 0) {
    throw new CliError(`supabase ${args.slice(0, 2).join(" ")} failed: ${firstLine(result.stderr || result.stdout)}`);
  }
  return extractJson(result.stdout);
}

function firstLine(text) {
  return text.split("\n").find((line) => line.trim() !== "" && !line.startsWith("Target:"))?.trim() ?? "no details";
}
