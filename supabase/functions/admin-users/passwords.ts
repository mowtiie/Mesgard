// No 0, O, 1, l or I, so a password read from a printed slip is not mistyped.
const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
const PASSWORD_LENGTH = 12;
const BYTE_RANGE = 256;

export type RandomBytes = (length: number) => Uint8Array;

const systemRandomBytes: RandomBytes = (length) => crypto.getRandomValues(new Uint8Array(length));

/**
 * A random password from the alphabet above. Bytes that would favor some characters are discarded, so every
 * character is equally likely.
 */
export function generateTemporaryPassword(randomBytes: RandomBytes = systemRandomBytes): string {
  const limit = BYTE_RANGE - (BYTE_RANGE % ALPHABET.length);
  let password = "";
  while (password.length < PASSWORD_LENGTH) {
    for (const byte of randomBytes(PASSWORD_LENGTH * 2)) {
      if (byte < limit && password.length < PASSWORD_LENGTH) password += ALPHABET[byte % ALPHABET.length];
    }
  }
  return password;
}
