import { formatReport, hasFailure, localMigrationVersions, runChecks } from "./checks.mjs";
import { describeChange, readAuthSettingDifferences } from "./auth-settings.mjs";

/** Reads the state of a server and prints a checklist. Nothing is changed. Returns the exit code. */
export async function runDoctor({ target, repositoryRoot, log = console.log }) {
  const results = await runChecks({
    url: target.url,
    isLocal: target.kind === "local",
    publicKey: target.publicKey(),
    fetch,
    query: target.query,
    localVersions: localMigrationVersions(repositoryRoot),
    readAuthDifferences: target.kind === "hosted"
      ? () => readAuthSettingDifferences(target.ref).map(describeChange)
      : null,
  });
  log(formatReport(results));
  return hasFailure(results) ? 1 : 0;
}
