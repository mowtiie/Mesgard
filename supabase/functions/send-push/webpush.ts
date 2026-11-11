import type { SendOutcome } from "./handler.ts";
import { buildPushBody, type Delivery } from "./payload.ts";

const RECORD_SIZE = 4096;
const SALT_LENGTH = 16;
const LAST_RECORD_DELIMITER = 0x02;
const TTL_SECONDS = 86400;
const REQUEST_TIMEOUT_MS = 10_000;
const GONE_STATUSES = [404, 410];

const encoder = new TextEncoder();

export function decodeBase64Url(value: string): Uint8Array {
  const base64 = value.replaceAll("-", "+").replaceAll("_", "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  return Uint8Array.from(atob(base64), (character) => character.charCodeAt(0));
}

export function encodeBase64Url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

function concatenate(...parts: Uint8Array[]): Uint8Array {
  const result = new Uint8Array(parts.reduce((total, part) => total + part.length, 0));
  let offset = 0;
  for (const part of parts) {
    result.set(part, offset);
    offset += part.length;
  }
  return result;
}

async function hkdf(secret: Uint8Array, salt: Uint8Array, info: Uint8Array, bits: number): Promise<Uint8Array> {
  const key = await crypto.subtle.importKey("raw", secret, "HKDF", false, ["deriveBits"]);
  return new Uint8Array(await crypto.subtle.deriveBits({ name: "HKDF", hash: "SHA-256", salt, info }, key, bits));
}

export interface EncryptionOptions {
  applicationServerKeys?: CryptoKeyPair;
  salt?: Uint8Array;
}

// Encrypts a message for a Web Push subscription with the aes128gcm content coding (RFC 8291).
export async function encryptWebPush(
  plaintext: Uint8Array,
  userAgentPublicKey: string,
  authSecret: string,
  options: EncryptionOptions = {},
): Promise<Uint8Array> {
  const userAgentPublic = decodeBase64Url(userAgentPublicKey);
  const auth = decodeBase64Url(authSecret);
  const userAgentKey = await crypto.subtle.importKey(
    "raw",
    userAgentPublic,
    { name: "ECDH", namedCurve: "P-256" },
    false,
    [],
  );
  const serverKeys = options.applicationServerKeys ??
    await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, false, ["deriveBits"]);
  const serverPublic = new Uint8Array(await crypto.subtle.exportKey("raw", serverKeys.publicKey));
  const sharedSecret = new Uint8Array(
    await crypto.subtle.deriveBits({ name: "ECDH", public: userAgentKey }, serverKeys.privateKey, 256),
  );

  const keyInfo = concatenate(encoder.encode("WebPush: info\0"), userAgentPublic, serverPublic);
  const inputKeyMaterial = await hkdf(sharedSecret, auth, keyInfo, 256);
  const salt = options.salt ?? crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
  const contentKey = await hkdf(inputKeyMaterial, salt, encoder.encode("Content-Encoding: aes128gcm\0"), 128);
  const nonce = await hkdf(inputKeyMaterial, salt, encoder.encode("Content-Encoding: nonce\0"), 96);

  const aesKey = await crypto.subtle.importKey("raw", contentKey, "AES-GCM", false, ["encrypt"]);
  const padded = concatenate(plaintext, new Uint8Array([LAST_RECORD_DELIMITER]));
  const ciphertext = new Uint8Array(await crypto.subtle.encrypt({ name: "AES-GCM", iv: nonce }, aesKey, padded));

  const recordSize = new Uint8Array(4);
  new DataView(recordSize.buffer).setUint32(0, RECORD_SIZE);
  return concatenate(salt, recordSize, new Uint8Array([serverPublic.length]), serverPublic, ciphertext);
}

export function createWebPushSender(fetchImplementation: typeof fetch = fetch) {
  return async function send(delivery: Delivery): Promise<SendOutcome> {
    let body: Uint8Array;
    try {
      body = await encryptWebPush(encoder.encode(buildPushBody(delivery)), delivery.p256dh, delivery.auth_secret);
    } catch (error) {
      console.error("A push subscription has unusable keys", error instanceof Error ? error.name : error);
      return "gone";
    }

    try {
      const response = await fetchImplementation(delivery.endpoint, {
        method: "POST",
        headers: {
          "Content-Encoding": "aes128gcm",
          "Content-Type": "application/octet-stream",
          TTL: String(TTL_SECONDS),
          Urgency: "high",
        },
        body,
        redirect: "manual",
        signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
      });
      await response.body?.cancel();
      if (response.ok) return "sent";
      console.error(`The push service rejected a message with status ${response.status}`);
      return GONE_STATUSES.includes(response.status) ? "gone" : "failed";
    } catch (error) {
      console.error("The push service could not be reached", error instanceof Error ? error.name : error);
      return "failed";
    }
  };
}
