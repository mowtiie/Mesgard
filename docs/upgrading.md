# Upgrading

Update the server first, then the app. A new version of the app may rely on a database change or a function that an
older server does not have yet.

## The quick way

1. Back up the database: use the project's backups in the dashboard, or `pg_dump` on a self-hosted server.
2. From the repository of the version you are installing, run the setup again. It only applies what is new:

   ```
   git pull
   node scripts/server/index.mjs setup --project-ref <project ref>
   ```

3. Install the new app. Migrations only add things, so phones that still have the old app normally keep working
   against the updated server.

`node scripts/server/index.mjs doctor` checks the result, including "Server is new enough for the app". The app
also tells you: if the server is older than the app, the Sign in screen, About and the Administration page show "This
server needs an update". Nothing is shown when the server cannot be reached, so a bad connection never raises a false
alarm.

## By hand

```bash
supabase db push --remote
supabase functions deploy admin-users
supabase functions deploy send-push --no-verify-jwt   # only when you use push
```

Without `--remote` the command targets the local development stack. For a self-hosted server that is not linked to
the Supabase CLI, use `supabase db push --db-url <connection string>`. Migrations are only ever added, never edited,
so applying them in order is safe to repeat.

## What goes wrong in the other order

| New app, old server | What people see |
|---|---|
| Without migration `20261116000100` | The banner "Unable to reach the server" stays on and new messages do not arrive live, because the private Realtime channel for message changes is refused. |
| Without migration `20261116000200` | An organization without an announcement conversation has no way to create one, because the app no longer has the button that did it. |
| Without migration `20261117000300` | The app shows "This server needs an update". |
| Without migrations `20261117000500` and `20261117000600` | The Remove dialog does not offer to erase personal data, and a returning person cannot be added again. Without `20261117000600`, the stricter erasure mode cannot blank messages. |
| Without migration `20261117000400` | After someone is removed from an organization, a direct conversation with them still accepts messages that nobody receives, and their old messages lose the sender name on phones that have not stored it. |

## For developers

When the app starts to use something an older server lacks, raise `api_level` in `server_info()` with a new migration
and raise `REQUIRED_API_LEVEL` in `ServerCompatibility.kt` in the same change.
