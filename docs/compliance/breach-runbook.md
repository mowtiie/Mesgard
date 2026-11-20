# Personal data breach runbook

For the institution's Data Protection Officer and the server administrator. Under NPC Circular 16-03, as reported by secondary sources, the NPC and the affected people must be notified within 72 hours of knowing, or reasonably believing, that a breach requiring notification happened. A full report follows within 5 days unless the NPC grants more time. In May 2026 the NPC issued a clarification on submitting breach notifications through its Data Breach Notification Management System. Confirm the current rules and the submission method on privacy.gov.ph before relying on this page.

## 1. First hour: contain

1. Write down the time you learned of the incident. The 72 hours start from knowledge or reasonable belief.
2. Tell the Data Protection Officer.
3. Stop the leak without destroying evidence:
   - Disable affected logins (Members, Remove, or ban the login in Auth). Do not erase anything.
   - Revoke sessions of affected accounts in Auth.
   - Rotate secrets that may have leaked: the service role key in the Supabase project, and the push webhook secret with `node scripts/server/index.mjs push --rotate`. The anon key is public by design and is not a secret.
   - If a phone is lost or stolen, disable the account. The database on the phone is encrypted and the app lock protects the screen, but attachment files on the phone are not encrypted.
4. Keep logs: Auth logs, Postgres logs, Edge Function logs, reverse proxy logs. Do not enable log rotation that deletes them.

## 2. First day: assess

Answer these and write the answers down.

- What data was involved (see the [data inventory](data-inventory.md))?
- How many people, and are any of them minors? Children's data raises the risk.
- Was sensitive personal information involved (for example school records or messages about health or discipline)?
- Was the data readable by the person who got it, or encrypted?
- Is there a risk of serious harm to the people involved?

Useful sources inside Mesgard:

- `audit_logs` records role changes, member adds and removals, group changes and pins. It does not record who read a message.
- Auth audit logs show sign-ins.
- The `message_receipts` and `deleted_message_bodies` tables help reconstruct what happened to a message.

## 3. Within 72 hours: notify

1. The Data Protection Officer decides, with the facts above, whether notification is required.
2. If it is, notify the NPC through the channel it currently requires, and the affected people. Tell them what happened, what data, what the institution is doing and who to contact.
3. If a child may be at risk of sexual abuse or exploitation, also report to law enforcement (RA 11930) and keep the evidence.

## 4. Within 5 days: full report

Describe the cause, the data and people involved, the measures taken, and what will prevent a repeat. Keep a copy.

## 5. After the incident

- Record the incident, the decision and the actions in the institution's breach log. Keep it, whether or not notification was required.
- Fix the cause. Apply pending migrations and updates. Review who holds administrator access.
- Test the restore of the backup (see the backup and restore guide when it exists).
