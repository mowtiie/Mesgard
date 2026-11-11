import { assertEquals, assertRejects } from "jsr:@std/assert@1";
import { createFcmSender, type ServiceAccount } from "./fcm.ts";
import { buildFcmMessage, type Delivery } from "./payload.ts";

async function generateServiceAccount(): Promise<ServiceAccount> {
  const pair = await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"],
  );
  const pkcs8 = new Uint8Array(await crypto.subtle.exportKey("pkcs8", pair.privateKey));
  const body = btoa(String.fromCharCode(...pkcs8));
  return {
    project_id: "mesgard-test",
    client_email: "push@mesgard-test.iam.gserviceaccount.com",
    private_key: `-----BEGIN PRIVATE KEY-----\n${body}\n-----END PRIVATE KEY-----\n`,
  };
}

const message = buildFcmMessage(
  {
    fcm_token: "token-1",
    recipient_id: "user-1",
    preview: "sender_only",
    conversation_id: "conversation-1",
    conversation_type: "direct",
    conversation_title: null,
    sender_name: "Ana",
    body: null,
  } satisfies Delivery,
);

function fakeFetch(sendResponse: () => Response) {
  const calls: { url: string; init?: RequestInit }[] = [];
  const implementation = ((input: string | URL | Request, init?: RequestInit) => {
    const url = String(input);
    calls.push({ url, init });
    if (url.startsWith("https://oauth2.googleapis.com/token")) {
      return Promise.resolve(Response.json({ access_token: "access-1", expires_in: 3600 }));
    }
    return Promise.resolve(sendResponse());
  }) as typeof fetch;
  return { implementation, calls };
}

Deno.test("sends the message to the project with a bearer token", async () => {
  const { implementation, calls } = fakeFetch(() => Response.json({ name: "projects/x/messages/1" }));
  const send = createFcmSender(await generateServiceAccount(), implementation);

  assertEquals(await send(message), "sent");

  const sendCall = calls.find((call) => call.url.includes("messages:send"))!;
  assertEquals(sendCall.url, "https://fcm.googleapis.com/v1/projects/mesgard-test/messages:send");
  assertEquals((sendCall.init!.headers as Record<string, string>).Authorization, "Bearer access-1");
  assertEquals(JSON.parse(sendCall.init!.body as string), message);
});

Deno.test("signs the token request with a JWT assertion", async () => {
  const { implementation, calls } = fakeFetch(() => Response.json({}));
  await createFcmSender(await generateServiceAccount(), implementation)(message);

  const form = calls[0].init!.body as URLSearchParams;
  assertEquals(form.get("grant_type"), "urn:ietf:params:oauth:grant-type:jwt-bearer");
  assertEquals(form.get("assertion")!.split(".").length, 3);
});

Deno.test("reuses the access token between messages", async () => {
  const { implementation, calls } = fakeFetch(() => Response.json({}));
  const send = createFcmSender(await generateServiceAccount(), implementation);

  await send(message);
  await send(message);

  assertEquals(calls.filter((call) => call.url.startsWith("https://oauth2.googleapis.com")).length, 1);
});

Deno.test("reports an unregistered token as invalid", async () => {
  const { implementation } = fakeFetch(() =>
    Response.json({ error: { status: "NOT_FOUND", details: [{ errorCode: "UNREGISTERED" }] } }, { status: 404 })
  );
  assertEquals(await createFcmSender(await generateServiceAccount(), implementation)(message), "invalid_token");
});

Deno.test("reports an invalid argument as an invalid token", async () => {
  const { implementation } = fakeFetch(() => Response.json({ error: { status: "INVALID_ARGUMENT" } }, { status: 400 }));
  assertEquals(await createFcmSender(await generateServiceAccount(), implementation)(message), "invalid_token");
});

Deno.test("reports a server error as a failure that keeps the token", async () => {
  const { implementation } = fakeFetch(() => Response.json({ error: { status: "UNAVAILABLE" } }, { status: 503 }));
  assertEquals(await createFcmSender(await generateServiceAccount(), implementation)(message), "failed");
});

Deno.test("fails when the access token cannot be obtained", async () => {
  const implementation = (() => Promise.resolve(new Response("denied", { status: 401 }))) as typeof fetch;
  const send = createFcmSender(await generateServiceAccount(), implementation);
  await assertRejects(() => send(message), Error, "status 401");
});
