<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Mesgard icon" width="160" height="160">

# Mesgard

**Your campus. Your chat. Your server.**

Mesgard is an open-source, real-time messaging app for schools, universities and other institutions. It connects to your own Supabase backend instead of a company's servers. Whether you use a Supabase Cloud project or a self-hosted instance, your messages live where you decide.

![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)

</div>

## Features

* **Bring your own backend:** Connect to any Supabase project, Cloud or self-hosted, with your project URL and public (anon) key. Switch servers anytime from the Sign in screen. Administrators can share a setup link or a QR code.
* **Private per organization:** People only see their own organization's data, enforced by Row Level Security in your database.
* **Direct messages and group chats:** Real-time messaging with typing indicators, online status, read marking and unread counts.
* **Announcements:** Every organization has an announcement conversation. Administrators and staff post to everyone in it. Students can read but not reply.
* **Photos and files:** Send photos and documents (up to 20 MB each, 10 per message). Photos open in a full screen viewer.
* **Works offline:** Your conversations are saved on the device (encrypted), so the list opens instantly. You can write without a connection, and messages send when you are back online. A message that fails to send can be retried or discarded.
* **Notifications without Google:** Push uses [UnifiedPush](https://unifiedpush.org) with a distributor app such as ntfy. There is no Firebase and no Google services, and an institution can run its own push server. Choose how much a notification shows: sender and text, sender only, or nothing.
* **Administration:** Administrators get an Administration page in Settings with quick actions, counts and the latest changes. They add members, import a roster from a CSV file, reset passwords, change roles, manage groups and read an audit log of every change. New accounts get a temporary password and must choose their own at first sign in. Anyone can change their own password later from their Account.
* **Two-step verification:** Administrators and staff can be required to enter a one-time code from an authenticator app when they sign in, and students can turn it on. The database enforces it, a lost phone is reset by an administrator, and there is no SMS. See [`docs/mfa.md`](docs/mfa.md).
* **Themes and privacy:** Light and dark themes, three contrast levels, dynamic colors, an optional app lock and an optional screen privacy mode that blocks screenshots.

**Not end-to-end encrypted.** Messages are protected in transit (TLS) and on the device, and your server can read them. This is by design, so that your institution can search, audit and keep its own records.

## Getting Started

You need Android 8.0 (API 26) or later, and a Supabase project that is reachable over **HTTPS**: either a [Supabase Cloud](https://supabase.com) project or your own self-hosted instance.

1. Create a project at [supabase.com](https://supabase.com) (or run your own).
2. Install the [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started) and sign in with `supabase login`. Then run the setup script, which links the project, applies the migrations, deploys the account function, sets the Auth settings, creates your first organization and its administrator, and prints the link that connects the app:
   ```
   node scripts/server/index.mjs setup --project-ref <your-project-ref>
   ```
   It needs [Node.js](https://nodejs.org) 18 or later, and no Docker. The details, a checklist command (`doctor`) and the same steps by hand are in [`docs/supabase-setup.md`](docs/supabase-setup.md). You can also run the setup from the **Actions** tab of your own copy of the repository, with nothing installed. Push notifications are optional: see [`docs/push-notifications.md`](docs/push-notifications.md).
3. Open Mesgard on your phone, open the link the script printed (or enter the **Project URL** and **anon (publishable) key** from the project's **Connect** dialog), and sign in with the administrator's email and the temporary password. You are asked to choose your own. After that, the administrator adds everyone else from the app.

**Only use the anon (publishable) key.** Never paste a secret or `service_role` key into the app: it bypasses Row Level Security. The app refuses both.

To use another server later, tap **Change server** on the Sign in screen. More details are in [`docs/server-setup.md`](docs/server-setup.md). When you update Mesgard later, update the server first: see [`docs/upgrading.md`](docs/upgrading.md). To require two-step verification for administrators and staff, follow [`docs/mfa.md`](docs/mfa.md).

## Verification

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

## Mapping Files

Each release on GitHub includes a `mapping-<version>.txt` file alongside the APK. This file is needed to deobfuscate stack traces from crash reports: match the file to the version shown in the crash report header and use it with `retrace` from the Android SDK.

## Security

To report a vulnerability, see [SECURITY.md](SECURITY.md).

## License

This project is licensed under the GNU Affero General Public License v3.0 or later. See the
[LICENSE](LICENSE) file for details.

Mesgard is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

The illustrations are from [unDraw](https://undraw.co/) (see [`docs/illustrations.md`](docs/illustrations.md)).
