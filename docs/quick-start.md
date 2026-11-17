# Quick start

Set up your own Mesgard server in four steps. You need a computer with [Node.js](https://nodejs.org) 18 or later and [Git](https://git-scm.com), and an Android phone.

## 1. Create a Supabase project

1. Sign up at [supabase.com](https://supabase.com) and choose **New project**.
2. Pick the region closest to your users, and set a **database password**. Save it in a password manager.
3. Wait until the project is ready, then copy its **project ref**: the 20 letters in the address, `supabase.com/dashboard/project/<project ref>`.

## 2. Install the tools

1. Install the [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started).
2. Download Mesgard and sign in to Supabase:

   ```
   git clone https://github.com/mowtiie/Mesgard.git
   cd Mesgard
   supabase login
   ```

## 3. Run the setup

```
node scripts/server/index.mjs setup --project-ref <project ref>
```

It asks for your database password once, then asks for:

- the name of your school or organization,
- a short name for it (press Enter to accept the suggestion),
- the email and name of the first administrator.

When it finishes, it prints three things. Keep them for the next step:

- the **server address** and the **public key**, or a **link** that contains both,
- the administrator's **temporary password**, shown once.

Every line of the final checklist should say `[ok]`. If one says `[FAIL]`, the line under it tells you what to do, and you can run the same command again.

## 4. Connect the phone

1. Install Mesgard on the phone.
2. Open the link from step 3 on the phone, or open Mesgard and type the server address and the public key.
3. Sign in with the administrator's email and the temporary password. Mesgard asks you to choose your own password.

You are done. From **Settings**, **Administration**, add the other people in your school.

## More

- Add two-step verification for administrators and staff: [`mfa.md`](mfa.md)
- Push notifications: [`push-notifications.md`](push-notifications.md)
- The same setup without installing anything, or step by step by hand: [`supabase-setup.md`](supabase-setup.md)
