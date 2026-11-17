import {
  findProblems,
  generatePassword,
  organizationSql,
  REQUIRE_TWO_STEP_SQL,
  setupLink,
  slugFrom,
} from "./organization.mjs";
import { CliError } from "./supabase.mjs";

const EMAIL_EXISTS_CODE = "email_exists";
const MIN_SUPPLIED_PASSWORD_LENGTH = 12;

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
  } else if (created?.usedSuppliedPassword) {
    log("");
    log(`First administrator: ${created.email}, with the password you provided (changed at the first sign in).`);
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
  if (options["skip-organization"] === true) {
    log("Skipping the first organization.");
    return null;
  }
  if (organizations > 0 && options["add-organization"] !== true) {
    log(`Found ${organizations} organization(s), so no new one is created. Use --add-organization to add another.`);
    return null;
  }
  const answers = await collectAnswers(options, deps);
  const problems = findProblems(answers);
  if (problems.length > 0) throw new CliError(problems.join("\n"));

  const requireTwoStep = await decideTwoStep(target, options, deps, log);
  const supplied = suppliedPassword(options, deps.env);
  const password = supplied ?? generatePassword();
  const login = await deps.createLogin({
    url: target.url,
    secretKey: deps.secretKey(),
    email: answers.email,
    password,
    displayName: answers.displayName,
  });
  if (!login.created) log(`A login for ${answers.email} already exists. It keeps its current password.`);
  log("Creating the organization");
  deps.execute(requireTwoStep ? `${organizationSql(answers)}\n${REQUIRE_TWO_STEP_SQL}` : organizationSql(answers));
  if (requireTwoStep) log("Two-step verification is now required for administrators and staff");
  return { email: answers.email, password: login.created && !supplied ? password : null, usedSuppliedPassword: login.created && Boolean(supplied) };
}

/**
 * Asked with the other answers, before anything is created. A server that already requires it is left as it is, and
 * a server that does not is never changed without a yes, so an upgrade through the workflow cannot switch it on.
 */
async function decideTwoStep(target, options, deps, log) {
  if (options["require-mfa"] === true && options["no-require-mfa"] === true) {
    throw new CliError("Use either --require-mfa or --no-require-mfa, not both.");
  }
  const [settings] = await target.query("select enforced from private.mfa_settings");
  if (settings?.enforced === true) {
    log("Two-step verification is already required for administrators and staff");
    return false;
  }
  if (options["no-require-mfa"] === true) return false;
  if (options["require-mfa"] === true || options.yes === true) return true;
  const answer = await deps.ask("Require two-step verification for administrators and staff? [Y/n]: ");
  return !/^n(o)?$/i.test(answer.trim());
}

async function collectAnswers(options, deps) {
  const name = text(options["org-name"]) ?? (await deps.ask("Organization name: "));
  const slug = text(options["org-slug"]) ?? (options.yes === true ? slugFrom(name) : (await deps.ask(`Short name [${slugFrom(name)}]: `)) || slugFrom(name));
  const email = text(options["admin-email"]) ?? (await deps.ask("Administrator email: "));
  const displayName = text(options["admin-name"]) ?? (await deps.ask("Administrator name: "));
  return { name: name.trim(), slug: slug.trim(), email: email.trim(), displayName: displayName.trim() };
}

/**
 * A password the administrator chose, read from an environment variable so it never appears in a command line or a
 * log. The mark that makes the app ask for a new password is set either way.
 */
function suppliedPassword(options, environment = {}) {
  const name = text(options["admin-password-env"]);
  if (name === undefined) return null;
  const value = environment[name];
  if (!value || value.length < MIN_SUPPLIED_PASSWORD_LENGTH) {
    throw new CliError(`The environment variable ${name} must hold a password of at least ${MIN_SUPPLIED_PASSWORD_LENGTH} characters.`);
  }
  return value;
}

const text = (value) => (typeof value === "string" ? value : undefined);
