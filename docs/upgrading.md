# Upgrading

Update the server before the app. A new version of the app may rely on a database change or an Edge Function that an
older server does not have yet.

## Order

1. **Back up the database.** Use the project's backups in the dashboard, or `pg_dump` on a self-hosted server.
2. **Apply the new migrations.** From the repository of the version you are installing:

   ```bash
   supabase db push --remote
   ```

   Without `--remote` the command targets the local development stack. For a self-hosted server that is not linked to
   the Supabase CLI, use `supabase db push --db-url <connection string>` instead. Migrations are only ever added, never
   edited, so applying them in order is safe to repeat.
3. **Deploy the Edge Functions that changed.** `admin-users` is always needed; `send-push` only when you use push:

   ```bash
   supabase functions deploy admin-users
   supabase functions deploy send-push --no-verify-jwt
   ```
4. **Install the new app.** Migrations only add things, so phones that still have the old app normally keep working
   against the updated server.

## What goes wrong in the other order

| New app, old server | What people see |
|---|---|
| Without migration `20261116000100` | The banner "Unable to reach the server" stays on and new messages do not arrive live, because the private Realtime channel for message changes is refused. |
| Without migration `20261116000200` | An organization without an announcement conversation has no way to create one, because the app no longer has the button that did it. |

## Checking the result

After step 2, the output lists the applied migrations. Sign in with the new app, open a conversation from a second
account and send a message: it should arrive without a refresh and without the banner.
