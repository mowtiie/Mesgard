import { CliError } from "./supabase.mjs";
import { createUi } from "./ui.mjs";

const COUNTS_SQL = `select
  (select count(*) from auth.users)::int as logins,
  (select count(*) from public.organizations)::int as organizations,
  (select count(*) from public.messages)::int as messages`;

export const localResetArguments = ({ seed }) => ["db", "reset", ...(seed ? [] : ["--no-seed"])];

/** A hosted project is never seeded: the seed adds accounts with a known password, which must not exist there. */
export const hostedResetArguments = (ref) => ["db", "reset", `--remote=${ref}`, "--no-seed", "--yes"];

async function describeContents(target) {
  try {
    const [counts] = await target.query(COUNTS_SQL);
    return `${counts.logins} logins, ${counts.organizations} organizations and ${counts.messages} messages`;
  } catch {
    return "everything in its database";
  }
}

/**
 * Builds the database again from the migrations. The local stack is reset as it is, with the seed unless told
 * otherwise. A hosted project is erased for good, so it only happens when the project was named, and then the person has
 * to type its ref.
 */
export async function runReset({ target, options, deps, isExplicit, log = console.log, unicode = false, color = false }) {
  const ui = createUi({ log, unicode, color });
  if (!isExplicit) {
    throw new CliError("Say what to reset: --local for the local stack, or --project-ref <ref> for a hosted project.");
  }
  if (target.kind === "local") return resetLocal(ui, options, deps);
  return resetHosted(ui, target, options, deps);
}

function resetLocal(ui, options, deps) {
  ui.banner("Mesgard database reset", "Local Supabase stack");
  ui.step("Resetting the local database");
  deps.resetLocal({ seed: options["no-seed"] !== true });
  ui.done(options["no-seed"] === true ? "Rebuilt from the migrations, without the seed" : "Rebuilt from the migrations and seeded");
  if (options.chained === true) return;
  ui.info("The sign in accounts of supabase/seed.sql are back, for example admin@rnhs.test.");
}

async function resetHosted(ui, target, options, deps) {
  ui.banner("Mesgard database reset", `Project ${target.ref}`);
  ui.warn(`This erases ${await describeContents(target)} of the project ${target.ref}, for good.`);
  ui.detail("Files in Storage stay where they are. Nothing is seeded.");
  const typed = typeof options.confirm === "string" ? options.confirm : await deps.ask(`Type ${target.ref} to erase it: `);
  if (typed.trim() !== target.ref) throw new CliError("That is not the project ref, so nothing was erased.");

  ui.step("Resetting the database");
  deps.resetHosted(target.ref);
  ui.done("Rebuilt from the migrations, empty");
  if (options.chained === true) return;
  ui.section("Next steps");
  ui.list([
    `Run: node scripts/server/index.mjs setup --project-ref ${target.ref}`,
    "It creates the organization and the first administrator again.",
    "People have to sign in again, and the app asks for the server again only if it was changed.",
  ]);
}
