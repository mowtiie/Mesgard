#!/usr/bin/env node
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "./args.mjs";
import { runDoctor } from "./doctor.mjs";
import { runPush } from "./push.mjs";
import { runReset } from "./reset.mjs";
import { runRetention } from "./retention.mjs";
import { createLogin, runSetup } from "./setup.mjs";
import { CliError } from "./supabase.mjs";
import { realTools } from "./tools.mjs";
import { resolveTarget } from "./target.mjs";
import { createUi, detectOutput } from "./ui.mjs";

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..", "..");

const HELP = `Mesgard server tools

Usage: node scripts/server/index.mjs <command> [options]

Commands:
  setup     Apply migrations, deploy the account function, set the Auth settings, create the first
            organization and its administrator, then print the link that connects the app.
  push      Set up push notifications: deploy the push function, create its secret, tell the database and
            check that they agree. Run it again with --rotate to replace the secret.
  retention Erase messages and files older than the limit each organization chose, except what a legal hold or
            an open report protects. Run it on a schedule (see docs/compliance/retention.md). Needs --local or
            --project-ref <ref>.
  doctor    Check a server and print what is missing. Changes nothing.
  reset     Rebuild the database from the migrations. Erases all data. Needs --local or --project-ref <ref>;
            a hosted project is never seeded and asks for its ref to be typed.
  reinstall Reset, then setup, in one go: an empty database with a new organization and administrator.
            Needs --local or --project-ref <ref>; never seeds, and takes the options of both commands.

Options:
  --project-ref <ref>   Hosted project to use (default: the linked project)
  --local               Use the local Supabase stack instead
  --yes                 setup: apply the Auth settings without asking
  --add-organization    setup: create another organization even when one exists
  --skip-organization   setup: do not create an organization
  --admin-password-env <NAME>
                        setup: environment variable holding the first administrator's password (12+ characters)
  --require-mfa, --no-require-mfa
                        setup: answer the two-step verification question (asked when an organization is created;
                        --yes means yes; a server that already requires it is never changed)
  --with-push           setup: also set up push notifications
  --push-hosts <list>   push: comma separated hosts of your own push server (default: any public push server)
  --rotate              push: replace the secret even when push is already set up
  --no-seed             reset: do not run the seed on the local stack
  --dry-run             retention: say how many messages are due, change nothing
  --batch <number>      retention: messages per round, 1 to 1000 (default 200)
  --confirm <ref>       reset: the project ref again, for a hosted project, when nothing can be asked
  --org-name, --org-slug, --admin-email, --admin-name
                        setup: answers for the first organization, so nothing is asked`;

async function setUp(target, options, output) {
  const tools = realTools(target, repositoryRoot);
  const deps = { ...tools, createLogin, secretKey: () => target.revealSecretKey() };
  await runSetup({ target, options, deps, ...output });
  if (options["with-push"] === true) await runPush({ target, options, deps: tools, ...output });
  return runDoctor({ target, repositoryRoot, embedded: true, ...output });
}

async function main() {
  const output = detectOutput();
  const { command, options } = parseArgs(process.argv.slice(2));
  if (command === "help" || options.help) return console.log(HELP);
  const target = resolveTarget({
    local: options.local === true,
    projectRef: typeof options["project-ref"] === "string" ? options["project-ref"] : null,
    repositoryRoot,
  });
  const isExplicit = options.local === true || typeof options["project-ref"] === "string";
  if (command === "push") {
    await runPush({ target, options, deps: realTools(target, repositoryRoot), ...output });
    process.exitCode = 0;
  } else if (command === "setup") {
    process.exitCode = await setUp(target, options, output);
  } else if (command === "reset") {
    await runReset({ target, options, deps: realTools(target, repositoryRoot), isExplicit, ...output });
  } else if (command === "reinstall") {
    // A seeded local stack already has organizations, which setup would skip, so it starts empty.
    const chained = { ...options, chained: true, "no-seed": true };
    await runReset({ target, options: chained, deps: realTools(target, repositoryRoot), isExplicit, ...output });
    process.exitCode = await setUp(target, options, output);
  } else if (command === "retention") {
    await runRetention({ target, options, deps: realTools(target, repositoryRoot), isExplicit, ...output });
  } else if (command === "doctor") process.exitCode = await runDoctor({ target, repositoryRoot, ...output });
  else throw new CliError(`Unknown command: ${command}\n\n${HELP}`);
}

main().catch((error) => {
  const message = error instanceof Error ? error.message : String(error);
  createUi({ log: console.error, ...detectOutput({ stream: process.stderr }) }).error(message);
  process.exitCode = 1;
});
