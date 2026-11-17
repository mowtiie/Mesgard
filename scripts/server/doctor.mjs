import { describeChange, readAuthSettingDifferences } from "./auth-settings.mjs";
import { FAIL, INFO, PASS, WARN, hasFailure, localMigrationVersions, requiredApiLevel, runChecks } from "./checks.mjs";
import { createUi } from "./ui.mjs";

const KIND = { [PASS]: "done", [WARN]: "warn", [FAIL]: "fail", [INFO]: "info" };

/** Prints one line per check, with the fix under anything that needs doing, and a closing sentence. */
export function printReport(ui, results) {
  for (const entry of results) {
    ui.result(KIND[entry.status], entry.label, entry.detail);
    if (entry.hint && entry.status !== PASS) ui.detail(entry.hint);
  }
  const failures = results.filter((entry) => entry.status === FAIL).length;
  const warnings = results.filter((entry) => entry.status === WARN).length;
  ui.blank();
  if (failures > 0) ui.problem(`${failures} ${failures === 1 ? "check" : "checks"} failed. Fix them, then run doctor again.`);
  else if (warnings > 0) ui.warn(`Working, with ${warnings} ${warnings === 1 ? "warning" : "warnings"} to look at.`);
  else ui.done("Everything required is working.");
}

/**
 * Reads the state of a server and prints a checklist. Nothing is changed. Returns the exit code. [embedded] is for the
 * end of setup, which has already printed its own heading.
 */
export async function runDoctor({ target, repositoryRoot, log = console.log, unicode = false, color = false, embedded = false }) {
  const ui = createUi({ log, unicode, color });
  if (embedded) ui.section("Checking the server");
  else ui.banner("Mesgard server check", target.kind === "hosted" ? `Project ${target.ref}` : "Local Supabase stack");
  const results = await runChecks({
    url: target.url,
    isLocal: target.kind === "local",
    publicKey: target.publicKey(),
    fetch,
    query: target.query,
    localVersions: localMigrationVersions(repositoryRoot),
    requiredApiLevel: requiredApiLevel(repositoryRoot),
    readAuthDifferences: target.kind === "hosted"
      ? () => readAuthSettingDifferences(target.ref).map(describeChange)
      : null,
  });
  printReport(ui, results);
  return hasFailure(results) ? 1 : 0;
}
