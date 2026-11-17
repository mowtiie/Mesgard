# Two-step verification

Mesgard supports a second sign in step with a one-time code (TOTP) from any authenticator app. Administrators and staff are required to use it once you turn enforcement on. Students can turn it on for themselves.

The requirement is enforced by the database, not only by the app. A session that has not entered the code (an `aal1` session) cannot read or write any data, call any function or use attachments and Realtime channels, even if someone calls the API directly with a modified app.

## How it works

| Person | Without a factor | With a confirmed factor |
|---|---|---|
| Administrator or staff, enforcement off | Works as before | Must enter a code at every sign in |
| Administrator or staff, enforcement on | Sets up a factor at the next sign in, before anything else | Must enter a code at every sign in |
| Student | Works as before, can turn it on in Account | Must enter a code at every sign in |

- **Sign in order:** password, then the code (or the setup screen when a factor is required but missing), then the forced password change for a new account, then the app.
- **Setup:** the screen shows a setup key in groups of four and an **Open in authenticator app** button. Screenshots are blocked on that screen whatever the screen privacy setting says. There is no QR code.
- **Recovery:** there are no backup codes. An administrator resets a lost phone from **Settings, Administration, Members, Reset two-step verification**. The person is signed out and sets up again at the next sign in. An administrator cannot reset their own factor.
- **Audit:** a reset is recorded in the audit log.

## On a new server

`setup` asks whether to require two-step verification for administrators and staff, and the default is yes. With yes, the first administrator is asked to set up an authenticator app at the first sign in, before anything else, so there is nothing to prepare. Use `--no-require-mfa` to leave it off, and the statement below to turn it on later.

## Turning it on for an existing server

The order matters. Enforcement is **off** after the migration, so nobody is locked out by the update.

1. Make sure Auth accepts TOTP.
   - **Supabase Cloud:** in the dashboard, open Authentication, then the multi-factor settings, and enable TOTP enrollment and verification.
   - **Self-hosted:** enable TOTP enrollment and verification in the Auth service. These are the `GOTRUE_MFA_TOTP_ENROLL_ENABLED` and `GOTRUE_MFA_TOTP_VERIFY_ENABLED` settings (taken from the Auth server documentation, not tested in this repository).
   - **Local development:** `supabase/config.toml` enables both. The Auth container only reads the file when it starts, so run `supabase stop` and `supabase start` after changing it.
2. Install the new version of the app and update the server, in that order (see [`upgrading.md`](upgrading.md)). Apply the migrations with `supabase db push` and deploy the function with `supabase functions deploy admin-users`.
3. Sign in as an administrator and set up two-step verification from **Settings, Account, Two-step verification**. Check that you can sign out and back in with a code.
4. Tell staff and administrators to install an authenticator app.
5. Turn enforcement on with one statement in the SQL Editor:

   ```sql
   update private.mfa_settings set enforced = true;
   ```

People who are already signed in are asked at their next app start or when their session refreshes. Anyone who signs in afterwards is asked right away.

## When someone is locked out

- **A person lost their phone:** an administrator resets them from Members. The person sets up again at the next sign in.
- **The only administrator lost their phone:** nobody can reset them in the app. Remove the factor in the SQL Editor:

  ```sql
  delete from auth.mfa_factors
  where user_id = (select id from auth.users where email = 'admin@school.example');
  ```

- **Enforcement locks people out and you need time:** turn it off again. This releases administrators and staff who have no factor. Anyone with a confirmed factor still has to enter the code.

  ```sql
  update private.mfa_settings set enforced = false;
  ```

## Things to know

- **Device time:** codes depend on the clock. A wrong code is the most common problem, and the app reminds the person to set the time automatically.
- **Attempts:** Auth rate limits code attempts. The app shows "Too many attempts" when it answers with a rate limit. This message has not been checked against a real server.
- **Offline:** the code and setup screens need a connection. If the app cannot ask the server whether two-step verification is required, it lets the person in, but the server still refuses their data until the code is verified.
- **New functions:** every database function that runs with elevated rights must call `private.require_mfa()` first, and every table needs the restrictive policy "Require two-step verification". The database tests fail when a new table or function forgets it.
- **Not included:** passkeys, SMS codes, backup codes, per-organization requirements and a QR code.
