import { assertEquals, assertRejects } from "jsr:@std/assert@1";
import type { Delivery } from "./payload.ts";
import { createWebPushSender, decodeBase64Url, encodeBase64Url, encryptWebPush } from "./webpush.ts";

const encoder = new TextEncoder();
const decoder = new TextDecoder();

// The worked example of RFC 8291, appendix A.
const RFC = {
  plaintext: "When I grow up, I want to be a watermelon",
  userAgentPublic: "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4",
  authSecret: "BTBZMqHH6r4Tts7J_aSIgg",
  serverPublic: "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8",
  serverPrivate: "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw",
  salt: "DGv6ra1nlYgDCS1FRnbzlw",
  body:
    "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN",
};

async function importKeyPair(publicKey: string, privateKey: string): Promise<CryptoKeyPair> {
  const raw = decodeBase64Url(publicKey);
  const jwk = {
    kty: "EC",
    crv: "P-256",
    x: encodeBase64Url(raw.slice(1, 33)),
    y: encodeBase64Url(raw.slice(33, 65)),
  };
  return {
    publicKey: await crypto.subtle.importKey("jwk", jwk, { name: "ECDH", namedCurve: "P-256" }, true, []),
    privateKey: await crypto.subtle.importKey(
      "jwk",
      { ...jwk, d: privateKey },
      { name: "ECDH", namedCurve: "P-256" },
      false,
      ["deriveBits"],
    ),
  };
}

async function hkdf(secret: Uint8Array, salt: Uint8Array, info: string, bits: number): Promise<Uint8Array> {
  const key = await crypto.subtle.importKey("raw", secret, "HKDF", false, ["deriveBits"]);
  return new Uint8Array(
    await crypto.subtle.deriveBits({ name: "HKDF", hash: "SHA-256", salt, info: encoder.encode(info) }, key, bits),
  );
}

// An independent implementation of the receiving side, used to decrypt what the sender produces.
async function decrypt(body: Uint8Array, userAgent: CryptoKeyPair, userAgentPublicRaw: Uint8Array, auth: Uint8Array) {
  const salt = body.slice(0, 16);
  const keyIdLength = body[20];
  const serverPublicRaw = body.slice(21, 21 + keyIdLength);
  const ciphertext = body.slice(21 + keyIdLength);
  const serverPublic = await crypto.subtle.importKey(
    "raw",
    serverPublicRaw,
    { name: "ECDH", namedCurve: "P-256" },
    false,
    [],
  );
  const shared = new Uint8Array(
    await crypto.subtle.deriveBits({ name: "ECDH", public: serverPublic }, userAgent.privateKey, 256),
  );
  const keyInfo = new Uint8Array([...encoder.encode("WebPush: info\0"), ...userAgentPublicRaw, ...serverPublicRaw]);
  const ikm = await crypto.subtle.deriveBits(
    {
      name: "HKDF",
      hash: "SHA-256",
      salt: auth,
      info: keyInfo,
    },
    await crypto.subtle.importKey("raw", shared, "HKDF", false, ["deriveBits"]),
    256,
  );
  const cek = await hkdf(new Uint8Array(ikm), salt, "Content-Encoding: aes128gcm\0", 128);
  const nonce = await hkdf(new Uint8Array(ikm), salt, "Content-Encoding: nonce\0", 96);
  const key = await crypto.subtle.importKey("raw", cek, "AES-GCM", false, ["decrypt"]);
  const padded = new Uint8Array(await crypto.subtle.decrypt({ name: "AES-GCM", iv: nonce }, key, ciphertext));
  return padded.slice(0, padded.lastIndexOf(0x02));
}

Deno.test("reproduces the encrypted message of the RFC 8291 example", async () => {
  const body = await encryptWebPush(encoder.encode(RFC.plaintext), RFC.userAgentPublic, RFC.authSecret, {
    applicationServerKeys: await importKeyPair(RFC.serverPublic, RFC.serverPrivate),
    salt: decodeBase64Url(RFC.salt),
  });
  assertEquals(encodeBase64Url(body), RFC.body);
});

