# Setting up the Supabase backend

The shortest path is in the [quick start](quick-start.md). This page has the details.

Mesgard needs a Supabase project with the database schema, one Edge Function and a few Auth settings. A script does all of it, and you can also do each step by hand.

## What you need

- A [Supabase](https://supabase.com) project (Cloud or self-hosted) that is reachable over HTTPS.
- The [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started), signed in with `supabase login`.
- [Node.js](https://nodejs.org) 18 or later, only for the script.
- A copy of this repository. Docker is not needed.

## The quick way

1. Create a project in the Supabase dashboard. Pick the region closest to your users and keep the database password somewhere safe.
2. In the repository, run:

   ```
   node scripts/server/index.mjs setup --project-ref <your-project-ref>
   ```

   The project ref is the 20 letter code in the project URL. The first run links the project, and the CLI may ask for the database password.

The script then does these things in order, and tells you what it is doing:

| Step | What happens |
|---|---|
| Link | Links the folder to the project, unless it already is. |
| Migrations | Applies the migrations in `supabase/migrations` that the project does not have yet. |
| Function | Deploys `admin-users`, the function that creates accounts, without Docker. |
| Auth settings | Shows the differences, then asks before turning public sign-ups off and enabling TOTP. Nothing else in your Auth settings is touched. |
| First organization | Asks for the organization name, its short name, and the administrator's email and name. Creates the login with a temporary password and adds the person as administrator. It is skipped when an organization already exists. |
| Connect | Prints the server address, the public key and a `mesgard://setup` link that fills the app's server form. |
| Check | Runs the checklist below. |

The temporary password is shown once. The administrator must choose their own at the first sign in. The script reads the secret key of the project only to create that login, keeps it in memory, and never prints it or stores it.

You can give the answers as options so nothing is asked: `--org-name`, `--org-slug`, `--admin-email`, `--admin-name`. Add `--yes` to apply the Auth settings without a question, and `--add-organization` to create another organization on a server that has one.

## Without installing anything: GitHub Actions

If you would rather not install the CLI or Node, a workflow in this repository does the same setup from the Actions tab.

1. Copy the repository to your own GitHub account (a fork).
2. In the Supabase dashboard, create an access token under your account settings, then in your copy open **Settings, Secrets and variables, Actions** and add these secrets:

   | Secret | Value |
   |---|---|
   | `SUPABASE_ACCESS_TOKEN` | The access token |
   | `SUPABASE_DB_PASSWORD` | The database password you chose for the project |
   | `MESGARD_ADMIN_PASSWORD` | A password of 12 or more characters for the first administrator. Only needed when you create the first organization. |

3. Open the **Actions** tab, choose **Deploy backend**, and select **Run workflow**. Enter the project ref. To create the first organization, also enter its name, the administrator's email and name, and optionally a short name. Leave those empty to only deploy.
4. When the run finishes, its log ends with the connect link and the checklist.

The administrator signs in with `MESGARD_ADMIN_PASSWORD` and is asked to choose their own password right away. That password is never printed. The log only contains public values (the project address and the public key), but anyone who can read your copy's Actions logs can see them, so keep the copy private if you can. To update later, bring your copy up to date with this repository and run the workflow again with the organization fields empty.

This workflow has not been run on GitHub yet. The script it runs is tested, and the workflow file passes `actionlint`.

## Checking a server

```
node scripts/server/index.mjs doctor
```

It only reads. It reports whether the server is reachable over HTTPS, public sign-ups are off, the migrations are all applied, the server is new enough for the app, `admin-users` is deployed, the Auth settings match, an organization and an administrator exist, and whether two-step verification enforcement and push notifications are set up. Each line that needs attention says what to do. Add `--project-ref <ref>` for a project other than the linked one, or `--local` for the local stack.

## Updating

Update the server before the app, as described in [`upgrading.md`](upgrading.md). Running `setup` again is safe: it applies only new migrations, deploys the function again, leaves matching settings alone and does not create another organization.

## Doing it by hand

Run the same steps yourself:

```
supabase link --project-ref <your-project-ref>
supabase db push
supabase functions deploy admin-users
```

Then in the dashboard, under **Authentication**, keep **Email** on, turn public sign-ups off, and enable TOTP multi-factor. Create the first organization as described in [`first-organization.md`](first-organization.md), and copy the **Project URL** and the publishable (anon) key from the **Connect** dialog into the app.

## Optional

- **Push notifications:** [`push-notifications.md`](push-notifications.md).
- **Two-step verification for administrators and staff:** [`mfa.md`](mfa.md). Setup turns TOTP on but leaves enforcement off.

## Problems

- **"The Supabase CLI was not found":** install it and open a new terminal, so that `supabase` runs there.
- **"No project selected":** pass `--project-ref`, or run `supabase link --project-ref <ref>` first.
- **A step failed:** fix the cause shown above it and run `setup` again. Every step can be repeated.
- **Setup stopped after creating the login:** run it again with the same email. It reuses the existing login and finishes the organization, but the original temporary password stays unknown, so reset it from the dashboard.
- **The administrator forgot the temporary password:** set a new one in the dashboard under Authentication, Users. The mark that asks for a new password stays until they choose their own, so they are still asked to at the first sign in.
