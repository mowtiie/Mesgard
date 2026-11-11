import { createEndpointGuard, parseAllowedHosts } from "./endpoint.ts";
import { handlePush } from "./handler.ts";
import type { Delivery } from "./payload.ts";
import { createWebPushSender } from "./webpush.ts";

function requiredEnvironment(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing environment variable ${name}`);
  return value;
}

const supabaseUrl = requiredEnvironment("SUPABASE_URL");
const serviceRoleKey = requiredEnvironment("SUPABASE_SERVICE_ROLE_KEY");
const webhookSecret = requiredEnvironment("PUSH_WEBHOOK_SECRET");
const allowedHosts = parseAllowedHosts(Deno.env.get("PUSH_ALLOWED_ENDPOINT_HOSTS"));

const isEndpointAllowed = createEndpointGuard(allowedHosts, async (hostname) => [
  ...await Deno.resolveDns(hostname, "A").catch(() => []),
  ...await Deno.resolveDns(hostname, "AAAA").catch(() => []),
]);
const sendMessage = createWebPushSender();

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

async function deleteSubscription(endpoint: string): Promise<void> {
  const response = await fetch(
    `${supabaseUrl}/rest/v1/push_subscriptions?endpoint=eq.${encodeURIComponent(endpoint)}`,
    { method: "DELETE", headers: restHeaders },
  );
  if (!response.ok) console.error(`Removing a push subscription failed with status ${response.status}`);
}

Deno.serve((request) =>
  handlePush(request, { webhookSecret, listDeliveries, isEndpointAllowed, sendMessage, deleteSubscription }).catch(
    (error) => {
      console.error("send-push failed", error);
      return new Response(JSON.stringify({ error: "internal_error" }), { status: 500 });
    },
  )
);
