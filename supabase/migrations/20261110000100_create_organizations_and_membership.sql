-- Organizations, profiles and organization membership, with Row Level Security.

create schema if not exists private;
grant usage on schema private to authenticated;

create function private.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create table public.org_roles (
  role text primary key
);

insert into public.org_roles (role) values ('org_admin'), ('staff'), ('student');

create table public.organizations (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(name) between 1 and 200),
  slug text not null unique check (slug ~ '^[a-z0-9-]+$'),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create trigger organizations_set_updated_at
  before update on public.organizations
  for each row execute function private.set_updated_at();

create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  display_name text not null check (char_length(display_name) between 1 and 100),
  avatar_path text,
  status text check (char_length(status) <= 140),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function private.set_updated_at();

create function private.create_profile_for_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name)
  values (
    new.id,
    coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), split_part(new.email, '@', 1))
  );
  return new;
end;
$$;

create trigger auth_users_create_profile
  after insert on auth.users
  for each row execute function private.create_profile_for_new_user();

create table public.org_members (
  org_id uuid not null references public.organizations (id) on delete cascade,
  user_id uuid not null references public.profiles (id) on delete cascade,
  role text not null default 'student' references public.org_roles (role),
  created_at timestamptz not null default now(),
  primary key (org_id, user_id)
);

create index org_members_user_id_idx on public.org_members (user_id);

create function private.is_org_member(target_org_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.org_members
    where org_id = target_org_id
      and user_id = (select auth.uid())
  );
$$;

create function private.has_org_role(target_org_id uuid, allowed_roles text[])
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.org_members
    where org_id = target_org_id
      and user_id = (select auth.uid())
      and role = any (allowed_roles)
  );
$$;

create function private.shares_org_with(other_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.org_members as mine
    join public.org_members as theirs on theirs.org_id = mine.org_id
    where mine.user_id = (select auth.uid())
      and theirs.user_id = other_user_id
  );
$$;

alter table public.org_roles enable row level security;
alter table public.organizations enable row level security;
alter table public.profiles enable row level security;
alter table public.org_members enable row level security;

revoke all on public.org_roles, public.organizations, public.profiles, public.org_members
  from anon, authenticated;

grant select on public.org_roles to authenticated;
grant select on public.organizations to authenticated;
grant update (name) on public.organizations to authenticated;
grant select on public.profiles to authenticated;
grant update (display_name, avatar_path, status) on public.profiles to authenticated;
grant select, delete on public.org_members to authenticated;
grant insert (org_id, user_id, role) on public.org_members to authenticated;
grant update (role) on public.org_members to authenticated;

create policy "Authenticated users can read organization roles"
  on public.org_roles for select to authenticated
  using (true);

create policy "Members can read their organizations"
  on public.organizations for select to authenticated
  using (private.is_org_member(id));

create policy "Organization admins can update their organization"
  on public.organizations for update to authenticated
  using (private.has_org_role(id, array['org_admin']))
  with check (private.has_org_role(id, array['org_admin']));

create policy "Users can read their own profile and profiles in shared organizations"
  on public.profiles for select to authenticated
  using (id = (select auth.uid()) or private.shares_org_with(id));

create policy "Users can update their own profile"
  on public.profiles for update to authenticated
  using (id = (select auth.uid()))
  with check (id = (select auth.uid()));

create policy "Members can read the roster of their organizations"
  on public.org_members for select to authenticated
  using (private.is_org_member(org_id));

create policy "Organization admins can add members"
  on public.org_members for insert to authenticated
  with check (private.has_org_role(org_id, array['org_admin']));

create policy "Organization admins can change member roles"
  on public.org_members for update to authenticated
  using (private.has_org_role(org_id, array['org_admin']))
  with check (private.has_org_role(org_id, array['org_admin']));

create policy "Organization admins can remove members"
  on public.org_members for delete to authenticated
  using (private.has_org_role(org_id, array['org_admin']));
