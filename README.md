<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Mesgard icon" width="160" height="160">

# Mesgard

**Your campus. Your chat. Your server.**

Mesgard is an open-source, real-time messaging app for schools, universities and other institutions. It connects to your own Supabase backend instead of a company's servers. Whether you use a Supabase Cloud project or a self-hosted instance, your messages live where you decide.

![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)

</div>

## Contents

- [What you get](#what-you-get)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Connecting phones](#connecting-phones)
- [Adding people](#adding-people)
- [Two-step verification](#two-step-verification)
- [Push notifications](#push-notifications)
- [Removing and erasing people](#removing-and-erasing-people)
- [Running the server](#running-the-server)
- [Troubleshooting](#troubleshooting)
- [Verify your download](#verify-your-download)
- [For developers](#for-developers)
- [Security](#security), [License](#license) and [Credits](#credits)

## What you get

* **Bring your own backend:** Connect to any Supabase project, Cloud or self-hosted, with your project URL and public (anon) key. Administrators can share a setup link or a QR code.
* **Private per organization:** People only see their own organization's data, enforced by Row Level Security in your database.
* **Direct messages and group chats:** Real-time messaging with typing indicators, online status, read marking and unread counts.
* **Announcements:** Every organization has an announcement conversation. Administrators and staff post to everyone in it. Students can read but not reply.
* **Photos and files:** Photos and documents up to 20 MB each, 10 per message.
* **Works offline:** Conversations are saved on the device (encrypted). You can write without a connection, and messages send when you are back online.
* **Notifications without Google:** Push uses [UnifiedPush](https://unifiedpush.org) with a distributor app such as ntfy. There is no Firebase and no Google services, and an institution can run its own push server. Choose how much a notification shows: sender and text, sender only, or nothing.
* **Administration:** Add members, import a roster from a CSV file, reset passwords, change roles, manage groups and read an audit log of every change. New accounts get a temporary password and must choose their own at first sign in.
* **Two-step verification and erasing people:** Administrators and staff can be required to enter a one-time code. Administrators can remove a member, or remove and erase their personal data.
* **Themes and privacy:** Light and dark themes, three contrast levels, dynamic colors, an optional app lock and an optional screen privacy mode that blocks screenshots.

**Not end-to-end encrypted.** Messages are protected in transit (TLS) and on the device, and your server can read them. This is by design, so that your institution can search, audit and keep its own records.

## Requirements

| For | You need |
|---|---|
| **Every phone** | Android 8.0 (API 26) or later. |
| **The server** | A [Supabase](https://supabase.com) project, Cloud or self-hosted, reachable over **HTTPS**. A free Supabase account is enough to start (check Supabase's current limits). |
| **The person setting it up** | A computer with [Node.js](https://nodejs.org) 18 or later, [Git](https://git-scm.com) and the [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started). Docker is not needed. Or nothing at all, using the [GitHub Action](#from-github-without-installing-anything). |
| **Two-step verification** (optional) | An authenticator app on each administrator's and staff member's phone. |
| **Push notifications** (optional) | A push app on each phone, such as [ntfy](https://ntfy.sh). Institutions can run their own push server. |

To build the app yourself, see [For developers](#for-developers).

## Quick start

Four steps. Only the anon (publishable) key ever goes into the app. **Never paste a secret or `service_role` key into it**: it bypasses Row Level Security, and the app refuses both.

### 1. Create a Supabase project

1. Sign up at [supabase.com](https://supabase.com) and choose **New project**.
2. Pick the region closest to your users, and set a **database password**. Save it in a password manager.
3. Set the API options like this:
   - **Enable Data API**: on. The app cannot work without it.
   - **Automatically expose new tables**: off. Mesgard's migrations grant access to their own tables.
   - **Enable automatic RLS**: on. Mesgard's tables already have Row Level Security, so this only protects tables you add yourself.
4. Wait until the project is ready, then copy its **project ref**: the 20 letters in the address, `supabase.com/dashboard/project/<project ref>`.

### 2. Install the tools

Install the Supabase CLI, then download Mesgard and sign in to Supabase:

```
git clone https://github.com/mowtiie/Mesgard.git
cd Mesgard
supabase login
```

### 3. Run the setup

```
node scripts/server/index.mjs setup --project-ref <project ref>
```

The CLI may ask for your database password. The setup then asks for:

- the name of your school or organization,
- a short name for it (press Enter to accept the suggestion),
- the email and name of the first administrator,
- whether to **require two-step verification** for administrators and staff (press Enter for yes, the recommended answer).

When it finishes, it prints the **server address** and **public key** (and a **link** that contains both), and the administrator's **temporary password**, shown once. Keep them for the next step. Every line of the final checklist should say `[ok]`. If one says `[FAIL]`, the line under it tells you what to do, and you can run the same command again.

### 4. Connect the phone

1. Install Mesgard on the phone.
2. Open the link from step 3 on the phone, or open Mesgard and type the server address and the public key.
3. Sign in with the administrator's email and the temporary password. If you answered yes to two-step verification, Mesgard first asks you to set up an authenticator app (enter the key it shows, then the 6-digit code). Then it asks you to choose your own password.

You are done. From **Settings, Administration**, add the other people in your school.

<details>
<summary><strong>What the setup does, and its options</strong></summary>

| Step | What happens |
|---|---|
| Link | Links the folder to the project, unless it already is. |
| Migrations | Applies the migrations in `supabase/migrations` that the project does not have yet. |
| Function | Deploys `admin-users`, the function that creates accounts, without Docker. |
| Auth settings | Shows the differences, then asks `[Y/n]` before turning public sign-ups off and enabling TOTP. Nothing else in your Auth settings is touched. |
| First organization | Asks the questions above. Creates the login with a temporary password and adds the person as administrator. It is skipped when an organization already exists, and it never changes the two-step setting of a server that already has an organization. |
| Connect | Prints the server address, the public key and a `mesgard://setup` link. |
| Check | Runs the checklist from [Running the server](#checking-a-server). |

The script reads the secret key of the project only to create that login, keeps it in memory, and never prints it or stores it.

You can give the answers as options so nothing is asked: `--org-name`, `--org-slug`, `--admin-email`, `--admin-name`, and `--require-mfa` or `--no-require-mfa`. Add `--yes` to apply the Auth settings and accept the suggested answers (including yes to two-step verification) without a question, `--add-organization` to create another organization on a server that has one, and `--skip-organization` to only deploy. `--with-push` also sets up [push notifications](#push-notifications).

</details>

### From GitHub, without installing anything

If you would rather not install the CLI or Node, a workflow in this repository does the same setup from the Actions tab. It has not been run on GitHub yet. The script it runs is tested, and the workflow file passes `actionlint`.

1. Copy the repository to your own GitHub account (a fork).
2. In the Supabase dashboard, create an access token under your account settings, then in your copy open **Settings, Secrets and variables, Actions** and add these secrets:

   | Secret | Value |
   |---|---|
   | `SUPABASE_ACCESS_TOKEN` | The access token |
   | `SUPABASE_DB_PASSWORD` | The database password you chose for the project |
   | `MESGARD_ADMIN_PASSWORD` | A password of 12 or more characters for the first administrator. Only needed when you create the first organization. |

3. Open the **Actions** tab, choose **Deploy backend**, and select **Run workflow**. Enter the project ref. To create the first organization, also enter its name, the administrator's email and name, and optionally a short name. Leave those empty to only deploy. Tick **require_mfa** to require two-step verification (off by default here, and only applied when you create the organization). Tick **enable_push** to set up push notifications as well.
4. When the run finishes, its log ends with the connect link and the checklist.

The administrator signs in with `MESGARD_ADMIN_PASSWORD` and is asked to choose their own password right away. That password is never printed. The log only contains public values (the project address and the public key), but anyone who can read your copy's Actions logs can see them, so keep the copy private if you can.

## Connecting phones

One Mesgard app works with any institution's Supabase server. The first time the app starts, it asks for the server address and the access key. Nothing about a school is built into the app.

| Item | What it is |
|---|---|
| Server address | The HTTPS address of the Supabase API, for example `https://chat.school.example` |
| Access key | The public **anon** (publishable) key of the project |

Three ways to set it up:

1. **Type it.** Open the app, enter the address and the key, and tap Connect.
2. **Open a setup link.** The setup prints one. It fills in the form, and the person still checks the address and taps Connect, so a link alone never changes the server.

   ```
   mesgard://setup?url=<server address, URL encoded>&key=<access key>
   ```

3. **Scan a QR code of that link.** Make the code with any generator and print it or put it on the school intranet. The phone camera opens the link in Mesgard.

The app only accepts `https` addresses. It asks the server for its public Auth settings with the key, which tells apart an address that cannot be reached, a key the server rejects, and an address that is not a Supabase server. The result is stored on the device.

To change the server, tap **Change server** on the Sign in screen. It is refused while someone is signed in, because the messages and the session on the device belong to the current server. Signing out removes them first.

## Adding people

The administrator opens **Settings, Administration, Members**:

- **Add member** creates a login with a temporary password, shown once, and the person chooses their own at the first sign in.
- **Import from CSV** adds many people at once. The file needs an email column (name and role are optional, a missing role means student), at most 2000 rows and 1 MB. Commas, semicolons and tabs work.
- **Change role**, **Reset password** and **Remove** are on each person. The last administrator cannot be removed.

Groups (classes, sections, departments) are managed under **Administration, Groups**. The **Audit log** lists every change.

## Two-step verification

Mesgard supports a second sign in step with a one-time code (TOTP) from any authenticator app. Administrators and staff are required to use it once enforcement is on. Students can turn it on for themselves under **Settings, Account**.

The requirement is enforced by the database, not only by the app. A session that has not entered the code cannot read or write any data, call any function or use attachments and Realtime channels, even if someone calls the API directly with a modified app.

| Person | Without a factor | With a confirmed factor |
|---|---|---|
| Administrator or staff, enforcement off | Works as before | Must enter a code at every sign in |
| Administrator or staff, enforcement on | Sets up a factor at the next sign in, before anything else | Must enter a code at every sign in |
| Student | Works as before, can turn it on in Account | Must enter a code at every sign in |

- **Sign in order:** password, then the code (or the setup screen when a factor is required but missing), then the forced password change for a new account, then the app.
- **Setup screen:** it shows a setup key in groups of four and an **Open in authenticator app** button. Screenshots are blocked there whatever the screen privacy setting says. There is no QR code.
- **Recovery:** there are no backup codes. An administrator resets a lost phone from **Settings, Administration, Members, Reset two-step verification**. The person is signed out and sets up again at the next sign in. An administrator cannot reset their own factor. Resets are recorded in the audit log.

**On a new server,** `setup` asks whether to require it, and the default is yes. With yes, the first administrator is asked to set up an authenticator app at the first sign in, so there is nothing to prepare.

**On an existing server,** enforcement is off after the update, so nobody is locked out. To turn it on:

1. Make sure Auth accepts TOTP. On Supabase Cloud, `setup` does this for you, or use the dashboard under Authentication, multi-factor. Self-hosted: enable TOTP enrollment and verification in the Auth service (`GOTRUE_MFA_TOTP_ENROLL_ENABLED` and `GOTRUE_MFA_TOTP_VERIFY_ENABLED`, taken from the Auth server documentation, not tested here).
2. Update the server and the app, in that order (see [Running the server](#updating)).
3. Sign in as an administrator and set up two-step verification from **Settings, Account, Two-step verification**. Check that you can sign out and back in with a code.
4. Tell staff and administrators to install an authenticator app.
5. Run this in the SQL Editor:

   ```sql
   update private.mfa_settings set enforced = true;
   ```

People who are already signed in are asked at their next app start or when their session refreshes. Anyone who signs in afterwards is asked right away.

**When someone is locked out:**

- **A person lost their phone:** an administrator resets them from Members.
- **The only administrator lost their phone:** nobody can reset them in the app. Remove the factor in the SQL Editor:

  ```sql
  delete from auth.mfa_factors
  where user_id = (select id from auth.users where email = 'admin@school.example');
  ```

- **Enforcement locks people out and you need time:** turn it off again. This releases administrators and staff who have no factor. Anyone with a confirmed factor still has to enter the code.

  ```sql
  update private.mfa_settings set enforced = false;
  ```

**Things to know:** codes depend on the device clock, and a wrong code is the most common problem. Auth rate limits attempts (the "Too many attempts" message has not been checked against a real server). The code screens need a connection: if the app cannot ask the server, it lets the person in, but the server still refuses their data until the code is verified. Passkeys, SMS codes, backup codes, per-organization requirements and a QR code are not included.

## Push notifications

Mesgard uses [UnifiedPush](https://unifiedpush.org). There is no Google or Firebase component. Each phone registers with a push app that the person installs (for example ntfy), and the server sends encrypted messages to it. An institution can run its own push server, so notifications can stay on its network, even when the campus has no internet access.

**Set it up** with one command. It deploys the push function, creates a secret, tells the database and checks that they agree:

```
node scripts/server/index.mjs push --project-ref <project ref>
```

- Add `--push-hosts ntfy.school.edu` to accept only your own push server. Without it, any public push server is accepted.
- Add `--rotate` to replace the secret later, or `--with-push` on the `setup` command to do it all at once.
- `doctor` makes the same check afterwards, so a mismatch is reported instead of push silently doing nothing.

**Then, on each phone:** install a push app such as ntfy, and choose it in Mesgard under **Settings, Notifications**. Without one, messages arrive only while the app is open.

**What a notification shows** is set per person (and the organization sets the default): sender and text, sender only (the default), or nothing. The server decides the content, so hidden text is never sent to the push server. The lock screen only shows a generic "New message".

<details>
<summary><strong>How it works, setting it up by hand, and known limits</strong></summary>

1. The app registers with the push app, which issues an endpoint (a URL). The app sends the endpoint and a key set to the server with `register_push_subscription`.
2. Inserting a message fires a trigger that posts the message id to the `send-push` Edge Function through `pg_net`, with the header `x-webhook-secret`. Without a row in `private.push_settings` the trigger does nothing, so messages keep working with push off.
3. `send-push` asks `list_push_deliveries` (service role) for the recipients, excluding the sender and muted participants, with text withheld according to each person's preview mode.
4. It encrypts a small JSON message (RFC 8291, `aes128gcm`) and posts it to the endpoint (RFC 8030). A `404` or `410` answer removes the subscription. The endpoint is checked first: it must be on the allowed host list, or a public HTTPS host that does not resolve to a private address.

By hand:

```bash
supabase functions deploy send-push --no-verify-jwt
supabase secrets set PUSH_WEBHOOK_SECRET="<random string of at least 16 characters>"
supabase secrets set PUSH_ALLOWED_ENDPOINT_HOSTS="ntfy.school.edu,*.push.school.edu"   # optional
```

```sql
insert into private.push_settings (function_url, webhook_secret)
values ('https://<project-ref>.supabase.co/functions/v1/send-push', '<the same random string>');
```

Known limits: messages are not authenticated with VAPID (RFC 8292), so a push server that requires it refuses registration. The endpoint of a subscription is a capability URL, so it is kept out of backups and removed on sign out. Not yet verified end to end with a real push app on a real phone.

</details>

## Removing and erasing people

There are two ways to take someone out of an organization, and each server decides how far the second one goes.

| | Remove | Remove and erase personal data |
|---|---|---|
| For | Someone who left, and may come back | Someone whose personal data must go |
| Their messages | Stay, with their name | Stay as "Former member", or are erased (see below) |
| Their name and email | Kept | Replaced with placeholders |
| Their login | Disabled | Disabled for good |
| Can they return? | Yes, add them again | No. The same email becomes a new person. |
| Undo | Add them again | **None** |

Both are in **Settings, Administration, Members**, under **Remove**. The erase option is a checkbox in the same dialog, and it only appears when the server allows it. Only someone who belongs to **no other organization** can be erased, and administrators cannot erase themselves.

**Choose what erasing means for your server** with one statement in the SQL Editor:

```sql
update private.erasure_settings set mode = 'keep_messages';
```

| Mode | What happens | Use it when |
|---|---|---|
| `keep_messages` (default) | Personal data is erased. Messages stay and show "Former member". | Conversations and records must stay readable. |
| `blank_messages` | The same, and the person's messages are erased too: they show as deleted, the original text is purged, and attachments are deleted. | A school must remove what people wrote. |
| `off` | The checkbox does not appear. Nobody can be erased. | The school must keep everything. |

The mode applies when someone is erased, and does not change people who were already erased.

**Erasing removes** the profile name, picture and status; the email (replaced by a placeholder that cannot receive mail), phone number and sign in details; sessions, authenticator apps, push subscriptions and settings; the rows in Auth's sign in log that mention their email, name or device address; and with `blank_messages`, their message text (also the copy administrators can read) and their attachments. The audit log keeps a record that an administrator erased someone, without their name.

**Bringing someone back:** add them again under **Members, Add member** with the same email. A removed person gets their login back with a new temporary password and must choose their own. An email that belongs to someone active, or to another organization, is refused. An erased person cannot return.

**What erasing cannot do:**

- **Undo.** Take a backup first if you are unsure.
- **Reach backups.** Hosted backups and any copy you made keep the data until they expire.
- **Reach the request logs of Auth.** Supabase keeps logs outside the database, with their own retention.
- **Remove what others wrote.** If someone typed another person's name in a message, it stays.
- **Replace advice.** Privacy laws, such as the Philippine Data Privacy Act of 2012 and GDPR, can ask a school to erase data and also to keep records. Ask the school's data protection officer which mode fits, and set it before the first erasure.

**If something fails:** erasing is made of steps, and each step can be repeated. If one fails (for example the file storage is unreachable), the app shows an error. The person has already been removed by then, so they no longer appear in Members, and **the app has no screen yet to finish the erase**. Whoever runs the server has to repeat the request: the `admin-users` function with the action `erase_member` and the person's id, sent by an administrator of the organization. It changes nothing for someone already erased. The function's logs say which step failed.

## Running the server

### Checking a server

```
node scripts/server/index.mjs doctor
```

It only reads. It reports whether the server is reachable over HTTPS, public sign-ups are off, the migrations are all applied, the server is new enough for the app, `admin-users` is deployed, the Auth settings match, an organization and an administrator exist, and whether two-step verification and push notifications are set up. Each line that needs attention says what to do. Add `--project-ref <ref>` for a project other than the linked one, or `--local` for the local stack.

### Updating

Update the server first, then the app. A new version of the app may rely on a database change or a function that an older server does not have yet.

1. Back up the database: use the project's backups in the dashboard, or `pg_dump` on a self-hosted server.
2. From the repository of the version you are installing, run the setup again. It only applies what is new, leaves matching settings alone and does not create another organization:

   ```
   git pull
   node scripts/server/index.mjs setup --project-ref <project ref>
   ```

3. Install the new app. Migrations only add things, so phones that still have the old app normally keep working against the updated server.

The app also tells you: if the server is older than the app, the Sign in screen, About and the Administration page show "This server needs an update". Nothing is shown when the server cannot be reached, so a bad connection never raises a false alarm.

<details>
<summary><strong>By hand, and what goes wrong in the other order</strong></summary>

```bash
supabase db push --remote
supabase functions deploy admin-users
supabase functions deploy send-push --no-verify-jwt   # only when you use push
```

Without `--remote` the command targets the local development stack. For a self-hosted server that is not linked to the Supabase CLI, use `supabase db push --db-url <connection string>`. Migrations are only ever added, never edited, so applying them in order is safe to repeat.

| New app, old server | What people see |
|---|---|
| Without migration `20261116000100` | The banner "Unable to reach the server" stays on and new messages do not arrive live. |
| Without migration `20261116000200` | An organization without an announcement conversation has no way to create one. |
| Without migration `20261117000300` | The app shows "This server needs an update". |
| Without migrations `20261117000500` and `20261117000600` | The Remove dialog does not offer to erase personal data, and a returning person cannot be added again. Without `20261117000600`, the stricter erasure mode cannot blank messages. |
| Without migration `20261117000400` | After someone is removed, a direct conversation with them still accepts messages that nobody receives, and their old messages lose the sender name on phones that have not stored it. |

</details>

## Troubleshooting

| Problem | What to do |
|---|---|
| "The Supabase CLI was not found" | Install it and open a new terminal, so that `supabase` runs there. |
| "No project selected" | Pass `--project-ref`, or run `supabase link --project-ref <ref>` first. |
| A setup step failed | Fix the cause shown above it and run `setup` again. Every step can be repeated. |
| Setup stopped after creating the login | Run it again with the same email. It reuses the login and finishes the organization, but the original temporary password stays unknown, so set a new one in the dashboard under Authentication, Users. The person is still asked to choose their own at the first sign in. |
| The administrator forgot the temporary password | Set a new one in the dashboard under Authentication, Users. |
| Nobody can sign in after turning on two-step verification | See [When someone is locked out](#two-step-verification). |
| Push does nothing | Run `doctor`. It tests the push function, and reports a missing function or a secret that differs. Then check each phone has a push app chosen in Settings, Notifications. |
| "This server needs an update" in the app | Run `setup` again from the version of the repository you are installing. |
| The banner "Unable to reach the server" | Check the phone's connection and the server address. A server that lacks migration `20261116000100` also shows it (see [Updating](#updating)). |
| An erase failed half way | See "If something fails" under [Removing and erasing people](#removing-and-erasing-people). |

## Verify your download

APK releases on GitHub are signed using my key. They can
be verified using
[apksigner](https://developer.android.com/studio/command-line/apksigner.html#options-verify):

```
apksigner verify --print-certs --verbose mesgard-vX.X.apk
```

The output should look like:

```
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
Verified using v3.1 scheme (APK Signature Scheme v3.1): false
Verified using v3.2 scheme (APK Signature Scheme v3.2): false
Verified using v4 scheme (APK Signature Scheme v4): false
```

The certificate fingerprints should correspond to the ones listed below:

```
Owner: CN=Mowtiie, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown
Issuer: CN=Mowtiie, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown
Serial number: 8a256fdcdde50069
Valid from: Wed Jun 10 22:57:23 PST 2026 until: Sun Oct 26 22:57:23 PST 2053
Certificate fingerprints:
         SHA1: 56:4E:2C:DB:E4:06:C9:EC:15:E6:BC:D9:0A:88:38:72:8B:FB:13:20
         SHA256: 8B:67:51:F3:C3:31:85:63:5F:98:95:30:B6:C0:73:A1:39:7B:3D:41:2B:EF:AE:69:06:A2:EB:58:45:D2:DE:63
```

**Warning:** Only install Mesgard APKs signed with the key above. Verifying the signature confirms you're running a genuine, unmodified build.

### PGP Signing

As an additional layer on top of the Android signature above, each release is also signed with my PGP key. While `apksigner` confirms the APK itself is intact, a PGP signature confirms that *I* am the one who published this specific file to GitHub, an independent check that doesn't rely on GitHub's account security alone.

**Public key fingerprint:**
```
9EA2 8F46 7802 5092 7643 1B69 42B5 FA42 AA63 90E1
```

Download and import the key from my website:

```
curl -O https://mowtiie.cc/PGP_PUBLIC_KEY.asc
gpg --import PGP_PUBLIC_KEY.asc
```

After importing, confirm the fingerprint printed by GPG matches the one listed above. That match is what actually establishes trust, not the import step itself. Fetching the key over HTTPS from a domain you already trust is arguably a stronger anchor than a keyserver, since keyservers accept uploads from anyone and don't vouch for identity.

Each release includes a detached `.asc` signature alongside the APK. Verify a downloaded release with:

```
gpg --verify mesgard-vX.X.apk.asc mesgard-vX.X.apk
```

A valid signature looks like:

```
Good signature from "Mowtiie <mowtiie.dev@gmail.com>"
```

**Note:** PGP signing is a supplementary trust measure, not a substitute for the `apksigner` check above. Verify both if you want the highest confidence that a release is genuine and unmodified.

### Mapping files

Each release on GitHub includes a `mapping-<version>.txt` file alongside the APK. This file is needed to deobfuscate stack traces from crash reports: match the file to the version shown in the crash report header and use it with `retrace` from the Android SDK.

## For developers

**Requirements:** Android Studio (current stable, with its bundled JDK), [Docker](https://www.docker.com) for the local Supabase stack and the database and function tests, Node.js 18 or later, and the Supabase CLI.

**Build and test the app:**

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew ktlintCheck detekt lintDebug
```

**The local backend:**

```
supabase start          # local stack
supabase db reset       # re-apply migrations and seed (local only)
supabase test db        # pgTAP tests for RLS, constraints and functions
node --test scripts/server/   # tests of the server tools
```

Local sign-in accounts come from `supabase/seed.sql` (for example `admin@rnhs.test`, password `password123`). Never seed or reset a real project.

**Point a build at a server.** `supabase.url` and `supabase.anonKey` in `local.properties` (or the `SUPABASE_URL` and `SUPABASE_ANON_KEY` environment variables) are optional. When set, the build starts with that server and skips the setup screen. A server chosen in the app takes precedence. Leave them empty to try the first launch. Debug builds accept plain `http` for the local stack (`http://10.0.2.2:54321` in the emulator, `http://<PC address>:54321` from a phone on the same Wi-Fi). Release builds accept `https` only.

**Releases.** `./gradlew clean assembleRelease lintVitalRelease` signs with a git-ignored `keystore.properties` (or `SIGNING_*` environment variables) and writes `app/build/outputs/apk/release/app-release.apk` and the R8 mapping file. Release files go to GitHub as `mesgard-vX.X.apk`, `mapping-X.X.txt`, a `.sha256` and the detached PGP `.asc`.

**When the app needs something new from the server,** raise `api_level` in `server_info()` with a new migration, and raise `REQUIRED_API_LEVEL` in `ServerCompatibility.kt` in the same change. Migrations are only ever added, never edited.

**Every new table** needs Row Level Security and the restrictive "Require two-step verification" policy, and every `security definer` function callable by signed in users must start with `perform private.require_mfa()`. The database tests fail when one forgets.

<details>
<summary><strong>Push on the local stack, with ntfy on the emulator</strong></summary>

```bash
cp supabase/functions/.env.example supabase/functions/.env
supabase functions serve --env-file supabase/functions/.env
```

`supabase/seed.sql` inserts the local `private.push_settings` row. The function tests run without Deno installed:

```bash
docker run --rm -v "$PWD/supabase/functions:/functions" -w /functions/send-push denoland/deno:2.1.4 deno test
```

1. Run a push server: `docker run -d -p 8085:80 binwiederhier/ntfy serve --base-url http://<host LAN IP>:8085`.
2. Set `PUSH_ALLOWED_ENDPOINT_HOSTS=<host LAN IP>` in `supabase/functions/.env` and restart `supabase functions serve`.
3. Install the F-Droid build of the ntfy app and set its Default server to `http://<host LAN IP>:8085`.
4. In Mesgard, open Settings, Notifications. Insert a message from another user while the app is in the background.

Use the LAN IP of the computer, not `10.0.2.2`: the emulator and the Edge Function container must both reach the push server at the same address, because the endpoint host is the one stored in the subscription. Verified this way: registration, delivery for all three preview modes and opening the conversation by tapping the notification.

</details>

<details>
<summary><strong>Setting up the backend by hand</strong></summary>

```
supabase link --project-ref <your-project-ref>
supabase db push
supabase functions deploy admin-users
```

Then in the dashboard, under **Authentication**, keep **Email** on, turn public sign-ups off, and enable TOTP multi-factor.

**Create the administrator's login:** open **Authentication, Users, Add user, Create new user**. Enter the email and a strong password, and keep **Auto Confirm User** on. A profile is created automatically with the part of the email before the `@` as display name. To use a real name, run this in the **SQL Editor**:

```sql
update public.profiles
set display_name = 'Maria Santos'
where id = (select id from auth.users where email = 'admin@school.example');
```

**Create the organization and make that person its administrator.** Replace the name, the slug (lowercase letters, digits and hyphens only) and the email:

```sql
with new_organization as (
  insert into public.organizations (name, slug)
  values ('Rizal National High School', 'rnhs')
  returning id
)
insert into public.org_members (org_id, user_id, role)
select new_organization.id, users.id, 'org_admin'
from new_organization, auth.users as users
where users.email = 'admin@school.example';
```

Adding the first member also creates the organization's announcement conversation. Run the second statement again with another name, slug and administrator to host another organization on the same server. A person can belong to several organizations, and the Administration page shows the first one they administer. The database password and the `service_role` key are not needed for any of this.

</details>

<details>
<summary><strong>Illustrations</strong></summary>

The empty states use vector drawables in `app/src/main/res/drawable/` named `illustration_<screen>.xml` (chats, conversation, contacts, search, members, group_no_group, group_no_member, audit_log). They refer to the color roles in `values/illustration_colors.xml`, and `values-night/illustration_colors.xml` redefines them for the dark theme. The accent role is the brand primary. The illustrations do not follow the contrast levels or dynamic color.

To add or replace one: download the SVG from unDraw with its default colors, convert it to a vector drawable, replace the unDraw colors with the roles (accent `#6c63ff`, ink `#090814`, `#2f2e41` and `#3f3d56`, surface `#fff`, surface variant `#f2f2f2`, outline `#e6e6e6` and `#e4e4e4`, strong outline `#ccc`, `#cacaca` and `#d6d6e3`), show it with `EmptyState` (`ui/common/EmptyState.kt`) and add it to `IllustrationResourcesTest`.

</details>

## Security

To report a vulnerability, see [SECURITY.md](SECURITY.md).

## License

This project is licensed under the GNU Affero General Public License v3.0 or later. See the
[LICENSE](LICENSE) file for details.

Mesgard is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

## Credits

The illustrations are from [unDraw](https://undraw.co/) by Katerina Limpitsouni, free for personal and commercial use without attribution. Check the licence on the unDraw site before adding more.
