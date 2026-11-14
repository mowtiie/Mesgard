import { handleAdminRequest } from "./handler.ts";
import { createSupabaseApi } from "./supabase_api.ts";

function requiredEnvironment(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing environment variable ${name}`);
  return value;
}

const api = createSupabaseApi({
  url: requiredEnvironment("SUPABASE_URL"),
  anonKey: requiredEnvironment("SUPABASE_ANON_KEY"),
  serviceRoleKey: requiredEnvironment("SUPABASE_SERVICE_ROLE_KEY"),
});

Deno.serve((request) =>
  handleAdminRequest(request, { api }).catch((error) => {
    console.error("admin-users failed", error instanceof Error ? error.message : "unknown error");
    return new Response(JSON.stringify({ error: "internal_error" }), { status: 500 });
  })
);
