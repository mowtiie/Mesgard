-- Legal hold: while a case or a legal order is open, what an organization holds about a person or a group is not
-- erased or deleted (RA 10175 section 13, RA 11930). An organization admin places a hold on a person, on a group or on
-- the whole organization and releases it with a note. An open report protects the person reported, the reporter and the
-- group of the reported message in the same way, so nothing has to be done by hand. The guards run in the database, so
-- no client can go around them. api_level is 10.

create table public.legal_holds (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations (id),
  user_id uuid references public.profiles (id),
  group_id uuid,
  reason text not null check (char_length(reason) between 10 and 500),
  placed_by uuid not null references public.profiles (id),
  placed_at timestamptz not null default now(),
  released_by uuid references public.profiles (id),
  released_at timestamptz,
  release_note text check (char_length(release_note) <= 500),
  check (user_id is null or group_id is null),
  check ((released_at is null) = (released_by is null))
);

create index legal_holds_org_id_placed_at_idx on public.legal_holds (org_id, placed_at desc, id desc);
create index legal_holds_active_user_id_idx on public.legal_holds (user_id) where released_at is null;
create index legal_holds_active_group_id_idx on public.legal_holds (group_id) where released_at is null;

alter table public.legal_holds enable row level security;
revoke all on public.legal_holds from anon, authenticated;

create policy "Require two-step verification" on public.legal_holds as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));

create function private.legal_hold_blocks_user(target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.legal_holds as hold
    where hold.released_at is null
      and (
        hold.user_id = target_user_id
        or (
          hold.user_id is null and hold.group_id is null
          and (
            exists (
              select 1 from public.org_members as member
              where member.org_id = hold.org_id and member.user_id = target_user_id
            )
            or exists (
              select 1 from public.messages as message
              where message.org_id = hold.org_id and message.sender_id = target_user_id
            )
          )
        )
      )
  ) or exists (
    select 1
    from public.reports as report
    where report.status in ('open', 'in_review')
      and (report.subject_user_id = target_user_id or report.reporter_id = target_user_id)
  );
$$;

create function private.legal_hold_blocks_group(target_group_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.legal_holds as hold
    join public.groups as target_group on target_group.org_id = hold.org_id
    where target_group.id = target_group_id
      and hold.released_at is null
      and (hold.group_id = target_group_id or (hold.user_id is null and hold.group_id is null))
  ) or exists (
    select 1
    from public.reports as report
    join public.conversations as conversation on conversation.id = report.conversation_id
    where conversation.group_id = target_group_id
      and report.status in ('open', 'in_review')
  );
$$;

create function private.reject_group_delete_under_hold()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if private.legal_hold_blocks_group(old.id) then
    raise exception 'A legal hold or an open report protects this group' using errcode = 'foreign_key_violation';
  end if;
  return old;
end;
$$;

create trigger groups_reject_delete_under_hold
  before delete on public.groups
  for each row execute function private.reject_group_delete_under_hold();

create or replace function public.list_user_attachment_paths(target_user_id uuid)
returns setof text
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if private.legal_hold_blocks_user(target_user_id) then
    raise exception 'A legal hold or an open report protects this person' using errcode = 'foreign_key_violation';
  end if;
  return query
  select attachment.storage_path
  from public.attachments as attachment
  join public.messages as message on message.id = attachment.message_id and message.org_id = attachment.org_id
  where message.sender_id = target_user_id
  union
  select object.name
  from storage.objects as object
  where object.bucket_id = 'message-attachments' and object.owner_id = target_user_id::text;
end;
$$;

revoke execute on function public.list_user_attachment_paths(uuid) from public, anon, authenticated;
grant execute on function public.list_user_attachment_paths(uuid) to service_role;

