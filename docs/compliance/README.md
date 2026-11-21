# Compliance pack (Philippines)

This pack helps a school or institution that runs Mesgard prepare its own compliance work. It is a starting point, not legal advice. The sources behind it are news reports and law firm summaries, and some official texts could not be opened. Have the institution's Data Protection Officer or a Philippine privacy lawyer review it before relying on it.

## Who is responsible for what

| Party | Role under the Data Privacy Act (RA 10173) | Main duties |
|---|---|---|
| The institution | Personal information controller | Lawful basis, privacy notices, parental consent in enrollment forms, Data Protection Officer, registration with the National Privacy Commission (NPC), privacy impact assessment, breach notification, child protection and anti-bullying policies |
| Mesgard (software only, self-hosted) | Software vendor | Provides software that supports those duties. Never receives the institution's data |
| Mesgard (if it ever hosts for an institution) | Personal information processor | A written outsourcing agreement, NPC registration of its own, the security measures of NPC Circular 2023-06 |
| Hosting provider (for example Supabase Cloud) | Sub-processor | Cross-border transfer, covered in [hosting guidance](hosting-guidance.md) |

## Contents

- [Data inventory](data-inventory.md): what Mesgard stores, where, who can read it, and how long it stays.
- [Breach runbook](breach-runbook.md): the first 72 hours after a suspected personal data breach.
- [Message retention](retention.md): the limit an organization sets, what is protected and how to run the job.
- [Hosting guidance](hosting-guidance.md): self-hosting, cross-border transfer, the push server and government data rules (EO 119).

## What Mesgard does not do

- No advertising, tracking, analytics or profiling of users.
- No automated decisions about people.
- No Google or Firebase services; push uses UnifiedPush through a server the institution chooses.
- No end-to-end encryption: the institution can search, audit and preserve messages. Never describe Mesgard as end-to-end encrypted.

## Laws and rules this pack refers to

| Law or rule | Subject |
|---|---|
| Data Privacy Act (RA 10173) and its implementing rules | Personal and sensitive personal information |
| NPC Circular 2023-06 | Security of personal data |
| NPC Circular 16-03 | Personal data breach management |
| NPC Circular 2022-04 | Registration of Data Protection Officers and data processing systems |
| NPC Advisory 2024-03 | Child-oriented transparency |
| RA 11930 | Online sexual abuse or exploitation of children |
| RA 10175 (section 13) | Preservation of computer data |
| RA 11313 | Safe Spaces Act |
| RA 10627 and its revised implementing rules (2025) | Anti-Bullying Act |
| DepEd Order 40 s. 2012 and its 2024 supplement | Child Protection Policy |
| DepEd Order 49 s. 2022 | Communication between personnel and learners |
| Executive Order 119 (2026) | Government data classification and residency |

## Not yet built

Built: reporting a message, designated reviewers, a legal hold, a privacy notice link, a retention limit, a data export for access requests, encrypted attachment files and a wipe when the server ends a session, messaging rules (no staff to student direct messages, quiet hours) and a choice to hide online status. Planned and not built yet: reporting a person from a screen, a notification when a report arrives, and opening evidence files in the app. Until they exist, the institution handles those duties outside the app.
