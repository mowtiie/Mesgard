const PLACEHOLDER_SUFFIX = "@erased.invalid";
const PLACEHOLDER_PREFIX = "erased-";

/** What replaces the email of an erased person. It cannot receive mail and nobody can sign in with it. */
export function placeholderEmail(userId: string): string {
  return `${PLACEHOLDER_PREFIX}${userId}${PLACEHOLDER_SUFFIX}`;
}

export function isErasedEmail(email: string | null): boolean {
  return email !== null && email.startsWith(PLACEHOLDER_PREFIX) && email.endsWith(PLACEHOLDER_SUFFIX);
}
