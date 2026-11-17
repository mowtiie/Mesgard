# Connecting the app to a server

One Mesgard app works with any institution's Supabase server. The first time the app starts, it asks for the server
address and the access key. Nothing about a school is built into the app.

## What the administrator hands out

The setup script prints the address, the key and a ready made link at the end, so there is nothing to look up.

| Item | What it is |
|---|---|
| Server address | The HTTPS address of the Supabase API, for example `https://chat.school.example` |
| Access key | The public **anon** key of the project (`anon` role). Also called the publishable key |

Never hand out the **service role** key or a secret key (`sb_secret_...`). The app refuses both, because anyone who
holds such a key has full access to the database.

## Three ways to set it up

1. **Type it.** Open the app, enter the address and the key, and tap Connect.
2. **Open a setup link.** A link of the form below fills in the form. The person still checks the address and taps
   Connect, so a link alone never changes the server.

   ```
   mesgard://setup?url=<server address, URL encoded>&key=<access key>
   ```

   Example: `mesgard://setup?url=https%3A%2F%2Fchat.school.example&key=eyJ...`
3. **Scan a QR code of that link.** Make the QR code with any generator and print it or put it on the school intranet.
   The phone camera opens the link in Mesgard.

## What the app checks

- The address must start with `https`. Plain `http` is only accepted by debug builds, for the local development
  stack: `http://10.0.2.2:54321` in the emulator, or `http://<PC address>:54321` from a phone on the same Wi-Fi.
- It asks the server for its public Auth settings with the key. This tells apart an address that cannot be reached, a
  key the server rejects, and an address that is not a Supabase server.
- The settings are stored on the device. When the server changes, the app restarts to use it.

## Changing the server

On the Sign in screen, tap Change server. A different server cannot be set up while someone is signed in, because the
messages and the session on the device belong to the current server. Signing out removes them first.

## Developer builds

`supabase.url` and `supabase.anonKey` in `local.properties` (or the `SUPABASE_URL` and `SUPABASE_ANON_KEY`
environment variables) are optional. When they are set, the build starts with that server and skips the setup screen.
A server chosen in the app takes precedence. Leave them empty to try the first launch experience.
