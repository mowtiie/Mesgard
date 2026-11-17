#!/usr/bin/env node
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "./args.mjs";
import { runDoctor } from "./doctor.mjs";
import { CliError } from "./supabase.mjs";
import { resolveTarget } from "./target.mjs";

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..", "..");

const HELP = `Mesgard server tools

Usage: node scripts/server/index.mjs <command> [options]

Commands:
  doctor    Check a server and print what is missing. Changes nothing.

Options:
  --project-ref <ref>   Hosted project to use (default: the linked project)
  --local               Use the local Supabase stack instead`;

async function main() {
  const { command, options } = parseArgs(process.argv.slice(2));
  if (command === "help" || options.help) return console.log(HELP);
  const target = resolveTarget({
    local: options.local === true,
    projectRef: typeof options["project-ref"] === "string" ? options["project-ref"] : null,
    repositoryRoot,
  });
  if (command === "doctor") process.exitCode = await runDoctor({ target, repositoryRoot });
  else throw new CliError(`Unknown command: ${command}\n\n${HELP}`);
}

main().catch((error) => {
  console.error(error instanceof CliError || error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
