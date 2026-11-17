import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createInterface } from "node:readline/promises";
import { applyAuthSettings, describeChange, readAuthSettingDifferences } from "./auth-settings.mjs";
import { CliError, runSupabase } from "./supabase.mjs";
import { readLinkedRef } from "./target.mjs";

function runVisibly(args, what) {
  const result = runSupabase(args, { inherit: true });
  if (result.status !== 0) throw new CliError(`${what} failed. Fix the problem above and run setup again.`);
}

async function prompt(question) {
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
    deployFunction: (ref) =>
      runVisibly(["functions", "deploy", "admin-users", `--remote=${ref}`, "--use-api", "--yes"], "Deploying admin-users"),
    authDifferences: (ref) => readAuthSettingDifferences(ref).map(describeChange),
    applyAuthSettings: (ref) => {
      const result = applyAuthSettings(ref);
      if (result.status !== 0) throw new CliError("Applying the Auth settings failed.");
    },
    execute: (sql) => executeSql(target, sql),
    ask: prompt,
    confirm: async (question) => /^y(es)?$/i.test((await prompt(question)).trim()),
  };
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
