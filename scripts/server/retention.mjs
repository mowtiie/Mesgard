import { CliError } from "./supabase.mjs";
import { createUi } from "./ui.mjs";

const ATTACHMENTS_BUCKET = "message-attachments";
const DEFAULT_BATCH = 200;
const MAX_BATCH = 1000;
const STORAGE_DELETE_BATCH = 100;
const MAX_ROUNDS = 100000;

/** The size of one round, from `--batch`. The database caps a batch at the same number. */
export function parseBatch(value) {
  if (value === undefined) return DEFAULT_BATCH;
  const batch = Number(value);
  if (!Number.isInteger(batch) || batch < 1 || batch > MAX_BATCH) {
    throw new CliError(`--batch is a whole number from 1 to ${MAX_BATCH}.`);
  }
  return batch;
}

/** The new secret keys are not tokens, so they only go in the apikey header. A legacy JWT key goes in both. */
export function storageHeaders(secretKey) {
  const headers = { apikey: secretKey, "Content-Type": "application/json" };
  if (secretKey.includes(".")) headers.Authorization = `Bearer ${secretKey}`;
  return headers;
}

/** Deletes files in Storage. A file that is already gone is not an error, so a repeated run finishes the job. */
export async function deleteFiles({ url, secretKey, paths, fetch }) {
  for (let start = 0; start < paths.length; start += STORAGE_DELETE_BATCH) {
    const response = await fetch(`${url}/storage/v1/object/${ATTACHMENTS_BUCKET}`, {
      method: "DELETE",
      headers: storageHeaders(secretKey),
      body: JSON.stringify({ prefixes: paths.slice(start, start + STORAGE_DELETE_BATCH) }),
    });
    if (!response.ok) throw new CliError(`Deleting files failed: the storage service answered ${response.status}.`);
  }
}

/**
 * Erases the messages that are older than the limit of their organization, for good. Per round the files go first and
 * the database second, so a failure halfway leaves rows that still point at files and the next run finishes it. A legal
 * hold or an open report protects a message, and the database decides that, so nothing here can go around it.
 */
export async function runRetention({ target, options, deps, isExplicit, log = console.log, unicode = false, color = false }) {
  const ui = createUi({ log, unicode, color });
  if (!isExplicit) {
    throw new CliError("Say where to run: --local for the local stack, or --project-ref <ref> for a hosted project.");
  }
  ui.banner("Mesgard retention", target.kind === "hosted" ? `Project ${target.ref}` : "Local Supabase stack");
  const batch = parseBatch(options.batch);
  const [{ organizations }] = await target.query(
    "select count(*)::int as organizations from public.organizations where message_retention_days is not null",
  );
  if (organizations === 0) {
    ui.info("No organization has set a retention limit, so nothing is erased.");
    return { messages: 0, files: 0 };
  }
  if (options["dry-run"] === true) return describeWaiting(ui, target, organizations);

  let messages = 0;
  let files = 0;
  let secretKey = null;
  for (let round = 0; round < MAX_ROUNDS; round += 1) {
    const [{ waiting }] = await target.query(
      `select count(*)::int as waiting from private.expired_message_ids(${batch})`,
    );
    if (waiting === 0) break;
    const rows = await target.query(`select path from public.list_expired_attachment_paths(${batch}) as path`);
    const paths = rows.map((row) => row.path);
    if (paths.length > 0) {
      secretKey ??= target.revealSecretKey();
      await deleteFiles({ url: target.url, secretKey, paths, fetch: deps.fetch });
      files += paths.length;
    }
    deps.execute(`select public.expire_messages(${batch});`);
    messages += waiting;
  }

  ui.done(`${messages} messages erased in ${organizations} organization${organizations === 1 ? "" : "s"} with a limit`);
  ui.done(`${files} files deleted`);
  if (messages === 0) ui.info("Nothing was old enough, or what was is protected by a legal hold or an open report.");
  return { messages, files };
}

async function describeWaiting(ui, target, organizations) {
  const [{ waiting }] = await target.query(
    `select count(*)::int as waiting from private.expired_message_ids(${MAX_BATCH})`,
  );
  ui.info(`${waiting >= MAX_BATCH ? `${MAX_BATCH} or more` : waiting} messages are old enough and not protected`);
  ui.detail(`${organizations} organization${organizations === 1 ? " has" : "s have"} a limit. Nothing was changed.`);
  return { messages: 0, files: 0 };
}
