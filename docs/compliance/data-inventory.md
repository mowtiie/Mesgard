# Data inventory

What Mesgard stores, where it lives and who can read it. The institution can use this as input for its privacy impact assessment and its registration of data processing systems. Check it against the current migrations before each release, because the schema changes.

## On the server (Supabase: PostgreSQL, Auth, Storage)

| Data | Where | Who can read it | Notes |
|---|---|---|---|
| Email, password hash, login metadata | Auth | Auth service, administrators through the Auth admin API | Accounts are created by the institution. A temporary password is shown once to the administrator |
| Two-step verification factor (TOTP secret) | Auth | Auth service | Optional for students, can be required for administrators and staff |
| Display name, avatar, status | `profiles` | People who share a conversation or an organization | |
| Organization membership and role | `org_members` | Members of the organization | Roles: `org_admin`, `staff`, `member` |
| Groups and group membership | `groups`, `group_members` | Members, and administrators of the organization | |
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
| Online status and typing | Realtime Presence and Broadcast | Members of the organization or conversation | Not stored. Visible to every member of the organization today |

## On the device (Android)

| Data | Where | Protection |
|---|---|---|
| Messages, conversations, people, groups | Room database | SQLCipher. The key is wrapped by a non-exportable Android Keystore key |
| Sign-in session | `noBackupFilesDir/session.bin` | Wrapped by the same Keystore key |
| Attachment files | `noBackupFilesDir/attachments` | Not encrypted. At most 200 MiB cached |
| Copies opened in another app | `cacheDir/shared_attachments` | Not encrypted. Deleted on sign out |
| Push endpoint, device settings | Shared preferences | Excluded from backups |

Android backup and device transfer are off. Signing out erases the database and both attachment directories.

## Third parties

- The push server chosen by the institution (a UnifiedPush distributor such as a self-hosted ntfy). It sees encrypted payloads and when a message arrives for a device, not the sender or text when the preview mode hides them.
- The hosting provider, when the institution does not host the server itself.
- No analytics, advertising, crash reporting or Google services.

## Retention and erasure today

- There is no automatic retention limit. Messages, files and the audit log stay until someone deletes them.
- Deleting a message blanks its text. The original goes to `deleted_message_bodies` for administrators.
- Removing a member keeps the account, profile and messages (foreign keys). The administrator can run the erase flow, which depends on the server setting `private.erasure_settings.mode`: `off`, `keep_messages` (default) or `blank_messages`.
- There is no legal hold yet. The `blank_messages` mode and the erase flow can destroy material a case or an order may require. The institution must decide before using them, until the legal hold exists.

## Data subject rights

| Right | Today |
|---|---|
| Access and portability | No export in the app. The administrator must query the database |
| Correction | Administrators can change roles. The database lets a person update their own profile name, status and avatar path, but the app has no screen for it yet, so a correction goes through the administrator |
| Erasure | The erase flow described above |
| Objection and complaint | Outside the app, through the institution's Data Protection Officer |
