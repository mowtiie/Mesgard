# Removing and erasing people

There are two ways to take someone out of an organization, and a server decides how far the second one goes.

| | Remove | Remove and erase personal data |
|---|---|---|
| For | Someone who left, and may come back | Someone whose personal data must go |
| Their messages | Stay, with their name | Stay as "Former member", or are erased (see below) |
| Their name and email | Kept | Replaced with placeholders |
| Their login | Disabled | Disabled for good |
| Can they return? | Yes, add them again | No. The same email becomes a new person. |
| Undo | Add them again | **None** |

Both are in **Settings, Administration, Members**, under **Remove**. The erase option is a checkbox in the same dialog, and it only appears when the server allows it.

## Who can be erased

Only someone who belongs to **no other organization**. A person who is in two organizations can be removed from yours, never erased, because erasing would break their other account. An administrator cannot erase themselves.

## What the server does

Each server decides what erasure means. Run one statement in the SQL Editor:

```sql
update private.erasure_settings set mode = 'keep_messages';
```

| Mode | What happens | Use it when |
|---|---|---|
| `keep_messages` (default) | Personal data is erased. Messages stay and show "Former member". | Conversations and records must stay readable. |
| `blank_messages` | The same, and the person's messages are erased too: they show as deleted, the original text is purged, and attachments are deleted. | A school must remove what people wrote. |
| `off` | The checkbox does not appear. Nobody can be erased. | The school must keep everything. |

The mode applies when someone is erased. Changing it later does not change people who were already erased.

## What erasing removes

- The profile name, picture and status, which become "Former member".
- The email, replaced by a placeholder that cannot receive mail, and the phone number and sign in details of the login.
- Their sessions, authenticator apps, push subscriptions and settings.
- The rows in the sign in log of Auth that mention their email, name or device address.
- With `blank_messages`: their message text (also the copy that administrators can read), and their attachments, including files that were never attached to a message.

The audit log keeps a record that an administrator erased someone, without their name.

## Bringing someone back

Add them again in **Members, Add member** with the same email. A removed person gets their login back with a new temporary password and must choose their own. An email that belongs to someone active, or to another organization, is refused. An erased person cannot return, because their email was replaced.

## What erasing cannot do

- **Undo.** Take a backup first if you are unsure.
- **Reach backups.** Hosted backups and any copy you made keep the data until they expire.
- **Reach the request logs of Auth.** Supabase keeps logs outside the database, with their own retention. No SQL or app can erase those.
- **Remove what others wrote.** If someone typed another person's name in a message, it stays.
- **Replace advice.** Privacy laws, such as the Philippine Data Privacy Act of 2012 and GDPR, can ask a school to erase data and also to keep records. Ask the school's data protection officer which mode fits, and set it before the first erasure.

## If something fails

Erasing is made of steps, and each step can be repeated. If one fails, for example when the file storage is unreachable, the app shows an error. The person has already been removed by then, so they no longer appear in the Members list, and **the app has no screen yet to finish the erase**.

The repeat is the same request to the `admin-users` function, with the action `erase_member` and the person's id, sent by an administrator of the organization. It works for a person that organization removed, and it changes nothing for someone already erased. Until the app has a list of former members, whoever runs the server has to send that request. The `docker` or dashboard logs of `admin-users` say which step failed.
