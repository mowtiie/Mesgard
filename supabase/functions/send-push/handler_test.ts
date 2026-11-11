import { assertEquals } from "jsr:@std/assert@1";
import { handlePush, type HandlerDependencies, type SendOutcome, WEBHOOK_SECRET_HEADER } from "./handler.ts";
import type { Delivery } from "./payload.ts";

const SECRET = "a-long-enough-webhook-secret";

function delivery(token: string): Delivery {
  return {
    fcm_token: token,
    recipient_id: `user-${token}`,
    preview: "sender_only",
    conversation_id: "conversation-1",
    conversation_type: "direct",
    conversation_title: null,
    sender_name: "Ana",
    body: null,
  };
}

function request(options: { method?: string; secret?: string | null; body?: unknown } = {}): Request {
  const headers = new Headers({ "Content-Type": "application/json" });
  if (options.secret !== null) headers.set(WEBHOOK_SECRET_HEADER, options.secret ?? SECRET);
  return new Request("http://localhost/send-push", {
    method: options.method ?? "POST",
    headers,
    body: JSON.stringify(options.body ?? { message_id: "message-1" }),
  });
}

function dependencies(
  deliveries: Delivery[],
  outcomes: Record<string, SendOutcome> = {},
) {
  const deletedTokens: string[] = [];
  const sentTokens: string[] = [];
  const value: HandlerDependencies = {
    webhookSecret: SECRET,
    listDeliveries: () => Promise.resolve(deliveries),
    sendMessage: (message) => {
      sentTokens.push(message.message.token);
      return Promise.resolve(outcomes[message.message.token] ?? "sent");
    },
    deleteToken: (token) => {
      deletedTokens.push(token);
      return Promise.resolve();
    },
  };
  return { value, deletedTokens, sentTokens };
}

Deno.test("rejects a request without the webhook secret", async () => {
  const { value, sentTokens } = dependencies([delivery("a")]);
  const response = await handlePush(request({ secret: null }), value);
  assertEquals(response.status, 401);
  assertEquals(sentTokens, []);
});

Deno.test("rejects a request with a wrong webhook secret", async () => {
  const { value } = dependencies([delivery("a")]);
  assertEquals((await handlePush(request({ secret: "wrong" }), value)).status, 401);
});

Deno.test("rejects every request when no secret is configured", async () => {
  const { value } = dependencies([delivery("a")]);
  const response = await handlePush(request({ secret: "" }), { ...value, webhookSecret: "" });
  assertEquals(response.status, 401);
});

Deno.test("rejects methods other than POST", async () => {
  const { value } = dependencies([]);
  const response = await handlePush(
    new Request("http://localhost/send-push", { method: "GET", headers: { [WEBHOOK_SECRET_HEADER]: SECRET } }),
    value,
  );
  assertEquals(response.status, 405);
});

Deno.test("rejects a body without a message identifier", async () => {
  const { value } = dependencies([]);
  assertEquals((await handlePush(request({ body: {} }), value)).status, 400);
});

Deno.test("sends one message per delivery and reports the counts", async () => {
  const { value, sentTokens } = dependencies([delivery("a"), delivery("b")]);
  const response = await handlePush(request(), value);
  assertEquals(response.status, 200);
  assertEquals(await response.json(), { sent: 2, removed: 0, failed: 0 });
  assertEquals(sentTokens.sort(), ["a", "b"]);
});

Deno.test("removes tokens that FCM reports as invalid and keeps the others", async () => {
  const { value, deletedTokens } = dependencies(
    [delivery("a"), delivery("b"), delivery("c")],
    { b: "invalid_token", c: "failed" },
  );
  const response = await handlePush(request(), value);
  assertEquals(await response.json(), { sent: 1, removed: 1, failed: 1 });
  assertEquals(deletedTokens, ["b"]);
});

Deno.test("does nothing when the message has no deliveries", async () => {
  const { value, sentTokens } = dependencies([]);
  const response = await handlePush(request(), value);
  assertEquals(await response.json(), { sent: 0, removed: 0, failed: 0 });
  assertEquals(sentTokens, []);
});
