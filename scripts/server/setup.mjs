import { findProblems, generatePassword, organizationSql, setupLink, slugFrom } from "./organization.mjs";
import { CliError } from "./supabase.mjs";

const EMAIL_EXISTS_CODE = "email_exists";

/** Creates the administrator's login through the Auth admin API. The secret key stays in this function's memory. */
export async function createLogin({ url, secretKey, email, password, displayName }) {
  const headers = { apikey: secretKey, "Content-Type": "application/json" };
  if (!secretKey.startsWith("sb_secret_")) headers.Authorization = `Bearer ${secretKey}`;
  const response = await fetch(`${url}/auth/v1/admin/users`, {
    method: "POST",
    headers,
    body: JSON.stringify({
      email,
      password,
      email_confirm: true,
      user_metadata: { display_name: displayName },
      app_metadata: { must_change_password: true },
    }),
  });
  if (response.ok) return { created: true };
  const body = await response.json().catch(() => ({}));
  if (response.status === 422 && body.error_code === EMAIL_EXISTS_CODE) return { created: false };
  throw new CliError(`Creating the login failed (${response.status}): ${body.msg ?? body.message ?? "no details"}`);
}

export async function runSetup({ target, options, deps, log = console.log }) {
  if (target.kind === "hosted") await prepareHostedProject(target, options, deps, log);
  else log("Local stack: the database comes from `supabase db reset`, so migrations and functions are skipped.");

  const created = await createFirstOrganization(target, options, deps, log);
  const publicKey = target.publicKey();
  log("");
  log("Connect the app");
  log(`  Server: ${target.url}`);
  log(`  Key:    ${publicKey}  (public key, safe to share)`);
  log(`  Link:   ${setupLink(target.url, publicKey)}`);
  if (created?.password) {
    log("");
    log(`First administrator: ${created.email}`);
    log(`Temporary password (shown once, change it at the first sign in): ${created.password}`);
  }
  return created;
}

async function prepareHostedProject(target, options, deps, log) {
  if (deps.linkedRef() !== target.ref) {
    log("Linking the project");
    deps.link(target.ref);
  }
  log("Applying database migrations");
  deps.pushMigrations(target.ref);
  log("Deploying the account function (admin-users)");
  deps.deployFunction(target.ref);
  await applyAuthSettings(target, options, deps, log);
}

async function applyAuthSettings(target, options, deps, log) {
  const differences = deps.authDifferences(target.ref);
  if (differences.length === 0) return log("Auth settings are already right (sign-ups off, TOTP enabled)");
  log("Auth settings to change:");
  differences.forEach((line) => log(`  ${line}`));
  const approved = options.yes === true || (await deps.confirm("Apply these Auth settings? [y/N] "));
  if (!approved) return log("Skipped the Auth settings. Change them in the dashboard or run setup again.");
  deps.applyAuthSettings(target.ref);
  log("Auth settings applied");
}

async function createFirstOrganization(target, options, deps, log) {
  const [{ organizations }] = await target.query("select count(*)::int as organizations from public.organizations");
  if (organizations > 0 && options["add-organization"] !== true) {
    log(`Found ${organizations} organization(s), so no new one is created. Use --add-organization to add another.`);
    return null;
  }
  const answers = await collectAnswers(options, deps);
  const problems = findProblems(answers);
  if (problems.length > 0) throw new CliError(problems.join("\n"));

  const password = generatePassword();
  const login = await deps.createLogin({
    url: target.url,
    secretKey: deps.secretKey(),
    email: answers.email,
    password,
    displayName: answers.displayName,
  });
  if (!login.created) log(`A login for ${answers.email} already exists. It keeps its current password.`);
  log("Creating the organization");
  deps.execute(organizationSql(answers));
  return { email: answers.email, password: login.created ? password : null };
}

async function collectAnswers(options, deps) {
  const name = text(options["org-name"]) ?? (await deps.ask("Organization name: "));
  const slug = text(options["org-slug"]) ?? ((await deps.ask(`Short name [${slugFrom(name)}]: `)) || slugFrom(name));
  const email = text(options["admin-email"]) ?? (await deps.ask("Administrator email: "));
  const displayName = text(options["admin-name"]) ?? (await deps.ask("Administrator name: "));
  return { name: name.trim(), slug: slug.trim(), email: email.trim(), displayName: displayName.trim() };
}

const text = (value) => (typeof value === "string" ? value : undefined);
