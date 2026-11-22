-- Break-glass access: administrators cannot read the messages of a group a person created. When there is a serious reason
-- (a report, an allegation) an organization admin can open that group's messages for seven days, after writing the
-- reason. Every opening is recorded in the audit log with the reason, and the members of the group can see that an
-- administrator opened it and when (not who and not why, which could point at the person who reported). Only read
-- access: nothing can be changed, and files are not included. api_level is 21.

create table public.group_access_grants (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations (id) on delete cascade,
  group_id uuid not null references public.groups (id) on delete cascade,
  granted_to uuid not null references public.profiles (id),
  reason text not null check (char_length(reason) between 10 and 500),
  created_at timestamptz not null default now(),
  expires_at timestamptz not null
);

create index group_access_grants_group_id_expires_at_idx on public.group_access_grants (group_id, expires_at desc);

alter table public.group_access_grants enable row level security;
revoke all on public.group_access_grants from anon, authenticated;

create policy "Require two-step verification" on public.group_access_grants as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));

create function private.group_access_days()
returns integer
language sql
immutable
set search_path = ''
as $$
  select 7;
$$;

create function public.open_group_messages(target_group_id uuid, access_reason text)
returns timestamptz
language plpgsql
security definer
set search_path = ''
as $$
declare
  target_group public.groups%rowtype;
  trimmed_reason text := btrim(coalesce(access_reason, ''));
  grant_expires_at timestamptz := now() + make_interval(days => private.group_access_days());
begin
  perform private.require_mfa();
  select * into target_group from public.groups where id = target_group_id;
  if not found then
    raise exception 'The group does not exist' using errcode = 'no_data_found';
  end if;
  if not private.has_org_role(target_group.org_id, array['org_admin']) then
    raise exception 'Only an organization admin can open the messages of a group' using errcode = 'insufficient_privilege';
  end if;
  if not target_group.user_created then
    raise exception 'Only the messages of a group a person created need this' using errcode = 'invalid_parameter_value';
  end if;
  if char_length(trimmed_reason) < 10 or char_length(trimmed_reason) > 500 then
    raise exception 'Give a reason of 10 to 500 characters' using errcode = 'invalid_parameter_value';
  end if;
  insert into public.group_access_grants (org_id, group_id, granted_to, reason, expires_at)
  values (target_group.org_id, target_group_id, (select auth.uid()), trimmed_reason, grant_expires_at);
  perform private.record_audit_event(
    target_group.org_id, 'group.messages_opened', 'group', target_group_id,
    jsonb_build_object('reason', trimmed_reason, 'expires_at', grant_expires_at)
  );
  return grant_expires_at;
end;
$$;

create function public.read_group_messages(
  target_group_id uuid,
  page_size integer default 50,
  before_created_at timestamptz default null,
  before_message_id uuid default null
)
returns table (
  message_id uuid,
  sender_id uuid,
  sender_name text,
  body text,
  created_at timestamptz,
  edited_at timestamptz,
  deleted_at timestamptz,
  has_attachments boolean
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not exists (
    select 1 from public.group_access_grants as access
    where access.group_id = target_group_id
      and access.granted_to = (select auth.uid())
      and access.expires_at > now()
  ) then
    raise exception 'Open the messages of this group first, with a reason' using errcode = 'insufficient_privilege';
  end if;
  return query
  select
    message.id,
    message.sender_id,
    profile.display_name,
    case when message.deleted_at is null then message.body end,
    message.created_at,
    message.edited_at,
    message.deleted_at,
    message.has_attachments
  from public.messages as message
  join public.conversations as conversation on conversation.id = message.conversation_id
  left join public.profiles as profile on profile.id = message.sender_id
  where conversation.group_id = target_group_id
    and (before_created_at is null or (message.created_at, message.id) < (before_created_at, before_message_id))
  order by message.created_at desc, message.id desc
  limit least(greatest(page_size, 1), 100);
end;
$$;

create function public.list_group_access_notices(target_group_id uuid)
returns table (opened_at timestamptz)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
begin
  perform private.require_mfa();
  if caller_id is null or not exists (
    select 1 from public.group_members where group_id = target_group_id and user_id = caller_id
  ) then
    raise exception 'Only members of the group can see this' using errcode = 'insufficient_privilege';
  end if;
  return query
  select access.created_at
  from public.group_access_grants as access
  where access.group_id = target_group_id and access.created_at > now() - interval '30 days'
  order by access.created_at desc
  limit 5;
end;
$$;

revoke execute on function
  public.open_group_messages(uuid, text),
  public.read_group_messages(uuid, integer, timestamptz, uuid),
  public.list_group_access_notices(uuid)
  from public, anon;
grant execute on function
  public.open_group_messages(uuid, text),
  public.read_group_messages(uuid, integer, timestamptz, uuid),
  public.list_group_access_notices(uuid)
  to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 21);
$$;
