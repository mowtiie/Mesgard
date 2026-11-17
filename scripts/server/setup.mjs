import { createFirstOrganization } from "./first-organization.mjs";
import { setupLink } from "./organization.mjs";
import { CliError } from "./supabase.mjs";
import { createUi } from "./ui.mjs";

const EMAIL_EXISTS_CODE = "email_exists";
const HOSTED_STEPS = 5;
const LOCAL_STEPS = 1;

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

export async function runSetup({ target, options, deps, log = console.log, unicode = false, color = false }) {
  const ui = createUi({ log, unicode, color });
  const hosted = target.kind === "hosted";
  ui.banner(
    "Mesgard setup",
    hosted ? `Project ${target.ref}` : "Local Supabase stack (the database comes from supabase db reset)",
  );
  ui.plan(hosted ? HOSTED_STEPS : LOCAL_STEPS);
  if (hosted) await prepareHostedProject(target, options, deps, ui);

  const created = await createFirstOrganization(target, options, deps, ui);
  summarize(ui, target, created);
  return created;
}

async function prepareHostedProject(target, options, deps, ui) {
  ui.step("Linking the project");
  if (deps.linkedRef() === target.ref) {
    ui.done("Already linked");
  } else {
    deps.link(target.ref);
    ui.done(`Linked ${target.ref}`);
  }

  ui.step("Applying database migrations");
  ui.info("This can take a minute. If the Supabase CLI asks for the database password, enter the one you set for the project.");
  deps.pushMigrations(target.ref);
  ui.done("The database is up to date");

  ui.step("Deploying the account function");
  deps.deployFunction(target.ref);
  ui.done("admin-users is deployed");

  await applyAuthSettings(target, options, deps, ui);
}

async function applyAuthSettings(target, options, deps, ui) {
  ui.step("Checking the sign-in settings");
  const differences = deps.authDifferences(target.ref);
  if (differences.length === 0) return ui.done("Sign-ups are off and two-step verification (TOTP) is enabled");
  ui.info("Mesgard needs these Auth settings. Nothing else is changed.");
  differences.forEach((difference) => ui.detail(difference));
  const approved = options.yes === true || (await deps.confirm("Apply these Auth settings? [Y/n]: "));
  if (!approved) return ui.warn("Skipped. Change them in the dashboard or run setup again.");
  deps.applyAuthSettings(target.ref);
  return ui.done("Auth settings applied");
}

function summarize(ui, target, created) {
  const publicKey = target.publicKey();
  ui.section("Connect the app");
  ui.rows([
    ["Server", target.url],
    ["Key", publicKey, "public key, safe to share"],
    ["Link", setupLink(target.url, publicKey)],
  ]);
  if (!created) return;

  ui.section("First administrator");
  const hasTemporaryPassword = Boolean(created.password) || created.usedSuppliedPassword;
  ui.rows([
    ["Email", created.email],
    ...(created.password
      ? [["Password", created.password, "temporary, shown once"]]
      : [["Password", created.usedSuppliedPassword ? "the one you provided" : "unchanged, the login already existed"]]),
  ]);

  ui.section("Next steps");
  ui.list([
    "Install Mesgard on the phone, then open the link above. You can also type the server address and key.",
    `Sign in with ${created.email}${created.password ? " and the password above" : created.usedSuppliedPassword ? " and the password you provided" : ""}.`,
    ...(created.requiredTwoStep ? ["Set up the authenticator app when Mesgard asks."] : []),
    ...(hasTemporaryPassword ? ["Choose your own password when Mesgard asks."] : []),
    "Add the other people under Settings, Administration.",
  ]);
}
