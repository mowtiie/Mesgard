import type { Delivery } from "./payload.ts";

export type SendOutcome = "sent" | "gone" | "failed";

export interface HandlerDependencies {
  webhookSecret: string;
  listDeliveries(messageId: string): Promise<Delivery[]>;
  isEndpointAllowed(endpoint: string): Promise<boolean>;
  sendMessage(delivery: Delivery): Promise<SendOutcome>;
  deleteSubscription(endpoint: string): Promise<void>;
}

export const WEBHOOK_SECRET_HEADER = "x-webhook-secret";

function constantTimeEquals(left: string, right: string): boolean {
  const encoder = new TextEncoder();
  const a = encoder.encode(left);
  const b = encoder.encode(right);
  let difference = a.length ^ b.length;
  for (let index = 0; index < Math.max(a.length, b.length); index++) {
    difference |= (a[index] ?? 0) ^ (b[index] ?? 0);
  }
  return difference === 0;
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

async function deliver(delivery: Delivery, dependencies: HandlerDependencies): Promise<SendOutcome> {
  if (!await dependencies.isEndpointAllowed(delivery.endpoint)) {
    console.error("A push subscription points at an endpoint that is not allowed");
    return "failed";
  }
  const outcome = await dependencies.sendMessage(delivery);
  if (outcome === "gone") await dependencies.deleteSubscription(delivery.endpoint);
  return outcome;
}

export async function handlePush(request: Request, dependencies: HandlerDependencies): Promise<Response> {
  if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const secret = request.headers.get(WEBHOOK_SECRET_HEADER) ?? "";
  if (dependencies.webhookSecret.length === 0 || !constantTimeEquals(secret, dependencies.webhookSecret)) {
    return json({ error: "unauthorized" }, 401);
  }

  const payload = await request.json().catch(() => null);
  const messageId = payload?.message_id;
  if (typeof messageId !== "string" || messageId.length === 0) return json({ error: "invalid_request" }, 400);

  const deliveries = await dependencies.listDeliveries(messageId);
  const outcomes = await Promise.all(deliveries.map((delivery) => deliver(delivery, dependencies)));

  return json(
    {
      sent: outcomes.filter((outcome) => outcome === "sent").length,
      removed: outcomes.filter((outcome) => outcome === "gone").length,
      failed: outcomes.filter((outcome) => outcome === "failed").length,
    },
    200,
  );
}
