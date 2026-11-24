-- Usernames: each membership has a lowercase username, unique inside the organization, generated from the display name
-- when a member is added. A person changes their own at most once every 30 days when the organization policy allows it;
-- an organization admin can set or reset any. Names that were released are held back for 90 days so nobody takes one
-- to impersonate. api_level is 24.

create table private.reserved_usernames (
  username text primary key check (username = lower(username))
);

insert into private.reserved_usernames (username) values
  ('admin'), ('administrator'), ('staff'), ('teacher'), ('faculty'), ('student'), ('principal'), ('dean'),
  ('registrar'), ('school'), ('official'), ('mesgard'), ('support'), ('system'), ('moderator'), ('root'),
  ('owner'), ('help'), ('info'), ('security'), ('everyone'), ('everybody'), ('all'), ('here'), ('channel');

create table private.released_usernames (
  org_id uuid not null references public.organizations (id) on delete cascade,
  username text not null,
  user_id uuid not null,
  released_at timestamptz not null default now()
);

create index released_usernames_org_id_username_idx on private.released_usernames (org_id, username, released_at desc);

revoke all on private.reserved_usernames, private.released_usernames from public, anon, authenticated;

alter table public.org_members add column username text;
alter table public.org_members add column username_changed_at timestamptz;

alter table public.organizations add column username_policy text not null default 'everyone'
  check (username_policy in ('everyone', 'staff_and_admins', 'nobody'));

create function private.is_valid_username(candidate text)
returns boolean
language sql
immutable
set search_path = ''
as $$
  select coalesce(candidate ~ '^[a-z0-9]+([._][a-z0-9]+)*$' and char_length(candidate) between 3 and 20, false);
$$;

