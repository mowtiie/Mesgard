import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { runSupabase, runSupabaseJson } from "./supabase.mjs";

/**
 * The only Auth settings Mesgard asks for. The file is separate from supabase/config.toml on purpose: that one holds
 * local development values (such as the site URL) that must never overwrite a real project.
 */
export const AUTH_SETTINGS_TOML = `project_id = "mesgard"

[auth]
enable_signup = false

[auth.mfa.totp]
enroll_enabled = true
verify_enabled = true
`;

/** Keeps only the differences in properties the settings file declares. The rest of the project is none of our business. */
export function declaredChanges(diff) {
  return (diff.changes ?? []).filter((change) => change.declared !== false);
}

export function describeChange(change) {
  return `${change.path.join(".")}: ${JSON.stringify(change.remote)} -> ${JSON.stringify(change.local)}`;
}

function withSettingsFolder(action) {
  const folder = mkdtempSync(join(tmpdir(), "mesgard-auth-"));
  try {
    mkdirSync(join(folder, "supabase"));
    writeFileSync(join(folder, "supabase", "config.toml"), AUTH_SETTINGS_TOML);
    return action(folder);
  } finally {
    rmSync(folder, { recursive: true, force: true });
  }
}

export function readAuthSettingDifferences(ref) {
  return withSettingsFolder((workdir) =>
    declaredChanges(runSupabaseJson(["config", "diff", `--remote=${ref}`], { workdir }))
  );
}

export function applyAuthSettings(ref) {
  return withSettingsFolder((workdir) => runSupabase(["config", "push", `--remote=${ref}`, "--yes"], { workdir }));
}