create function public.legal_hold_blocks_erasure(target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select private.legal_hold_blocks_user(target_user_id);
$$;

revoke execute on function public.legal_hold_blocks_erasure(uuid) from public, anon, authenticated;
grant execute on function public.legal_hold_blocks_erasure(uuid) to service_role;

create or replace function public.erase_user_data(target_user_id uuid, old_email text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  erasure_mode text := (select mode from private.erasure_settings);
  old_name text;
  placeholder_name constant text := 'Former member';
  placeholder_email text;
begin
  if erasure_mode = 'off' then
    raise exception 'Erasure is turned off on this server' using errcode = 'insufficient_privilege';
  end if;
  if private.legal_hold_blocks_user(target_user_id) then
    raise exception 'A legal hold or an open report protects this person' using errcode = 'foreign_key_violation';
  end if;
  if exists (select 1 from public.org_members where user_id = target_user_id) then
    raise exception 'Remove the person from every organization first' using errcode = 'insufficient_privilege';
  end if;

  select display_name into old_name from public.profiles where id = target_user_id;
  placeholder_email := 'erased-' || target_user_id || '@erased.invalid';

  update public.profiles
  set display_name = placeholder_name, avatar_path = null, status = null
  where id = target_user_id;
  delete from public.push_subscriptions where user_id = target_user_id;
  delete from public.user_settings where user_id = target_user_id;

  update auth.users set raw_user_meta_data = '{}'::jsonb, phone = null where id = target_user_id;
  delete from auth.sessions where user_id = target_user_id;
  delete from auth.mfa_factors where user_id = target_user_id;
  delete from auth.one_time_tokens where user_id = target_user_id;

  -- Auth's own log keeps the email, the name and the address of the device. Only the rows about this person change.
  update auth.audit_log_entries
  set
    payload = replace(
      replace(payload::text, lower(old_email), placeholder_email),
      coalesce(old_name, placeholder_name),
      placeholder_name
    )::json,
    ip_address = ''
  where payload::text ilike '%' || old_email || '%' or payload::text like '%' || target_user_id || '%';

  if erasure_mode = 'blank_messages' then
    -- Deleting a message archives its text for administrators, so the archive is purged for every message of the person.
    update public.messages set deleted_at = now() where sender_id = target_user_id and deleted_at is null;
    delete from public.deleted_message_bodies
    where message_id in (select id from public.messages where sender_id = target_user_id);
    delete from public.attachments
    where message_id in (select id from public.messages where sender_id = target_user_id);
  end if;
end;
$$;

-- Placing, listing and releasing holds (organization admins only).

create function public.place_legal_hold(
  target_org_id uuid,
  hold_reason text,
  target_user_id uuid default null,
  target_group_id uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  actor uuid := (select auth.uid());
  trimmed_reason text := trim(hold_reason);
  new_hold_id uuid;
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can place a legal hold' using errcode = '42501';
  end if;
  if char_length(coalesce(trimmed_reason, '')) < 10 then
    raise exception 'A legal hold needs a reason of at least 10 characters' using errcode = '22023';
  end if;
  if target_user_id is not null and target_group_id is not null then
    raise exception 'A hold covers a person or a group, not both' using errcode = '22023';
  end if;
  if target_user_id is not null and not (
    exists (select 1 from public.org_members where org_id = target_org_id and user_id = target_user_id)
    or exists (select 1 from public.messages where org_id = target_org_id and sender_id = target_user_id)
  ) then
    raise exception 'Person not found in this organization' using errcode = '22023';
  end if;
  if target_group_id is not null and not exists (
    select 1 from public.groups where id = target_group_id and org_id = target_org_id
  ) then
    raise exception 'Group not found in this organization' using errcode = '22023';
  end if;

  insert into public.legal_holds (org_id, user_id, group_id, reason, placed_by)
  values (target_org_id, target_user_id, target_group_id, left(trimmed_reason, 500), actor)
  returning id into new_hold_id;

  perform private.record_audit_event(
    target_org_id,
    'legal_hold.placed',
    case when target_user_id is not null then 'user' when target_group_id is not null then 'group' else 'organization' end,
    coalesce(target_user_id, target_group_id, target_org_id),
    jsonb_build_object('hold_id', new_hold_id)
  );
  return new_hold_id;
end;
$$;

create function public.release_legal_hold(target_hold_id uuid, note text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  actor uuid := (select auth.uid());
  hold_row public.legal_holds;
  trimmed_note text := trim(note);
begin
  perform private.require_mfa();
  select * into hold_row from public.legal_holds where id = target_hold_id for update;
  if not found or not private.has_org_role(hold_row.org_id, array['org_admin']) then
    raise exception 'Only an organization admin can release a legal hold' using errcode = '42501';
  end if;
  if hold_row.released_at is not null then
    raise exception 'This legal hold is already released' using errcode = '22023';
  end if;
  if char_length(coalesce(trimmed_note, '')) < 3 then
    raise exception 'Releasing a legal hold needs a note' using errcode = '22023';
  end if;

  update public.legal_holds
  set released_at = now(), released_by = actor, release_note = left(trimmed_note, 500)
  where id = target_hold_id;

  perform private.record_audit_event(
    hold_row.org_id,
    'legal_hold.released',
    case when hold_row.user_id is not null then 'user' when hold_row.group_id is not null then 'group' else 'organization' end,
    coalesce(hold_row.user_id, hold_row.group_id, hold_row.org_id),
    jsonb_build_object('hold_id', hold_row.id)
  );
end;
$$;

create function public.list_legal_holds(target_org_id uuid, include_released boolean default false)
returns table (
  hold_id uuid,
  scope text,
  user_id uuid,
  user_name text,
  group_id uuid,
  group_name text,
  reason text,
  placed_by_name text,
  placed_at timestamptz,
  released_at timestamptz,
  release_note text
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can read legal holds' using errcode = '42501';
  end if;
  return query
  select
    hold.id,
    case when hold.user_id is not null then 'person' when hold.group_id is not null then 'group' else 'organization' end,
    hold.user_id,
    person.display_name,
    hold.group_id,
    target_group.name,
    hold.reason,
    placer.display_name,
    hold.placed_at,
    hold.released_at,
    hold.release_note
  from public.legal_holds as hold
  join public.profiles as placer on placer.id = hold.placed_by
  left join public.profiles as person on person.id = hold.user_id
  left join public.groups as target_group on target_group.id = hold.group_id
  where hold.org_id = target_org_id and (include_released or hold.released_at is null)
  order by hold.placed_at desc, hold.id desc
  limit 100;
end;
$$;

revoke execute on function
  public.place_legal_hold(uuid, text, uuid, uuid),
  public.release_legal_hold(uuid, text),
  public.list_legal_holds(uuid, boolean)
from public, anon;
grant execute on function
  public.place_legal_hold(uuid, text, uuid, uuid),
  public.release_legal_hold(uuid, text),
  public.list_legal_holds(uuid, boolean)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 10);
$$;
