# Security Policy

Mesgard handles messages for schools and other institutions, so security reports are taken seriously.

## Supported versions

Only the latest release on [GitHub Releases](https://github.com/mowtiie/Mesgard/releases) receives security fixes. Update to the newest version before reporting an issue.

## Reporting a vulnerability

**Do not open a public issue for a security problem.**

Report it privately in one of these ways:

1. Use **Report a vulnerability** on the [Security tab](https://github.com/mowtiie/Mesgard/security/advisories/new) of this repository (private vulnerability reporting).
2. Or email mowtiie.dev@gmail.com. If the report is sensitive, encrypt it with the PGP key described in the [README](README.md#pgp-signing).

Please include:

* What the problem is and which part is affected (the Android app, the database migrations and Row Level Security policies, or an Edge Function).
* Steps to reproduce it, or a proof of concept.
* The app version and, if relevant, the server setup (Supabase Cloud or self-hosted).
* Your assessment of the impact, for example reading another organization's messages or acting as another user.

You will get an answer as soon as I can give one, and I will keep you informed until the problem is fixed. Once a fix is released, I am happy to credit you, unless you prefer to stay anonymous.

## Scope

In scope:

* The Android app.
* The database schema, migrations and Row Level Security policies in `supabase/`.
* The Edge Functions `admin-users` and `send-push`.
* Anything that lets one person read, change or delete data of another person or organization they should not reach, bypass sign in, roles or the app lock, or learn a secret.

Out of scope:

* Messages being readable by the server operator. Mesgard is not end-to-end encrypted by design.
* Screen privacy not stopping someone from photographing the screen with another device.
* A server that is misconfigured by its operator, for example a leaked `service_role` key, a missing HTTPS certificate or disabled Row Level Security.
* Denial of service, spam, social engineering and physical access to an unlocked device.
* Vulnerabilities in third-party software (Supabase, ntfy, Android) that are not caused by how Mesgard uses it. Please report those to the project concerned.

## Testing safely

Only test against your own server and your own accounts. Do not access, change or delete data that belongs to other people, and do not run tests against a server you do not operate.
