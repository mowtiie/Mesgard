import {
  fieldProblem,
  findProblems,
  generatePassword,
  organizationSql,
  REQUIRE_TWO_STEP_SQL,
  slugFrom,
} from "./organization.mjs";
import { CliError } from "./supabase.mjs";

const MIN_SUPPLIED_PASSWORD_LENGTH = 12;
const MAX_ATTEMPTS = 3;

const text = (value) => (typeof value === "string" ? value : undefined);

/** Creates the first organization and its administrator. Returns what the summary needs, or null when nothing was created. */
export async function createFirstOrganization(target, options, deps, ui) {
  ui.step("Creating the first organization");
  const [{ organizations }] = await target.query("select count(*)::int as organizations from public.organizations");
  if (options["skip-organization"] === true) {
    ui.info("Skipped, as requested");
    return null;
  }
  if (organizations > 0 && options["add-organization"] !== true) {
    ui.info(`Found ${organizations} organization(s), so no new one is created. Use --add-organization to add another.`);
    return null;
  }
  const answers = await collectAnswers(options, deps, ui);
  const problems = findProblems(answers);
  if (problems.length > 0) throw new CliError(problems.join("\n"));

  const twoStep = await decideTwoStep(target, options, deps, ui);
  const supplied = suppliedPassword(options, deps.env);
  const password = supplied ?? generatePassword();
  const login = await deps.createLogin({
    url: target.url,
    secretKey: deps.secretKey(),
    email: answers.email,
    password,
    displayName: answers.displayName,
  });
  if (login.created) ui.done(`Created the login for ${answers.email}`);
  else ui.warn(`A login for ${answers.email} already exists. It keeps its current password.`);

  deps.execute(twoStep.apply ? `${organizationSql(answers)}\n${REQUIRE_TWO_STEP_SQL}` : organizationSql(answers));
  ui.done(`Created ${answers.name} and made ${answers.email} its administrator`);
  if (twoStep.apply) ui.done("Two-step verification is now required for administrators and staff");
  return {
    email: answers.email,
    password: login.created && !supplied ? password : null,
    usedSuppliedPassword: login.created && Boolean(supplied),
    requiredTwoStep: twoStep.required,
  };
}

/** Asks until the answer is usable, so a typing mistake does not end the run. */
async function askUntilValid(deps, ui, question, check, fallback = "") {
  for (let attempt = 1; ; attempt += 1) {
    const answer = ((await deps.ask(question)) || fallback).trim();
    const problem = check(answer);
    if (!problem) return answer;
    if (attempt === MAX_ATTEMPTS) throw new CliError(problem);
    ui.warn(problem);
  }
}

async function collectAnswers(options, deps, ui) {
  const flagged = ["org-name", "admin-email", "admin-name"].every((name) => text(options[name]) !== undefined);
  if (!flagged) ui.info("A few questions. Press Enter to accept a suggestion in brackets.");

  const name = text(options["org-name"]) ?? (await askUntilValid(deps, ui, "Organization name: ", fieldProblem.name));
  const suggestion = slugFrom(name);
  const slug = text(options["org-slug"])
    ?? (options.yes === true ? suggestion : await askUntilValid(deps, ui, `Short name (${suggestion}): `, fieldProblem.slug, suggestion));
  const email = text(options["admin-email"]) ?? (await askUntilValid(deps, ui, "Administrator email: ", fieldProblem.email));
  const displayName = text(options["admin-name"]) ?? (await askUntilValid(deps, ui, "Administrator name: ", fieldProblem.displayName));
  return { name: name.trim(), slug: slug.trim(), email: email.trim(), displayName: displayName.trim() };
}

/**
 * Asked with the other answers, before anything is created. A server that already requires it is left as it is, and
 * a server that does not is never changed without a yes, so an upgrade through the workflow cannot switch it on.
 */
async function decideTwoStep(target, options, deps, ui) {
  if (options["require-mfa"] === true && options["no-require-mfa"] === true) {
    throw new CliError("Use either --require-mfa or --no-require-mfa, not both.");
  }
  const [settings] = await target.query("select enforced from private.mfa_settings");
  if (settings?.enforced === true) {
    ui.info("Two-step verification is already required for administrators and staff");
    return { apply: false, required: true };
  }
  if (options["no-require-mfa"] === true) return { apply: false, required: false };
  if (options["require-mfa"] === true || options.yes === true) return { apply: true, required: true };
  const answer = await deps.ask("Require two-step verification for administrators and staff? [Y/n]: ");
  const required = !/^n(o)?$/i.test(answer.trim());
  return { apply: required, required };
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
