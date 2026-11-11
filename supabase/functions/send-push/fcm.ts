import type { SendOutcome } from "./handler.ts";
import type { buildFcmMessage } from "./payload.ts";

const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const ASSERTION_LIFETIME_SECONDS = 3600;
const EXPIRY_MARGIN_SECONDS = 60;
const INVALID_TOKEN_ERRORS = ["UNREGISTERED", "INVALID_ARGUMENT"];

export interface ServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
}

function base64Url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

function textBase64Url(text: string): string {
  return base64Url(new TextEncoder().encode(text));
}

async function signAssertion(account: ServiceAccount, nowSeconds: number): Promise<string> {
  const header = textBase64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = textBase64Url(
    JSON.stringify({
      iss: account.client_email,
      scope: SCOPE,
      aud: TOKEN_URL,
      iat: nowSeconds,
      exp: nowSeconds + ASSERTION_LIFETIME_SECONDS,
    }),
  );
  const pem = account.private_key.replace(/-----(BEGIN|END) PRIVATE KEY-----|\s/g, "");
  const key = await crypto.subtle.importKey(
    "pkcs8",
    Uint8Array.from(atob(pem), (character) => character.charCodeAt(0)),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claims}`));
  return `${header}.${claims}.${base64Url(new Uint8Array(signature))}`;
}

export function createFcmSender(account: ServiceAccount, fetchImplementation: typeof fetch = fetch) {
  let cached: { token: string; expiresAtSeconds: number } | null = null;

  async function accessToken(): Promise<string> {
    const nowSeconds = Math.floor(Date.now() / 1000);
    if (cached && cached.expiresAtSeconds - EXPIRY_MARGIN_SECONDS > nowSeconds) return cached.token;

    const response = await fetchImplementation(TOKEN_URL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion: await signAssertion(account, nowSeconds),
      }),
    });
    if (!response.ok) throw new Error(`Access token request failed with status ${response.status}`);
    const body = await response.json();
    cached = { token: body.access_token, expiresAtSeconds: nowSeconds + body.expires_in };
    return cached.token;
  }

  return async function sendMessage(message: ReturnType<typeof buildFcmMessage>): Promise<SendOutcome> {
    const response = await fetchImplementation(
      `https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${await accessToken()}`, "Content-Type": "application/json" },
        body: JSON.stringify(message),
      },
    );
    if (response.ok) return "sent";

    const body = await response.json().catch(() => null);
    const errorCode: string | undefined =
      body?.error?.details?.find((detail: { errorCode?: string }) => detail.errorCode)?.errorCode ??
        body?.error?.status;
    console.error(`FCM rejected a message with status ${response.status} (${errorCode ?? "unknown"})`);
    return response.status === 404 || INVALID_TOKEN_ERRORS.includes(errorCode ?? "") ? "invalid_token" : "failed";
  };
}
