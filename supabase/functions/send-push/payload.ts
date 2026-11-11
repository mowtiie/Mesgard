export type PreviewMode = "sender_and_text" | "sender_only" | "hidden";

export interface Delivery {
  fcm_token: string;
  recipient_id: string;
  preview: PreviewMode;
  conversation_id: string;
  conversation_type: "direct" | "group" | "announcement";
  conversation_title: string | null;
  sender_name: string | null;
  body: string | null;
}

export interface PushContent {
  title: string;
  body: string;
}

export const APP_NAME = "Mesgard";
export const GENERIC_BODY = "New message";
export const MAX_TEXT_LENGTH = 200;

export function truncate(text: string, maxLength = MAX_TEXT_LENGTH): string {
  const characters = Array.from(text.trim());
  if (characters.length <= maxLength) return characters.join("");
  return characters.slice(0, maxLength - 1).join("") + "…";
}

// The content only uses fields that list_push_deliveries returns for the preview mode of the recipient.
export function buildContent(delivery: Delivery): PushContent {
  if (delivery.preview === "hidden") {
    return { title: APP_NAME, body: GENERIC_BODY };
  }

  const sender = delivery.sender_name ?? APP_NAME;
  const isDirect = delivery.conversation_type === "direct";
  const title = isDirect ? sender : (delivery.conversation_title ?? APP_NAME);

  if (delivery.preview === "sender_and_text" && delivery.body) {
    const text = truncate(delivery.body);
    return { title, body: isDirect ? text : `${sender}: ${text}` };
  }
  return { title, body: isDirect ? GENERIC_BODY : `${sender} sent a message` };
}

// A data message lets the app build the notification, so no text is added by the FCM service or the system.
export function buildFcmMessage(delivery: Delivery) {
  const { title, body } = buildContent(delivery);
  return {
    message: {
      token: delivery.fcm_token,
      data: {
        conversation_id: delivery.conversation_id,
        title,
        body,
      },
      android: { priority: "HIGH" },
    },
  };
}
