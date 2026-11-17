import { randomInt } from "node:crypto";

const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const EMAIL_PATTERN = /^[^\s@'"\\]+@[^\s@'"\\]+\.[^\s@'"\\]+$/;
const CONTROL_CHARACTERS = /[\u0000-\u001f\u007f]/;
const MAX_NAME_LENGTH = 200;
const MAX_DISPLAY_NAME_LENGTH = 100;
const MAX_SLUG_LENGTH = 60;
const PASSWORD_LENGTH = 16;
const LOWER = "abcdefghijkmnopqrstuvwxyz";
const UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
const DIGITS = "23456789";

/** "Example Organization" becomes "example-organization". */
export function slugFrom(name) {
  return name
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, MAX_SLUG_LENGTH)
    .replace(/-+$/g, "");
}

const isBadText = (text, limit) => !text || text.length > limit || CONTROL_CHARACTERS.test(text);

/** One sentence for an answer that cannot be used, or null. Nothing that passes can break out of an SQL string. */
export const fieldProblem = {
  name: (name) => (isBadText(name, MAX_NAME_LENGTH) ? `The organization name must be 1 to ${MAX_NAME_LENGTH} characters.` : null),
  slug: (slug) => (SLUG_PATTERN.test(slug ?? "") ? null : "The short name may only use lowercase letters, digits and single hyphens."),
  email: (email) => (EMAIL_PATTERN.test(email ?? "") ? null : "The administrator email is not valid."),
  displayName: (displayName) =>
    isBadText(displayName, MAX_DISPLAY_NAME_LENGTH) ? `The administrator name must be 1 to ${MAX_DISPLAY_NAME_LENGTH} characters.` : null,
};

/** Returns the problems of the answers, one sentence each. */
export function findProblems({ name, slug, email, displayName }) {
  return [fieldProblem.name(name), fieldProblem.slug(slug), fieldProblem.email(email), fieldProblem.displayName(displayName)]
    .filter(Boolean);
}

const quote = (text) => `'${text.replaceAll("'", "''")}'`;

/** The same statement as the manual route in README.md, with the answers filled in. */
export function organizationSql({ name, slug, email }) {
  return `with new_organization as (
  insert into public.organizations (name, slug)
  values (${quote(name)}, ${quote(slug)})
  returning id
)
insert into public.org_members (org_id, user_id, role)
select new_organization.id, users.id, 'org_admin'
from new_organization, auth.users as users
where lower(users.email) = lower(${quote(email)});`;
}

/** The statement from the Two-step verification section of README.md that holds administrators and staff to two-step verification. */
export const REQUIRE_TWO_STEP_SQL = "update private.mfa_settings set enforced = true;";

function pick(alphabet) {
  return alphabet[randomInt(alphabet.length)];
}

/** A temporary password the administrator is made to replace at the first sign in. */
export function generatePassword() {
  const all = LOWER + UPPER + DIGITS;
  const characters = [pick(LOWER), pick(UPPER), pick(DIGITS)];
  while (characters.length < PASSWORD_LENGTH) characters.push(pick(all));
  for (let index = characters.length - 1; index > 0; index -= 1) {
    const other = randomInt(index + 1);
    [characters[index], characters[other]] = [characters[other], characters[index]];
  }
  return characters.join("");
}

/** The link the app opens to fill the server form. It carries the public key only. */
export function setupLink(url, publicKey) {
  return `mesgard://setup?url=${encodeURIComponent(url)}&key=${encodeURIComponent(publicKey)}`;
}
