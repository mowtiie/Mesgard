#!/usr/bin/env node
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "./args.mjs";
import { runDoctor } from "./doctor.mjs";
import { createLogin, runSetup } from "./setup.mjs";
import { CliError } from "./supabase.mjs";
import { realTools } from "./tools.mjs";
import { resolveTarget } from "./target.mjs";

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..", "..");

const HELP = `Mesgard server tools

Usage: node scripts/server/index.mjs <command> [options]

Commands:
  setup     Apply migrations, deploy the account function, set the Auth settings, create the first
            organization and its administrator, then print the link that connects the app.
  doctor    Check a server and print what is missing. Changes nothing.

Options:
  --project-ref <ref>   Hosted project to use (default: the linked project)
  --local               Use the local Supabase stack instead
  --yes                 setup: apply the Auth settings without asking
  --add-organization    setup: create another organization even when one exists
  --skip-organization   setup: do not create an organization
  --admin-password-env <NAME>
                        setup: environment variable holding the first administrator's password (12+ characters)
  --org-name, --org-slug, --admin-email, --admin-name
                        setup: answers for the first organization, so nothing is asked`;

async function main() {
  const { command, options } = parseArgs(process.argv.slice(2));
  if (command === "help" || options.help) return console.log(HELP);
  const target = resolveTarget({
    local: options.local === true,
    projectRef: typeof options["project-ref"] === "string" ? options["project-ref"] : null,
    repositoryRoot,
  });
  if (command === "setup") {
    const tools = realTools(target, repositoryRoot);
    const deps = { ...tools, createLogin, secretKey: () => target.revealSecretKey() };
    await runSetup({ target, options, deps });
    console.log("\nChecking the server");
    process.exitCode = await runDoctor({ target, repositoryRoot });
  } else if (command === "doctor") process.exitCode = await runDoctor({ target, repositoryRoot });
  else throw new CliError(`Unknown command: ${command}\n\n${HELP}`);
}

main().catch((error) => {
  console.error(error instanceof CliError || error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
