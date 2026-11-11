# Push notifications

Push uses Firebase Cloud Messaging (FCM) as a delivery pipe only. The server decides what each notification says.

## Flow

1. After sign in, the app registers its FCM token with `register_device_token`.
2. Inserting a row in `messages` fires the trigger `messages_dispatch_push`, which posts the message id to the
   `send-push` Edge Function through `pg_net`. The request carries the header `x-webhook-secret`.
3. `send-push` calls `list_push_deliveries(message_id)` with the service role. The function returns one row per device
   of each recipient (the sender and muted participants are excluded). The sender name and message text are only
   returned when the recipient's notification preview mode allows them.
4. `send-push` builds a data message and sends it with the FCM HTTP v1 API. Tokens that FCM reports as `UNREGISTERED`
   or `INVALID_ARGUMENT` are deleted.
5. `MesgardMessagingService` shows the notification when the app is in the background. Tapping it opens the
   conversation.

The app shows content on the lock screen only through a generic public version ("Mesgard", "New message").

## Set up for a deployment

You need a Firebase project (Cloud Messaging only; no other Firebase product is used).

1. In the Firebase console, add an Android app with the application ID `com.mowtiie.mesgard` and download
   `google-services.json` into `app/`. The file is git-ignored. Without it the app builds and runs with push off.
2. Create a service account key for the project (Project settings, Service accounts). Never commit it.
3. Deploy the function and set its secrets:

   ```bash
   supabase functions deploy send-push --no-verify-jwt
   supabase secrets set PUSH_WEBHOOK_SECRET="<random string of at least 16 characters>"
   supabase secrets set FCM_SERVICE_ACCOUNT="$(cat service-account.json)"
   ```

4. Tell the database where the function lives (run once per deployment, as the database owner):

   ```sql
   insert into private.push_settings (function_url, webhook_secret)
   values ('https://<project-ref>.supabase.co/functions/v1/send-push', '<the same random string>');
   ```

   Without a row in `private.push_settings` the trigger does nothing, so messages keep working with push off.

## Local development

```bash
cp supabase/functions/.env.example supabase/functions/.env   # fill in FCM_SERVICE_ACCOUNT
supabase functions serve --env-file supabase/functions/.env
```

`supabase/seed.sql` inserts the local `private.push_settings` row. The function tests run without Deno installed:

```bash
docker run --rm -v "$PWD/supabase/functions:/functions" -w /functions/send-push denoland/deno:2.1.4 deno test
```

## Known limits

- FCM needs internet access through Google, so push does not work on a fully isolated campus network (see the
  air-gapped decision in the project open items).
- Realtime delivers messages while the app is open. Push covers the background case only.
