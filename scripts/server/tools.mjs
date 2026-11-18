import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createInterface } from "node:readline/promises";
import { applyAuthSettings, describeChange, readAuthSettingDifferences } from "./auth-settings.mjs";
import { CliError, runSupabase } from "./supabase.mjs";
import { hostedResetArguments, localResetArguments } from "./reset.mjs";
import { readLinkedRef } from "./target.mjs";

function runVisibly(args, what) {
  const result = runSupabase(args, { inherit: true });
  if (result.status !== 0) throw new CliError(`${what} failed. Fix the problem above and try again.`);
}

/** For steps that never ask anything: the CLI's progress is hidden and shown only when the step fails. */
function runQuietly(args, what) {
  const result = runSupabase(args);
  if (result.status === 0) return;
  process.stderr.write(`${(result.stderr || result.stdout).trim()}\n`);
  throw new CliError(`${what} failed. Fix the problem above and try again.`);
}

async function prompt(question) {
  if (!process.stdin.isTTY) {
    throw new CliError(`A question needs a terminal, so pass the answer as an option. Question: ${question.trim()}`);
  }
  const lines = createInterface({ input: process.stdin, output: process.stdout });
  try {
    return await lines.question(question);
  } finally {
    lines.close();
  }
}

/** The side effects of setup: the CLI, the terminal and the SQL that writes. */
export function realTools(target, repositoryRoot) {
  return {
    linkedRef: () => readLinkedRef(repositoryRoot),
    link: (ref) => runVisibly(["link", "--project-ref", ref], "Linking the project"),
    pushMigrations: (ref) => runVisibly(["db", "push", `--remote=${ref}`, "--yes"], "Applying the migrations"),
    deployFunction: (ref, name = "admin-users") =>
      runQuietly(["functions", "deploy", name, `--remote=${ref}`, "--use-api", "--yes"], `Deploying ${name}`),
    setSecrets: (ref, values) => setSecrets(ref, values),
    resetLocal: (settings) => runVisibly(localResetArguments(settings), "Resetting the local database"),
    resetHosted: (ref) => runVisibly(hostedResetArguments(ref), "Resetting the database"),
    fetch,
    wait: (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds)),
    authDifferences: (ref) => readAuthSettingDifferences(ref).map(describeChange),
    applyAuthSettings: (ref) => {
      const result = applyAuthSettings(ref);
      if (result.status !== 0) throw new CliError("Applying the Auth settings failed.");
    },
    execute: (sql) => executeSql(target, sql),
    env: process.env,
    ask: prompt,
    confirm: async (question) => !/^n(o)?$/i.test((await prompt(question)).trim()),
  };
}

/** Function secrets go through a file that only the owner can read, so they never appear in a command line. */
function setSecrets(ref, values) {
  const folder = mkdtempSync(join(tmpdir(), "mesgard-secrets-"));
  try {
    const file = join(folder, "secrets.env");
    writeFileSync(file, Object.entries(values).map(([name, value]) => `${name}=${value}`).join("\n") + "\n", { mode: 0o600 });
    const result = runSupabase(["secrets", "set", `--remote=${ref}`, "--env-file", file, "--yes"]);
    if (result.status !== 0) throw new CliError("Setting the function secrets failed.");
  } finally {
    rmSync(folder, { recursive: true, force: true });
  }
}

/** Runs a statement that writes. It goes through a file, so quotes in the text never reach a shell. */
function executeSql(target, sql) {
  const folder = mkdtempSync(join(tmpdir(), "mesgard-sql-"));
  try {
    const file = join(folder, "statement.sql");
    writeFileSync(file, sql);
    const remote = target.remoteFlag ? [target.remoteFlag, "--yes"] : [];
    const result = runSupabase(["db", "query", ...remote, "--file", file]);
    if (result.status !== 0) {
      throw new CliError(`Running the SQL failed: ${result.stderr.split("\n").find((line) => line.trim()) ?? "no details"}`);
    }
  } finally {
    rmSync(folder, { recursive: true, force: true });
  }
}
