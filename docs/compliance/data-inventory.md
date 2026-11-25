# Data inventory

What Mesgard stores, where it lives and who can read it. The institution can use this as input for its privacy impact assessment and its registration of data processing systems. Check it against the current migrations before each release, because the schema changes.

## On the server (Supabase: PostgreSQL, Auth, Storage)

| Data | Where | Who can read it | Notes |
|---|---|---|---|
| Email, password hash, login metadata | Auth | Auth service, administrators through the Auth admin API | Accounts are created by the institution. A temporary password is shown once to the administrator |
| Two-step verification factor (TOTP secret) | Auth | Auth service | Optional for students, can be required for administrators and staff |
| Display name, avatar, status | `profiles` | People who share a conversation or an organization | |
| Username | `org_members.username` | Members of the same organization, who see it with the display name | Released names are held back for 90 days (`private.released_usernames`, with the person's id) so nobody takes one to impersonate. That list is not cleared when a person is removed or erased |
| Profile picture (a 512 pixel JPEG, no metadata) | Storage bucket `avatars`, `org_members.avatar_version` | Members of the same organization, while it is the current picture and the organization allows pictures for that person's role. Students have none by default | An older version is deleted by the app, and the pictures of an erased person are deleted by the `admin-users` function. A picture a report keeps as evidence is protected from deletion and stays as long as the report exists, and reviewers can read it. The retention job does not touch it. It may show a minor, so a privacy lawyer must review how it is kept and for how long |
| Group picture (a 512 pixel JPEG, no metadata) | Storage bucket `avatars`, `groups.avatar_version` | Members of the same organization, while it is the current picture and the organization allows pictures. Administrators, owners and moderators of the group set it | An older version is deleted by the app. A picture a report keeps as evidence is protected from deletion and stays as long as the report exists, and reviewers can read it. The retention job does not touch it. The files of a deleted group stay until a cleanup exists. It may show a minor, so a privacy lawyer must review how it is kept and for how long |
| Profile notice (that a reviewer reset the username or removed the picture) | `profile_notices` | The person only | Shown once, then kept with a seen time. It names no reporter and no report |
| Organization membership and role | `org_members` | Members of the organization | Roles: `org_admin`, `staff`, `member` |
| Groups and group membership | `groups`, `group_members` | Members, and administrators of the organization | Who may create groups is an organization setting (administrators only, staff or everyone). The messages of a group a person created are not readable by administrators. An administrator can open them for seven days after writing a reason of at least ten characters (`group_access_grants`). Each opening is in the audit log with the reason, and the members of the group see that it happened and when, not who or why. The archives of deleted and edited messages and the data export leave these messages out |
| Conversations and participants, with read marker, mute, pin, archive, hidden before | `conversations`, `conversation_participants` | Participants (preferences: the person only) | |
| Message text, sender, time, reply and mentions | `messages` | Participants of the conversation | Plain text, searchable on the server |
| Edited and deleted message text | `message_edits`, `deleted_message_bodies` | Organization administrators | Kept for audit |
| Read receipts | `message_receipts` | The sender of the message | |
| Attachments (photos, documents up to 20 MiB) | `attachments` and the private Storage bucket `message-attachments` | Participants of the conversation | Files are not encrypted by the application. Rely on disk encryption of the host |
| Pinned messages | `pinned_messages` | Participants | |
| Favorite contacts | `favorite_contacts` | The person only | |
| Notification preview mode | `user_settings`, `organizations.notification_preview_default` | The person, administrators for the default | |
| Push subscription (endpoint and keys) | `push_subscriptions` | The Edge Function `send-push` (service role) | The endpoint is a capability URL. Keep it out of logs |
| Administrative actions | `audit_logs` | Organization administrators | Append only |
| Online status and typing | Realtime Presence and Broadcast | Members of the organization or conversation | Not stored. A person is shown online only when they choose; until then administrators and staff are shown and students are not. The choice is `user_settings.show_online_status`, private to the person. A modified app could still announce itself, so this is a privacy default, not an access control |

## On the device (Android)

| Data | Where | Protection |
|---|---|---|
| Messages, conversations, people, groups | Room database | SQLCipher. The key is wrapped by a non-exportable Android Keystore key |
| Sign-in session | `noBackupFilesDir/session.bin` | Wrapped by the same Keystore key |
| Attachment files | `noBackupFilesDir/attachments` | Encrypted per file with AES-256-GCM once sent or received, the key wrapped by the Keystore key. A file waiting to be sent is plain until then. At most 200 MiB cached |
| Copies opened in another app, and exports | `cacheDir/shared_attachments` | Not encrypted, because another app has to read them. Deleted on sign out |
| Push endpoint, device settings | Shared preferences | Excluded from backups |

Android backup and device transfer are off. Signing out erases the database and the attachment directories. So does a session the server ends, for example when the login is disabled or the person is removed, the next time the app learns of it.

## Third parties

- The push server chosen by the institution (a UnifiedPush distributor such as a self-hosted ntfy). It sees encrypted payloads and when a message arrives for a device, not the sender or text when the preview mode hides them.
- The hosting provider, when the institution does not host the server itself.
- No analytics, advertising, crash reporting or Google services.

## Retention and erasure today

- An organization admin can set a retention limit (30 days to 10 years). A job then erases older messages and their files, except what a legal hold or an open report protects; see [message retention](retention.md). Without a limit, or without the job running, messages and files stay until someone deletes them. The audit log is never erased.
- Deleting a message blanks its text. The original goes to `deleted_message_bodies` for administrators.
- Removing a member keeps the account, profile and messages (foreign keys). The administrator can run the erase flow, which depends on the server setting `private.erasure_settings.mode`: `off`, `keep_messages` (default) or `blank_messages`.
- A legal hold (Administration, Legal holds) on a person, a group or the whole organization, and any open report about or by a person, stops the erase flow and the retention job from touching them. Reports keep a frozen copy of the reported message.

## Data subject rights

| Right | Today |
|---|---|
| Access and portability | An organization admin exports one person's data as a JSON file (Members, Export data). It holds the account, profile, groups, conversations and the messages the person sent, with the originals of deleted messages, earlier versions of edited ones and the details of files. It does not hold messages of other people, reports, legal holds, the messages the person sent in groups a person created, or the files themselves. A request that covers those messages needs another way, for example the person reading them in the app or break-glass access with a recorded reason. Each export is recorded in the audit log |
| Correction | Administrators can change roles. The database lets a person update their own profile name, status and avatar path, but the app has no screen for it yet, so a correction goes through the administrator |
| Erasure | The erase flow described above |
| Objection and complaint | Outside the app, through the institution's Data Protection Officer |