create function private.is_username_available(target_org_id uuid, candidate text, for_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select private.is_valid_username(candidate)
    and not exists (select 1 from private.reserved_usernames where username = candidate)
    and not exists (
      select 1 from public.org_members
      where org_id = target_org_id and username = candidate and user_id is distinct from for_user_id
    )
    and not exists (
      select 1 from private.released_usernames
      where org_id = target_org_id and username = candidate and user_id is distinct from for_user_id
        and released_at > now() - interval '90 days'
    );
$$;

create function private.suggest_username(target_org_id uuid, display_name text, for_user_id uuid)
returns text
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  base text := regexp_replace(lower(coalesce(display_name, '')), '[^a-z0-9]+', '.', 'g');
  candidate text;
  suffix integer := 1;
begin
  base := regexp_replace(base, '^\.+|\.+$', '', 'g');
  if char_length(base) < 3 then
    base := 'user';
  end if;
  base := regexp_replace(left(base, 15), '\.+$', '');
  candidate := base;
  while not private.is_username_available(target_org_id, candidate, for_user_id) loop
    suffix := suffix + 1;
    candidate := base || suffix::text;
  end loop;
  return candidate;
end;
$$;

-- The name of a reset carries nothing of the old one, because the old name may be the reason for the reset.
create function private.neutral_username(target_org_id uuid, for_user_id uuid)
returns text
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  candidate text;
begin
  loop
    candidate := 'user' || lpad((floor(random() * 1000000))::int::text, 6, '0');
    exit when private.is_username_available(target_org_id, candidate, for_user_id);
  end loop;
  return candidate;
end;
$$;

do $$
declare
  member record;
begin
  for member in
    select membership.org_id, membership.user_id, profile.display_name
    from public.org_members as membership
    join public.profiles as profile on profile.id = membership.user_id
    order by membership.created_at, membership.user_id
  loop
    update public.org_members
    set username = private.suggest_username(member.org_id, member.display_name, member.user_id)
    where org_id = member.org_id and user_id = member.user_id;
  end loop;
end;
$$;

alter table public.org_members alter column username set not null;
create unique index org_members_org_id_username_key on public.org_members (org_id, username);
create index org_members_org_id_username_pattern_idx on public.org_members (org_id, username text_pattern_ops);

create function private.prepare_org_member_username()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' and new.username is null then
    new.username := private.suggest_username(
      new.org_id, (select display_name from public.profiles where id = new.user_id), new.user_id
    );
    return new;
  end if;
  if tg_op = 'UPDATE' and new.username is not distinct from old.username then
    return new;
  end if;

  if not private.is_valid_username(new.username) then
    raise exception 'Invalid username' using errcode = '22023';
  end if;
  if exists (select 1 from private.reserved_usernames where username = new.username) then
    raise exception 'Username is reserved' using errcode = '22023';
  end if;
  if exists (
    select 1 from private.released_usernames
    where org_id = new.org_id and username = new.username and user_id <> new.user_id
      and released_at > now() - interval '90 days'
  ) then
    raise exception 'Username is held' using errcode = '23505';
  end if;
  return new;
end;
$$;

create trigger org_members_prepare_username
  before insert or update of username on public.org_members
  for each row execute function private.prepare_org_member_username();

create function private.keep_released_username()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into private.released_usernames (org_id, username, user_id) values (old.org_id, old.username, old.user_id);
  return null;
end;
$$;

create trigger org_members_keep_released_username_on_change
  after update of username on public.org_members
  for each row when (old.username is not null and old.username is distinct from new.username)
  execute function private.keep_released_username();
create trigger org_members_keep_released_username_on_delete
  after delete on public.org_members
  for each row execute function private.keep_released_username();

create function private.can_change_own_username(target_org_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select case organization.username_policy
    when 'everyone' then true
    when 'staff_and_admins' then private.has_org_role(target_org_id, array['org_admin', 'staff'])
    else false
  end
  from public.organizations as organization
  where organization.id = target_org_id;
$$;

create function public.set_username(target_org_id uuid, new_username text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  chosen text := lower(trim(coalesce(new_username, '')));
  current_row public.org_members;
begin
  perform private.require_mfa();
  select * into current_row from public.org_members where org_id = target_org_id and user_id = caller_id;
  if not found then
    raise exception 'Only members of the organization can change a username' using errcode = '42501';
  end if;
  if not private.can_change_own_username(target_org_id) then
    raise exception 'Your organization does not allow changing usernames' using errcode = '42501';
  end if;
  if current_row.username_changed_at is not null and current_row.username_changed_at > now() - interval '30 days' then
    raise exception 'A username can change once every 30 days' using errcode = '54000';
  end if;
  if chosen = current_row.username then
    return;
  end if;

  update public.org_members
  set username = chosen, username_changed_at = now()
  where org_id = target_org_id and user_id = caller_id;

  perform private.record_audit_event(
    target_org_id, 'member.username_changed', 'user', caller_id,
    jsonb_build_object('from', current_row.username, 'to', chosen)
  );
exception
  when unique_violation then
    raise exception 'Username is taken' using errcode = '23505';
end;
$$;

create function public.admin_set_username(target_org_id uuid, target_user_id uuid, new_username text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  chosen text := lower(trim(coalesce(new_username, '')));
  current_row public.org_members;
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can set a username' using errcode = '42501';
  end if;
  select * into current_row from public.org_members where org_id = target_org_id and user_id = target_user_id;
  if not found then
    raise exception 'Person not found' using errcode = 'P0002';
  end if;
  if chosen = current_row.username then
    return;
  end if;

  update public.org_members
  set username = chosen, username_changed_at = now()
  where org_id = target_org_id and user_id = target_user_id;

  perform private.record_audit_event(
    target_org_id, 'member.username_set', 'user', target_user_id,
    jsonb_build_object('from', current_row.username, 'to', chosen)
  );
exception
  when unique_violation then
    raise exception 'Username is taken' using errcode = '23505';
end;
$$;

create function private.reset_username(target_org_id uuid, target_user_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  old_username text;
  new_username text;
begin
  select username into old_username from public.org_members where org_id = target_org_id and user_id = target_user_id;
  if not found then
    raise exception 'Person not found' using errcode = 'P0002';
  end if;
  new_username := private.neutral_username(target_org_id, target_user_id);
  update public.org_members
  set username = new_username, username_changed_at = now()
  where org_id = target_org_id and user_id = target_user_id;
  perform private.record_audit_event(
    target_org_id, 'member.username_reset', 'user', target_user_id,
    jsonb_build_object('from', old_username, 'to', new_username)
  );
  return new_username;
end;
$$;

create function public.reset_username(target_org_id uuid, target_user_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can reset a username' using errcode = '42501';
  end if;
  return private.reset_username(target_org_id, target_user_id);
end;
$$;

create function public.get_username_rules(target_org_id uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  member_row public.org_members;
  chosen_policy text;
  next_change timestamptz;
begin
  perform private.require_mfa();
  select * into member_row from public.org_members where org_id = target_org_id and user_id = (select auth.uid());
  if not found then
    raise exception 'Only members of the organization can ask' using errcode = '42501';
  end if;
  select username_policy into chosen_policy from public.organizations where id = target_org_id;
  next_change := case
    when member_row.username_changed_at is null then null
    else member_row.username_changed_at + interval '30 days'
  end;
  return jsonb_build_object(
    'policy', chosen_policy,
    'username', member_row.username,
    'policy_allows', private.can_change_own_username(target_org_id),
    'next_change_at', case when next_change > now() then next_change end
  );
end;
$$;

create function public.set_username_policy(target_org_id uuid, new_policy text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can change who sets a username' using errcode = '42501';
  end if;
  if new_policy not in ('everyone', 'staff_and_admins', 'nobody') then
    raise exception 'Unknown policy' using errcode = '22023';
  end if;
  update public.organizations set username_policy = new_policy where id = target_org_id;
  perform private.record_audit_event(
    target_org_id, 'organization.username_policy_changed', 'organization', target_org_id,
    jsonb_build_object('policy', new_policy)
  );
end;
$$;

revoke execute on function
  public.set_username(uuid, text),
  public.admin_set_username(uuid, uuid, text),
  public.reset_username(uuid, uuid),
  public.get_username_rules(uuid),
  public.set_username_policy(uuid, text)
from public, anon;
grant execute on function
  public.set_username(uuid, text),
  public.admin_set_username(uuid, uuid, text),
  public.reset_username(uuid, uuid),
  public.get_username_rules(uuid),
  public.set_username_policy(uuid, text)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 24);
$$;
