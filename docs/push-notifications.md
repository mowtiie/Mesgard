# Push notifications

Mesgard uses [UnifiedPush](https://unifiedpush.org) for push notifications. There is no Google or Firebase component.
Each device registers with a push distributor app that the user installs (for example ntfy), and the server sends
encrypted Web Push messages to the endpoint that the distributor issues. An institution can run its own push server, so
notifications can stay on its network.

## Flow

1. In Settings, Delivery, the user chooses a distributor app. The app registers with it.
2. The distributor issues an endpoint (a URL) and the connector library generates a key set for it. The app sends the
   endpoint and keys to the server with `register_push_subscription`, which stores them in `push_subscriptions`.
3. Inserting a row in `messages` fires the trigger `messages_dispatch_push`, which posts the message id to the
   `send-push` Edge Function through `pg_net`, with the header `x-webhook-secret`.
4. `send-push` calls `list_push_deliveries(message_id)` with the service role. It returns one row per subscription of
   each recipient (the sender and muted participants are excluded). The sender name and message text are only returned
   when the recipient's notification preview mode allows them, so the function never handles content it must not
   forward.
5. `send-push` encrypts a small JSON message (`conversation_id`, `title`, `body`) with the `aes128gcm` content coding of
   RFC 8291 and posts it to the endpoint (RFC 8030). A `404` or `410` answer removes the subscription.
6. The distributor delivers the message to `MesgardPushService`, where the connector library decrypts it. The app shows a
   notification when it is in the background; tapping it opens the conversation.

The lock screen only shows a generic public version ("Mesgard", "New message").

## Set up a deployment

1. Deploy the function and set its secret:

   ```bash
   supabase functions deploy send-push --no-verify-jwt
   supabase secrets set PUSH_WEBHOOK_SECRET="<random string of at least 16 characters>"
   ```

2. Tell the database where the function lives (run once per deployment, as the database owner):

   ```sql
   insert into private.push_settings (function_url, webhook_secret)
   values ('https://<project-ref>.supabase.co/functions/v1/send-push', '<the same random string>');
   ```

   Without a row in `private.push_settings` the trigger does nothing, so messages keep working with push off.

3. Decide which push servers subscriptions may use. A client chooses its own endpoint, so the function checks it before
   posting:

   - Self-hosted push server (recommended for institutions): set the allowed hosts. Plain HTTP is allowed for hosts on
     the list, which suits a push server on the campus network.

     ```bash
     supabase secrets set PUSH_ALLOWED_ENDPOINT_HOSTS="ntfy.school.edu,*.push.school.edu"
     ```

   - Public push server: leave the variable empty. Only public HTTPS hosts are accepted, and a host name that resolves
     to a private address is refused.

4. Users install a distributor app and choose it in Settings. For a campus deployment, run an
   [ntfy](https://ntfy.sh) server and point users at it.

## Local development

```bash
cp supabase/functions/.env.example supabase/functions/.env
supabase functions serve --env-file supabase/functions/.env
```

`supabase/seed.sql` inserts the local `private.push_settings` row. The function tests run without Deno installed:

```bash
docker run --rm -v "$PWD/supabase/functions:/functions" -w /functions/send-push denoland/deno:2.1.4 deno test
```

The encryption is checked against the worked example of RFC 8291.

### Trying it with ntfy on the emulator

1. Run a push server: `docker run -d -p 8085:80 binwiederhier/ntfy serve --base-url http://<host LAN IP>:8085`.
2. Set `PUSH_ALLOWED_ENDPOINT_HOSTS=<host LAN IP>` in `supabase/functions/.env` and restart `supabase functions serve`.
3. Install the F-Droid build of the ntfy app and set its Default server to `http://<host LAN IP>:8085`.
4. In Mesgard, open Settings, Delivery. Insert a message from another user while the app is in the background.

Use the LAN IP of the computer, not `10.0.2.2`: the emulator and the Edge Function container must both reach the push
server at the same address, because the endpoint host is the one stored in the subscription.

Verified this way: registration, delivery for all three preview modes (text withheld unless `sender_and_text`) and
opening the conversation by tapping the notification.

## Known limits

- The user has to install a distributor app and choose it. Without one, the app shows messages only while it is open.
- Messages are not authenticated with VAPID (RFC 8292). Most self-hosted push servers do not need it, but a push server
  that requires it will refuse registration. Add VAPID before relying on such a server.
- The endpoint of a subscription is a capability URL, so it is kept out of backups and removed on sign out.
- Not yet verified end to end with a real distributor app and push server.
