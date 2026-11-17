<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Mesgard icon" width="160" height="160">

# Mesgard

**Your campus. Your chat. Your server.**

A free, open-source messaging app for schools and other institutions. People chat in an Android app, and every message is stored on a server that **you** own and control.

![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)

</div>

## Contents

[What it does](#what-it-does) · [Requirements](#requirements) · [Quick start](#quick-start) · [After setup](#after-setup) · [Two-step verification](#two-step-verification) · [Push notifications](#push-notifications) · [Removing and erasing people](#removing-and-erasing-people) · [Updating](#updating) · [Troubleshooting](#troubleshooting) · [Verify your download](#verify-your-download) · [For developers](#for-developers)

## What it does

* **Chat:** direct messages, group chats, announcements, photos and files.
* **Your data stays with you:** it lives in your own [Supabase](https://supabase.com) project (cloud or self-hosted). Each organization only sees its own data.
* **Easy administration:** add people one by one or from a CSV file, manage groups, reset passwords and read an audit log.
* **Safer sign in:** optional two-step verification with any authenticator app.
* **Works offline:** messages are saved on the phone and send when the connection returns.
* **No Google:** notifications use [UnifiedPush](https://unifiedpush.org), with no Firebase and no Google services.
* **Privacy options:** app lock, screen privacy, and a choice of how much a notification shows.

**Not end-to-end encrypted.** Messages are protected in transit and on the phone, but your server can read them. This is on purpose, so your institution can search, audit and keep its own records.

## Requirements

- **Phones:** Android 8.0 or later.
- **Server:** a Supabase project reachable over HTTPS.
- **To set it up:** a computer with [Node.js](https://nodejs.org) 18+, [Git](https://git-scm.com) and the [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started). Or nothing at all, using [GitHub](#from-github-without-installing-anything).
- **Optional:** an authenticator app (two-step verification) and a push app such as [ntfy](https://ntfy.sh) (notifications).

## Quick start

1. **Create a Supabase project** at [supabase.com](https://supabase.com). Choose a region near your users and save the database password.
   - Enable Data API: **on**.
   - Automatically expose new tables: **off**.
   - Enable automatic RLS: **on**.
   - Copy the **project ref** (the 20 letters in the project address).
2. **Get the tools:**
   ```
   git clone https://github.com/mowtiie/Mesgard.git
   cd Mesgard
   supabase login
   ```
3. **Run the setup** and answer the questions (organization name, administrator email and name, and whether to require two-step verification, where Enter means yes):
   ```
   node scripts/server/index.mjs setup --project-ref <project ref>
   ```
   It prints a **link**, the **public key** and a one-time **temporary password**. Keep them. Every line of the final checklist should say `[ok]`. If not, the line tells you what to do, and you can run the command again.
4. **Connect the phone:** install Mesgard, open the link on the phone, and sign in with the administrator's email and the temporary password. The app then guides you through two-step verification (if chosen) and a new password.

Never paste a secret or `service_role` key into the app. It accepts only the public key.

### From GitHub, without installing anything

Not run on GitHub yet, so treat it as untested.

1. Copy (fork) this repository to your GitHub account.
2. In your copy, add the secrets `SUPABASE_ACCESS_TOKEN`, `SUPABASE_DB_PASSWORD` and `MESGARD_ADMIN_PASSWORD` (12+ characters) under **Settings, Secrets and variables, Actions**.
3. Open **Actions, Deploy backend, Run workflow**, enter the project ref and the organization details.

## After setup

- **Connect more phones:** share the setup link, a QR code of it, or the server address and public key. The app only accepts `https` addresses.
- **Add people:** **Settings, Administration, Members**. Add one by one or import a CSV file (an email column is required). Each person gets a temporary password, shown once, and chooses their own at first sign in.
- **Groups and audit log:** also under **Administration**.
- **Change server:** tap **Change server** on the Sign in screen (not while signed in).

## Two-step verification

People enter a 6-digit code from an authenticator app when they sign in. The server enforces it, not just the app.

- **Who:** administrators and staff when it is required. Students can turn it on themselves in **Settings, Account**.
- **New server:** the setup asks, and yes is the default.
- **Existing server:** set up your own first in **Settings, Account**, then run this in the Supabase SQL Editor:
  ```sql
  update private.mfa_settings set enforced = true;
  ```
- **Lost phone:** an administrator resets it in **Members, Reset two-step verification**. There are no backup codes and no SMS.

<details>
<summary><strong>If the only administrator is locked out</strong></summary>

Run in the SQL Editor to remove their factor:

```sql
delete from auth.mfa_factors
where user_id = (select id from auth.users where email = 'admin@school.example');
```

To turn the requirement off for everyone for a while:

```sql
update private.mfa_settings set enforced = false;
```

</details>

## Push notifications

Optional. Without it, messages arrive only while the app is open.

1. Run `node scripts/server/index.mjs push --project-ref <project ref>`. Add `--push-hosts ntfy.school.edu` to allow only your own push server.
2. On each phone, install a push app such as ntfy and choose it in **Settings, Notifications**.

An institution can run its own push server, so notifications also work on a campus network with no internet. Each person chooses what a notification shows: sender and text, sender only (default), or nothing.

## Removing and erasing people

- **Remove:** the person leaves the organization. Their messages stay, and you can add them back later.
- **Remove and erase:** also deletes their personal data (name, email, devices, sign in details). It **cannot be undone**, and the person cannot return.
- Find both in **Settings, Administration, Members, Remove**. Erase is a checkbox that appears only when the server allows it, and not for someone who is in another organization.

Choose what erasing means for your server in the SQL Editor (`keep_messages` is the default):

```sql
update private.erasure_settings set mode = 'keep_messages';
```

| Mode | Result |
|---|---|
| `keep_messages` | Personal data is erased. Messages stay and show "Former member". |
| `blank_messages` | The same, and the person's messages and files are erased too. |
| `off` | Nobody can be erased. |

Backups and Supabase's own logs are not erased. Ask your data protection officer which mode fits before the first erasure. If an erase fails half way, the person is already removed and the app cannot finish it yet, so repeat the `erase_member` request to the `admin-users` function.

## Updating

Update the server first, then the app.

1. Back up the database.
2. Run `git pull`, then the same setup command again. It only applies what is new and creates nothing twice.
3. Install the new app.

Check a server at any time with `node scripts/server/index.mjs doctor`. It only reads, and tells you what to fix. If the server is older than the app, the app shows "This server needs an update".

## Troubleshooting

| Problem | What to do |
|---|---|
| "Supabase CLI was not found" | Install it and open a new terminal. |
| "No project selected" | Add `--project-ref <ref>` to the command. |
| A setup step failed | Fix the cause shown above it and run the command again. |
| Forgot the temporary password | Set a new one in the Supabase dashboard, Authentication, Users. |
| Locked out by two-step verification | See the box under [Two-step verification](#two-step-verification). |
| Push does nothing | Run `doctor`, then check that each phone has a push app chosen. |
| "Unable to reach the server" | Check the phone's connection and the server address. |

## Verify your download

Releases on GitHub are signed with my key. Only install APKs that match.

1. **Android signature:**
   ```
   apksigner verify --print-certs --verbose mesgard-vX.X.apk
   ```
   The SHA-256 fingerprint must be:
   ```
   8B:67:51:F3:C3:31:85:63:5F:98:95:30:B6:C0:73:A1:39:7B:3D:41:2B:EF:AE:69:06:A2:EB:58:45:D2:DE:63
   ```
2. **PGP signature:** import my key, check its fingerprint, then verify.
   ```
   curl -O https://mowtiie.cc/PGP_PUBLIC_KEY.asc
   gpg --import PGP_PUBLIC_KEY.asc
   gpg --verify mesgard-vX.X.apk.asc mesgard-vX.X.apk
   ```
   The key fingerprint must be `9EA2 8F46 7802 5092 7643 1B69 42B5 FA42 AA63 90E1`. A match, not the import, is what establishes trust. Use both checks for the highest confidence.

Each release also has a `mapping-<version>.txt` file, used to read crash reports with `retrace` from the Android SDK.

## For developers

Needs Android Studio, Docker (local Supabase and tests), Node.js 18+ and the Supabase CLI.

```
./gradlew assembleDebug testDebugUnitTest      # build and test the app
./gradlew ktlintCheck detekt lintDebug         # style and lint
supabase start && supabase test db             # local backend and database tests
node --test scripts/server/                    # server tool tests
./gradlew clean assembleRelease lintVitalRelease   # signed release (keystore.properties or SIGNING_* variables)
```

- **Local accounts:** from `supabase/seed.sql`, for example `admin@rnhs.test` with password `password123`. Never seed or reset a real project.
- **Point a build at a server:** set `supabase.url` and `supabase.anonKey` in `local.properties`. Debug builds allow plain `http` for the local stack.
- **New server feature the app needs:** raise `api_level` in `server_info()` and `REQUIRED_API_LEVEL` in the same change. Migrations are only added, never edited.
- **New table or function:** it needs Row Level Security, the "Require two-step verification" policy and `private.require_mfa()`. The database tests fail if one is missing.

<details>
<summary><strong>Setting up the backend by hand</strong></summary>

```
supabase link --project-ref <your-project-ref>
supabase db push
supabase functions deploy admin-users
```

In the dashboard under **Authentication**, keep Email on, turn public sign-ups off and enable TOTP. Create the administrator under **Authentication, Users, Add user** (keep Auto Confirm on), then run this in the SQL Editor with your own name, short name and email:

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

</details>

## Security

To report a vulnerability, see [SECURITY.md](SECURITY.md).

## License

GNU Affero General Public License v3.0 or later. See [LICENSE](LICENSE). Mesgard is free software: you can redistribute it and modify it under those terms.

## Credits

Illustrations are from [unDraw](https://undraw.co/) by Katerina Limpitsouni.
