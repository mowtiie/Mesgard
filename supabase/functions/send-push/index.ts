import { createFcmSender, type ServiceAccount } from "./fcm.ts";
import { handlePush } from "./handler.ts";
import type { Delivery } from "./payload.ts";

function requiredEnvironment(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing environment variable ${name}`);
  return value;
}

const supabaseUrl = requiredEnvironment("SUPABASE_URL");
const serviceRoleKey = requiredEnvironment("SUPABASE_SERVICE_ROLE_KEY");
const webhookSecret = requiredEnvironment("PUSH_WEBHOOK_SECRET");
const serviceAccount: ServiceAccount = JSON.parse(requiredEnvironment("FCM_SERVICE_ACCOUNT"));
const sendMessage = createFcmSender(serviceAccount);

const restHeaders = {
  apikey: serviceRoleKey,
  Authorization: `Bearer ${serviceRoleKey}`,
  "Content-Type": "application/json",
};

async function listDeliveries(messageId: string): Promise<Delivery[]> {
  const response = await fetch(`${supabaseUrl}/rest/v1/rpc/list_push_deliveries`, {
    method: "POST",
    headers: restHeaders,
    body: JSON.stringify({ target_message_id: messageId }),
  });
  if (!response.ok) throw new Error(`list_push_deliveries failed with status ${response.status}`);
  return await response.json();
}

async function deleteToken(fcmToken: string): Promise<void> {
  const response = await fetch(`${supabaseUrl}/rest/v1/device_tokens?fcm_token=eq.${encodeURIComponent(fcmToken)}`, {
    method: "DELETE",
    headers: restHeaders,
  });
  if (!response.ok) console.error(`Removing an invalid token failed with status ${response.status}`);
}

Deno.serve((request) =>
  handlePush(request, { webhookSecret, listDeliveries, sendMessage, deleteToken }).catch((error) => {
    console.error("send-push failed", error);
    return new Response(JSON.stringify({ error: "internal_error" }), { status: 500 });
  })
);
