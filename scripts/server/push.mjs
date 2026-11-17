import { randomBytes } from "node:crypto";
import { CliError } from "./supabase.mjs";

const HOSTS_PATTERN = /^(\*\.)?[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*(:\d{1,5})?$/i;
const SECRET_BYTES = 32;
const VERIFY_ATTEMPTS = 6;
const VERIFY_DELAY_MS = 3000;
const UNKNOWN_MESSAGE_ID = "00000000-0000-0000-0000-000000000000";

export const generateWebhookSecret = () => randomBytes(SECRET_BYTES).toString("hex");

/** A comma separated list of push server hosts, or an empty list for the public servers. */
export function parseHosts(text) {
  const hosts = (text ?? "").split(",").map((host) => host.trim()).filter(Boolean);
  const invalid = hosts.find((host) => !HOSTS_PATTERN.test(host));
  if (invalid) throw new CliError(`"${invalid}" is not a valid push server host.`);
  return hosts;
}

const quote = (text) => `'${text.replaceAll("'", "''")}'`;

/** The row that tells the database where the function is, with the same secret the function holds. */
export function pushSettingsSql(functionUrl, secret) {
  return `insert into private.push_settings (function_url, webhook_secret)
values (${quote(functionUrl)}, ${quote(secret)})
on conflict (singleton) do update
set function_url = excluded.function_url, webhook_secret = excluded.webhook_secret;`;
}

export const functionUrlFor = (projectUrl) => `${projectUrl}/functions/v1/send-push`;

/**
 * Asks the function about a message that does not exist. A function that holds the same secret answers with a count of
 * zero, so nothing is sent to anyone.
 */
export async function checkPushFunction({ functionUrl, secret, fetch }) {
  const response = await fetch(functionUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json", "x-webhook-secret": secret },
    body: JSON.stringify({ message_id: UNKNOWN_MESSAGE_ID }),
  });
  if (response.status === 404) return { ok: false, reason: "send-push is not deployed" };
  if (response.status === 401) return { ok: false, reason: "the function holds a different secret than the database" };
  return response.ok ? { ok: true } : { ok: false, reason: `the function answered ${response.status}` };
}

async function verifyWithRetries(options) {
  let result;
  for (let attempt = 1; attempt <= VERIFY_ATTEMPTS; attempt += 1) {
    result = await checkPushFunction(options);
    if (result.ok || attempt === VERIFY_ATTEMPTS) break;
    await options.wait(VERIFY_DELAY_MS);
  }
  return result;
}

export async function runPush({ target, options, deps, log = console.log }) {
  if (target.kind !== "hosted") {
    log("Local stack: supabase/seed.sql already sets up push, so there is nothing to do.");
    return;
  }
  const rotate = options.rotate === true;
  const [existing] = await target.query("select function_url from private.push_settings");
  const hosts = options["push-hosts"] === undefined ? null : parseHosts(options["push-hosts"]);

  log("Deploying the push function (send-push)");
  deps.deployFunction(target.ref, "send-push");

  let secret;
  if (existing && !rotate) {
    log("Push is already set up, so its secret stays. Use --rotate to replace it.");
    secret = (await target.query("select webhook_secret from private.push_settings"))[0].webhook_secret;
    if (hosts) deps.setSecrets(target.ref, { PUSH_ALLOWED_ENDPOINT_HOSTS: hosts.join(",") });
  } else {
    secret = generateWebhookSecret();
    log("Setting the function secret and telling the database where the function is");
    const values = { PUSH_WEBHOOK_SECRET: secret };
    if (hosts) values.PUSH_ALLOWED_ENDPOINT_HOSTS = hosts.join(",");
    deps.setSecrets(target.ref, values);
    deps.execute(pushSettingsSql(functionUrlFor(target.url), secret));
  }

  log("Checking that the function and the database agree");
  const result = await verifyWithRetries({
    functionUrl: functionUrlFor(target.url),
    secret,
    fetch: deps.fetch,
    wait: deps.wait,
  });
  if (!result.ok) throw new CliError(`Push is not working yet: ${result.reason}. Run this again, or see docs/push-notifications.md.`);
  log("Push is ready.");
  log("");
  log("Each person installs a push app (for example ntfy) and chooses it in Mesgard under Settings, Notifications.");
  log(hosts?.length
    ? `Allowed push servers: ${hosts.join(", ")}`
    : "No push server list was given, so any public push server is accepted. Pass --push-hosts to restrict it.");
}
