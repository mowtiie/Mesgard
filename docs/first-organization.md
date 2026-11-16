# Creating the first organization

A new server has no organizations and no people. Create the first organization and its administrator once, with the
steps below. After that, the administrator adds everyone else from the app (Settings, Administration).

## 1. Create the administrator's login

In the Supabase dashboard open **Authentication**, then **Users**, then **Add user**, and choose **Create new user**.
Enter the email address and a strong password, and keep **Auto Confirm User** on, because the server has no
confirmation email for a first login. The menu labels can differ slightly between dashboard versions.

A profile is created automatically. Its display name is the part of the email before the `@`. To use a real name, run
this in the **SQL Editor** (replace the email and the name):

```sql
update public.profiles
set display_name = 'Maria Santos'
where id = (select id from auth.users where email = 'admin@school.example');
```

## 2. Create the organization and make that person its administrator

Run this in the **SQL Editor**. Replace the name, the slug (lowercase letters, digits and hyphens only) and the email:

```sql
with new_organization as (
  insert into public.organizations (name, slug)
  values ('Rizal National High School', 'rnhs')
  returning id
)
insert into public.org_members (org_id, user_id, role)
select new_organization.id, users.id, 'org_admin'
from new_organization, auth.users as users
where users.email = 'admin@school.example';
```

Adding the first member also creates the organization's announcement conversation, so announcements work right away.

## 3. Sign in and add the others

Open Mesgard, connect to the server (see [server-setup.md](server-setup.md)) and sign in with the administrator's
email and password. Then open **Settings**, **Administration**, and add people one by one or import a roster from a CSV
file. Each person gets a temporary password that is shown once, and chooses their own at the first sign in.

## Notes

- Run step 2 again with another name, slug and administrator to host a second organization on the same server. Each
  organization only sees its own data.
- A person can belong to several organizations. The Administration page shows the first organization they administer.
- The database password and the `service_role` key are not needed for any of this. Never type them into the app.