Deno.test("a message encrypted for a subscription can be decrypted with its private key", async () => {
  const userAgent = await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, true, ["deriveBits"]);
  const publicRaw = new Uint8Array(await crypto.subtle.exportKey("raw", userAgent.publicKey));
  const auth = crypto.getRandomValues(new Uint8Array(16));
  const message = '{"title":"Ana","body":"Hello é\u{1F600}"}';

  const body = await encryptWebPush(encoder.encode(message), encodeBase64Url(publicRaw), encodeBase64Url(auth));

  assertEquals(body[20], 65);
  assertEquals(new DataView(body.buffer).getUint32(16), 4096);
  assertEquals(decoder.decode(await decrypt(body, userAgent, publicRaw, auth)), message);
});

Deno.test("two encryptions of the same message differ", async () => {
  const first = await encryptWebPush(encoder.encode("same"), RFC.userAgentPublic, RFC.authSecret);
  const second = await encryptWebPush(encoder.encode("same"), RFC.userAgentPublic, RFC.authSecret);
  assertEquals(encodeBase64Url(first) === encodeBase64Url(second), false);
});

Deno.test("accepts keys with or without base64 padding", async () => {
  const padded = RFC.authSecret + "==";
  const withPadding = await encryptWebPush(encoder.encode("x"), RFC.userAgentPublic, padded, {
    applicationServerKeys: await importKeyPair(RFC.serverPublic, RFC.serverPrivate),
    salt: decodeBase64Url(RFC.salt),
  });
  const without = await encryptWebPush(encoder.encode("x"), RFC.userAgentPublic, RFC.authSecret, {
    applicationServerKeys: await importKeyPair(RFC.serverPublic, RFC.serverPrivate),
    salt: decodeBase64Url(RFC.salt),
  });
  assertEquals(encodeBase64Url(withPadding), encodeBase64Url(without));
});

Deno.test("rejects a public key that is not on the curve", async () => {
  await assertRejects(() => encryptWebPush(encoder.encode("x"), "AAAA", RFC.authSecret));
});

const delivery: Delivery = {
  endpoint: "https://push.example.org/abc",
  p256dh: RFC.userAgentPublic,
  auth_secret: RFC.authSecret,
  recipient_id: "user-1",
  preview: "sender_only",
  conversation_id: "conversation-1",
  conversation_type: "direct",
  conversation_title: null,
  sender_name: "Ana",
  body: null,
};

function senderWith(response: () => Response | Promise<Response>) {
  const calls: { url: string; init: RequestInit }[] = [];
  const implementation = ((input: string | URL | Request, init?: RequestInit) => {
    calls.push({ url: String(input), init: init! });
    return Promise.resolve(response());
  }) as typeof fetch;
  return { send: createWebPushSender(implementation), calls };
}

Deno.test("posts the encrypted message to the endpoint with Web Push headers", async () => {
  const { send, calls } = senderWith(() => new Response(null, { status: 201 }));

  assertEquals(await send(delivery), "sent");

  assertEquals(calls[0].url, "https://push.example.org/abc");
  const headers = calls[0].init.headers as Record<string, string>;
  assertEquals(headers["Content-Encoding"], "aes128gcm");
  assertEquals(headers.TTL, "86400");
  assertEquals(calls[0].init.redirect, "manual");
  assertEquals((calls[0].init.body as Uint8Array).length > 86, true);
});

Deno.test("reports a missing or expired subscription as gone", async () => {
  for (const status of [404, 410]) {
    const { send } = senderWith(() => new Response(null, { status }));
    assertEquals(await send(delivery), "gone");
  }
});

Deno.test("keeps the subscription after a server error, a rejection or a redirect", async () => {
  for (const status of [400, 413, 429, 500, 503, 302]) {
    const { send } = senderWith(() => new Response(null, { status }));
    assertEquals(await send(delivery), "failed", String(status));
  }
});

Deno.test("keeps the subscription when the push service cannot be reached", async () => {
  const { send } = senderWith(() => Promise.reject(new TypeError("network")));
  assertEquals(await send(delivery), "failed");
});

Deno.test("reports a subscription with unusable keys as gone without posting", async () => {
  const { send, calls } = senderWith(() => new Response(null, { status: 201 }));
  assertEquals(await send({ ...delivery, p256dh: "seed-p256dh-key" }), "gone");
  assertEquals(calls.length, 0);
});
