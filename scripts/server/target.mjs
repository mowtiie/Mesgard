import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { CliError, runSupabaseJson } from "./supabase.mjs";

const PROJECT_REF_PATTERN = /^[a-z]{20}$/;

export function readLinkedRef(repositoryRoot) {
  const file = join(repositoryRoot, "supabase", ".temp", "project-ref");
  return existsSync(file) ? readFileSync(file, "utf8").trim() : null;
}

export function isProjectRef(value) {
  return PROJECT_REF_PATTERN.test(value ?? "");
}

/** Picks the key the app should use: the publishable key, or the legacy anon key on older projects. */
export function publicKeyOf(keys) {
  return (keys.find((key) => key.type === "publishable") ?? keys.find((key) => key.id === "anon"))?.api_key ?? null;
}

export function secretKeyOf(keys) {
  return (keys.find((key) => key.type === "secret") ?? keys.find((key) => key.id === "service_role"))?.api_key ?? null;
}

/**
 * Where the commands run: a hosted project or the local stack. The secret key is only fetched by [revealSecretKey],
 * and the caller keeps it in memory.
 */
export function resolveTarget({ local, projectRef, repositoryRoot }) {
  if (local) return localTarget();
  const ref = projectRef ?? readLinkedRef(repositoryRoot);
  if (!isProjectRef(ref)) {
    throw new CliError("No project selected. Pass --project-ref <ref> or run `supabase link --project-ref <ref>` first.");
  }
  const remote = `--remote=${ref}`;
  const keys = () => runSupabaseJson(["projects", "api-keys", remote]).keys;
  return {
    kind: "hosted",
    ref,
    url: `https://${ref}.supabase.co`,
    publicKey: () => publicKeyOf(keys()),
    revealSecretKey: () => secretKeyOf(runSupabaseJson(["projects", "api-keys", remote, "--reveal"]).keys),
    query: (sql) => runSupabaseJson(["db", "query", remote, "--read-only", sql]).rows,
    remoteFlag: remote,
  };
}

function localTarget() {
  const status = () => runSupabaseJson(["status"]);
  return {
    kind: "local",
    ref: null,
    url: status().API_URL,
    publicKey: () => status().PUBLISHABLE_KEY ?? status().ANON_KEY,
    revealSecretKey: () => status().SECRET_KEY ?? status().SERVICE_ROLE_KEY,
    query: (sql) => runSupabaseJson(["db", "query", "--read-only", sql]).rows,
    remoteFlag: null,
  };
}
