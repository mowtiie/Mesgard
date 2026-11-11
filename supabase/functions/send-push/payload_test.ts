import { assertEquals, assertNotMatch } from "jsr:@std/assert@1";
import { buildContent, buildFcmMessage, type Delivery, GENERIC_BODY, MAX_TEXT_LENGTH, truncate } from "./payload.ts";

function delivery(overrides: Partial<Delivery> = {}): Delivery {
  return {
    fcm_token: "token-1",
    recipient_id: "user-1",
    preview: "sender_only",
    conversation_id: "conversation-1",
    conversation_type: "direct",
    conversation_title: null,
    sender_name: "Ana Reyes",
    body: null,
    ...overrides,
  };
}

Deno.test("hidden mode shows only the application name", () => {
  const content = buildContent(delivery({ preview: "hidden", sender_name: null }));
  assertEquals(content, { title: "Mesgard", body: GENERIC_BODY });
});

Deno.test("sender only mode shows the sender of a direct message", () => {
  assertEquals(buildContent(delivery()), { title: "Ana Reyes", body: GENERIC_BODY });
});

Deno.test("sender only mode shows the conversation and sender of a group message", () => {
  const content = buildContent(delivery({ conversation_type: "group", conversation_title: "Grade 10 Section A" }));
  assertEquals(content, { title: "Grade 10 Section A", body: "Ana Reyes sent a message" });
});

Deno.test("sender and text mode shows the text of a direct message", () => {
  const content = buildContent(delivery({ preview: "sender_and_text", body: "See you at 8." }));
  assertEquals(content, { title: "Ana Reyes", body: "See you at 8." });
});

Deno.test("sender and text mode prefixes the sender in a group message", () => {
  const content = buildContent(
    delivery({
      preview: "sender_and_text",
      conversation_type: "announcement",
      conversation_title: "School Announcements",
      body: "Classes are suspended.",
    }),
  );
  assertEquals(content, { title: "School Announcements", body: "Ana Reyes: Classes are suspended." });
});

Deno.test("sender and text mode falls back to a generic body when the text is missing", () => {
  assertEquals(buildContent(delivery({ preview: "sender_and_text", body: null })).body, GENERIC_BODY);
});

Deno.test("long text is truncated with an ellipsis", () => {
  const text = truncate("a".repeat(MAX_TEXT_LENGTH + 50));
  assertEquals(Array.from(text).length, MAX_TEXT_LENGTH);
  assertEquals(text.endsWith("…"), true);
});

Deno.test("short text is kept and trimmed", () => {
  assertEquals(truncate("  Hello  "), "Hello");
});

Deno.test("truncation does not split a character made of two code units", () => {
  const text = truncate("\u{1F600}".repeat(MAX_TEXT_LENGTH + 5));
  assertEquals(Array.from(text).length, MAX_TEXT_LENGTH);
});

Deno.test("the FCM message is a high priority data message for the token", () => {
  const { message } = buildFcmMessage(delivery());
  assertEquals(message.token, "token-1");
  assertEquals(message.android.priority, "HIGH");
  assertEquals(message.data, { conversation_id: "conversation-1", title: "Ana Reyes", body: GENERIC_BODY });
  assertEquals("notification" in message, false);
});

Deno.test("the FCM message never contains text that the preview mode does not allow", () => {
  const serialized = JSON.stringify(buildFcmMessage(delivery({ preview: "sender_only", body: "secret text" })));
  assertNotMatch(serialized, /secret text/);
});
