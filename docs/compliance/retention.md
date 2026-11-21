# Message retention

NPC Circular 2023-06 expects personal data to be kept only as long as necessary. Mesgard lets each organization choose
how long messages are kept, and a job erases what is older. Nothing is erased until an organization admin sets a limit
**and** the job runs.

## What the limit does

An organization admin chooses a limit between 30 days and 10 years, or no limit (Administration, Message retention).
The job then handles every message older than the limit:

- The text is blanked, as for a deleted message.
- The archived original of a deleted message and the earlier versions of an edited message are purged.
- The attachment rows and the files in Storage are deleted.

A message that is only a few days old is never touched, and an organization without a limit loses nothing.

## What is protected

The database decides this, so the job cannot go around it. A message is not erased while:

- a legal hold covers its sender, its group or the whole organization, or
- a report is open or in review about its sender, by its sender, or about its conversation.

Closing the report or releasing the hold lets the message go on the next run. A report keeps its own frozen copy of the
reported message until the report is closed, and the audit log is never erased.

## Running the job

The files can only be deleted through the Storage API, so the job is a command, not a database schedule:

```bash
node scripts/server/index.mjs retention --project-ref <ref>      # a hosted project
node scripts/server/index.mjs retention --local                  # the local stack
node scripts/server/index.mjs retention --local --dry-run        # how many messages are due, nothing changes
```

Each round lists the files, deletes them, then erases the rows, so a failure halfway is finished by running it again.
`--batch <1 to 1000>` sets the size of a round (default 200).

Schedule it once a day, for example with cron on a machine that has the Supabase CLI logged in:

```
0 3 * * * cd /opt/mesgard && node scripts/server/index.mjs retention --project-ref <ref> >> /var/log/mesgard-retention.log 2>&1
```

The command erases for good, so it only runs when the target is named. Test it with `--dry-run` first.

A self-hosted server (Phase 5) has no project ref yet. Until the Docker Compose package supports this command, a
self-hosted installation runs the same three steps by hand: list with `public.list_expired_attachment_paths(200)`,
delete those files through the Storage API with the service role key, then call `public.expire_messages(200)`, and
repeat until `private.expired_message_ids(1)` returns no row.

## What it does not cover

- Reports and their frozen evidence, which stay until an organization decides otherwise. Closed reports are not erased
  by this job yet.
- The audit log, push subscriptions and profiles.
- Copies on phones. Signing out removes them, and the app refetches only what the server still has.
- Backups of the database or the Storage volume. Apply the same limit to them.
