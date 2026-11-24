-- Profile pictures: a private bucket, one picture per membership, shown to members of the same organization. An
-- organization chooses who may have one (off, staff and administrators, everyone), and students are off by default.
-- A picture is reported like any other subject: the object stays where it is, protected from deletion while a report
-- keeps it as evidence, and only reviewers can read an object that is no longer the current picture. A reviewer closing
-- the report can remove the picture and block new uploads for a number of days. api_level is 26.

alter table public.org_members add column avatar_version integer check (avatar_version > 0);
alter table public.org_members add column avatar_blocked_until timestamptz;

alter table public.organizations add column profile_picture_policy text not null default 'staff_and_admins'
  check (profile_picture_policy in ('off', 'staff_and_admins', 'everyone'));

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', false, 1048576, array['image/jpeg', 'image/webp'])
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

alter table public.profile_notices drop constraint profile_notices_kind_check;
alter table public.profile_notices add constraint profile_notices_kind_check
  check (kind in ('username_reset', 'picture_removed'));

alter table public.report_events drop constraint report_events_action_check;
alter table public.report_events add constraint report_events_action_check check (
  action in (
    'submitted', 'viewed', 'status_changed', 'message_removed', 'reporter_revealed', 'username_reset',
    'picture_removed'
  )
);

-- Object names are <organization id>/<user id>/<version>.

create function private.avatar_path_org(object_name text)
returns uuid
language sql
immutable
set search_path = ''
as $$
  select case
    when object_name ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/[0-9]{1,9}$'
      then split_part(object_name, '/', 1)::uuid
  end;
$$;

create function private.avatar_path_user(object_name text)
returns uuid
language sql
immutable
set search_path = ''
as $$
  select case when private.avatar_path_org(object_name) is not null then split_part(object_name, '/', 2)::uuid end;
$$;

create function private.avatar_path_version(object_name text)
returns integer
language sql
immutable
set search_path = ''
as $$
  select case when private.avatar_path_org(object_name) is not null then split_part(object_name, '/', 3)::integer end;
$$;

create function private.policy_allows_picture(target_org_id uuid, member_role text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select case organization.profile_picture_policy
    when 'everyone' then true
    when 'staff_and_admins' then member_role in ('org_admin', 'staff')
    else false
  end
  from public.organizations as organization
  where organization.id = target_org_id;
$$;

create function private.can_set_profile_picture(target_org_id uuid, target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(
    (
      select private.policy_allows_picture(target_org_id, member.role)
        and (member.avatar_blocked_until is null or member.avatar_blocked_until <= now())
      from public.org_members as member
      where member.org_id = target_org_id and member.user_id = target_user_id
    ),
    false
  );
$$;

create function private.can_view_profile_picture(object_name text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select private.is_org_member(private.avatar_path_org(object_name))
    and exists (
      select 1
      from public.org_members as member
      where member.org_id = private.avatar_path_org(object_name)
        and member.user_id = private.avatar_path_user(object_name)
        and member.avatar_version = private.avatar_path_version(object_name)
        and private.policy_allows_picture(member.org_id, member.role)
    );
$$;

create function private.avatar_is_evidence(object_name text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (select 1 from public.report_evidence_files where storage_path = object_name);
$$;

create policy "Members can upload their own profile picture"
  on storage.objects for insert to authenticated
  with check (
    bucket_id = 'avatars'
    and private.avatar_path_user(name) = (select auth.uid())
    and private.can_set_profile_picture(private.avatar_path_org(name), (select auth.uid()))
  );

create policy "Members can read profile pictures of their organization"
  on storage.objects for select to authenticated
  using (
    bucket_id = 'avatars'
    and (private.avatar_path_user(name) = (select auth.uid()) or private.can_view_profile_picture(name))
  );

create policy "Report reviewers can read the pictures kept as evidence"
  on storage.objects for select to authenticated
  using (bucket_id = 'avatars' and private.is_report_evidence_file_reviewer(name));

create policy "People can delete their own profile pictures unless a report keeps them"
  on storage.objects for delete to authenticated
  using (
    bucket_id = 'avatars'
    and private.avatar_path_user(name) = (select auth.uid())
    and not private.avatar_is_evidence(name)
  );

create function public.set_profile_picture(target_org_id uuid, new_version integer)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  current_version integer;
begin
  perform private.require_mfa();
  select avatar_version into current_version from public.org_members
  where org_id = target_org_id and user_id = caller_id;
  if not found then
    raise exception 'Only members of the organization can set a profile picture' using errcode = '42501';
  end if;
  if not private.can_set_profile_picture(target_org_id, caller_id) then
    raise exception 'Your organization does not allow you to set a profile picture' using errcode = '42501';
  end if;
  if new_version is null or new_version <= coalesce(current_version, 0) then
    raise exception 'The picture version must be newer' using errcode = '22023';
  end if;
  if not exists (
    select 1 from storage.objects
    where bucket_id = 'avatars'
      and name = target_org_id::text || '/' || caller_id::text || '/' || new_version::text
      and owner_id = caller_id::text
  ) then
    raise exception 'The picture has not been uploaded' using errcode = '22023';
  end if;

  update public.org_members set avatar_version = new_version where org_id = target_org_id and user_id = caller_id;
end;
$$;

create function public.remove_profile_picture(target_org_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  update public.org_members set avatar_version = null
  where org_id = target_org_id and user_id = (select auth.uid());
end;
$$;

create function public.get_profile_picture_rules(target_org_id uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  member_row public.org_members;
  chosen_policy text;
begin
  perform private.require_mfa();
  select * into member_row from public.org_members where org_id = target_org_id and user_id = (select auth.uid());
  if not found then
    raise exception 'Only members of the organization can ask' using errcode = '42501';
  end if;
  select profile_picture_policy into chosen_policy from public.organizations where id = target_org_id;
  return jsonb_build_object(
    'policy', chosen_policy,
    'can_set', private.can_set_profile_picture(target_org_id, member_row.user_id),
    'version', member_row.avatar_version,
    'blocked_until', case when member_row.avatar_blocked_until > now() then member_row.avatar_blocked_until end
  );
end;
$$;

create function public.set_profile_picture_policy(target_org_id uuid, new_policy text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can change who has a profile picture' using errcode = '42501';
  end if;
  if new_policy not in ('off', 'staff_and_admins', 'everyone') then
    raise exception 'Unknown policy' using errcode = '22023';
  end if;
  update public.organizations set profile_picture_policy = new_policy where id = target_org_id;
  perform private.record_audit_event(
    target_org_id, 'organization.profile_picture_policy_changed', 'organization', target_org_id,
    jsonb_build_object('policy', new_policy)
  );
end;
$$;

create function private.freeze_profile_picture(target_report_id uuid, target_org_id uuid, target_user_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  version integer;
  object_path text;
  object_row storage.objects;
begin
  select avatar_version into version from public.org_members
  where org_id = target_org_id and user_id = target_user_id;
  if version is null then
    raise exception 'This person has no profile picture' using errcode = '22023';
  end if;
  object_path := target_org_id::text || '/' || target_user_id::text || '/' || version::text;
  select * into object_row from storage.objects where bucket_id = 'avatars' and name = object_path;
  if not found then
    raise exception 'This person has no profile picture' using errcode = '22023';
  end if;

  insert into public.report_evidence (report_id, subject_text) values (target_report_id, 'Profile picture');
  insert into public.report_evidence_files (report_id, storage_path, file_name, mime_type, size_bytes)
  values (
    target_report_id, object_path, 'profile-picture',
    coalesce(object_row.metadata ->> 'mimetype', 'image/jpeg'), coalesce((object_row.metadata ->> 'size')::bigint, 1)
  );
end;
$$;

create or replace function public.submit_report(
  new_report_id uuid,
  target_org_id uuid,
  report_category text,
  target_message_id uuid default null,
  target_user_id uuid default null,
  report_note text default null,
  report_anonymous boolean default false,
  report_subject text default null
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  reporter uuid := (select auth.uid());
  chosen_subject text := coalesce(report_subject, case when target_message_id is null then 'person' else 'message' end);
  subject uuid;
  message_row public.messages;
  conversation_row public.conversations;
  existing_reporter uuid;
begin
  perform private.require_mfa();
  if reporter is null or not exists (
    select 1 from public.org_members where org_id = target_org_id and user_id = reporter
  ) then
    raise exception 'Not allowed to report in this organization' using errcode = '42501';
  end if;
  if chosen_subject not in ('message', 'person', 'name', 'username', 'profile_picture') then
    raise exception 'This cannot be reported yet' using errcode = '22023';
  end if;
  if (target_message_id is null) = (target_user_id is null)
    or (chosen_subject = 'message') <> (target_message_id is not null) then
    raise exception 'Report either a message or a person' using errcode = '22023';
  end if;

  select reporter_id into existing_reporter from public.reports where id = new_report_id;
  if found then
    if existing_reporter = reporter then
      return new_report_id;
    end if;
    raise exception 'Report id already used' using errcode = '23505';
  end if;

  if (select count(*) from public.reports where reporter_id = reporter and created_at > now() - interval '1 day') >= 20 then
    raise exception 'At most 20 reports a day' using errcode = '54000';
  end if;

  if target_message_id is not null then
    select * into message_row from public.messages where id = target_message_id;
    if not found or message_row.org_id <> target_org_id or message_row.deleted_at is not null
      or not private.can_read_message(target_message_id) then
      raise exception 'Message not found' using errcode = 'P0002';
    end if;
    subject := message_row.sender_id;
  else
    subject := target_user_id;
    if not exists (select 1 from public.org_members where org_id = target_org_id and user_id = subject) then
      raise exception 'Person not found' using errcode = 'P0002';
    end if;
  end if;
  if subject = reporter then
    raise exception 'You cannot report yourself' using errcode = '22023';
  end if;

  insert into public.reports (
    id, org_id, reporter_id, anonymous, category, subject, subject_user_id, message_id, conversation_id, note
  )
  values (
    new_report_id, target_org_id, reporter, coalesce(report_anonymous, false), report_category, chosen_subject, subject,
    message_row.id, message_row.conversation_id, nullif(trim(report_note), '')
  );

  if message_row.id is not null then
    select * into conversation_row from public.conversations where id = message_row.conversation_id;

    insert into public.report_evidence (
      report_id, message_id, conversation_id, conversation_title, sender_id, sender_name, body, sent_at, edited_at, context
    )
    select
      new_report_id, message_row.id, message_row.conversation_id,
      case when conversation_row.type = 'group' then conversation_row.title end,
      message_row.sender_id, sender.display_name, message_row.body, message_row.created_at, message_row.edited_at,
      coalesce((
        select jsonb_agg(
          jsonb_build_object('sender_name', earlier.sender_name, 'body', earlier.body, 'sent_at', earlier.created_at)
          order by earlier.created_at, earlier.id
        )
        from (
          select previous.id, previous.created_at, previous.body, author.display_name as sender_name
          from public.messages as previous
          join public.profiles as author on author.id = previous.sender_id
          where previous.conversation_id = message_row.conversation_id
            and previous.deleted_at is null
            and (previous.created_at, previous.id) < (message_row.created_at, message_row.id)
          order by previous.created_at desc, previous.id desc
          limit 5
        ) as earlier
      ), '[]'::jsonb)
    from public.profiles as sender
    where sender.id = message_row.sender_id;

    insert into public.report_evidence_files (report_id, storage_path, file_name, mime_type, size_bytes)
    select new_report_id, attachment.storage_path, attachment.file_name, attachment.mime_type, attachment.size_bytes
    from public.attachments as attachment
    where attachment.message_id = message_row.id;
  elsif chosen_subject = 'name' then
    insert into public.report_evidence (report_id, subject_text)
    select new_report_id, profile.display_name from public.profiles as profile where profile.id = subject;
  elsif chosen_subject = 'username' then
    insert into public.report_evidence (report_id, subject_text)
    select new_report_id, member.username
    from public.org_members as member
    where member.org_id = target_org_id and member.user_id = subject;
  elsif chosen_subject = 'profile_picture' then
    perform private.freeze_profile_picture(new_report_id, target_org_id, subject);
  end if;

  insert into public.report_events (report_id, org_id, actor_id, action)
  values (new_report_id, target_org_id, case when coalesce(report_anonymous, false) then null else reporter end, 'submitted');

  return new_report_id;
end;
$$;

drop function public.update_report(uuid, text, text, boolean, boolean);

create function public.update_report(
  target_report_id uuid,
  new_status text,
  action_note text default null,
  remove_message boolean default false,
  reset_username boolean default false,
  remove_picture boolean default false,
  block_days integer default null
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  actor uuid := (select auth.uid());
  report_row public.reports;
  final_status boolean := new_status in ('resolved', 'dismissed');
  trimmed_note text := nullif(trim(action_note), '');
begin
  perform private.require_mfa();
  select * into report_row from public.reports where id = target_report_id for update;
  if not found or not private.is_report_reviewer(report_row.org_id) then
    raise exception 'Not allowed to handle this report' using errcode = '42501';
  end if;
  if report_row.status in ('resolved', 'dismissed') then
    raise exception 'A closed report cannot be changed' using errcode = '22023';
  end if;
  if new_status not in ('in_review', 'resolved', 'dismissed') or new_status = report_row.status then
    raise exception 'Invalid report status' using errcode = '22023';
  end if;
  if final_status and char_length(coalesce(trimmed_note, '')) < 3 then
    raise exception 'Closing a report needs a note' using errcode = '22023';
  end if;
  if remove_message and report_row.message_id is null then
    raise exception 'This report has no message to remove' using errcode = '22023';
  end if;
  if reset_username and (report_row.subject <> 'username' or not final_status) then
    raise exception 'A username is reset when a report about it is closed' using errcode = '22023';
  end if;
  if (remove_picture or block_days is not null) and (report_row.subject <> 'profile_picture' or not final_status) then
    raise exception 'A picture is removed when a report about it is closed' using errcode = '22023';
  end if;
  if block_days is not null and (not remove_picture or block_days not in (7, 30)) then
    raise exception 'Uploads are blocked for 7 or 30 days, together with removing the picture' using errcode = '22023';
  end if;

  if remove_message then
    update public.messages set deleted_at = now() where id = report_row.message_id and deleted_at is null;
    update public.attachments set removed_at = now() where message_id = report_row.message_id and removed_at is null;
    insert into public.report_events (report_id, org_id, actor_id, action)
    values (report_row.id, report_row.org_id, actor, 'message_removed');
  end if;

  if reset_username then
    perform private.reset_username(report_row.org_id, report_row.subject_user_id);
    insert into public.profile_notices (user_id, org_id, kind)
    values (report_row.subject_user_id, report_row.org_id, 'username_reset');
    insert into public.report_events (report_id, org_id, actor_id, action)
    values (report_row.id, report_row.org_id, actor, 'username_reset');
  end if;

  if remove_picture then
    update public.org_members
    set avatar_version = null,
        avatar_blocked_until = case when block_days is not null then now() + make_interval(days => block_days) end
    where org_id = report_row.org_id and user_id = report_row.subject_user_id;
    perform private.record_audit_event(
      report_row.org_id, 'member.picture_removed', 'user', report_row.subject_user_id,
      jsonb_build_object('blocked_days', block_days)
    );
    insert into public.profile_notices (user_id, org_id, kind)
    values (report_row.subject_user_id, report_row.org_id, 'picture_removed');
    insert into public.report_events (report_id, org_id, actor_id, action)
    values (report_row.id, report_row.org_id, actor, 'picture_removed');
  end if;

  update public.reports
  set status = new_status,
      updated_at = now(),
      resolved_by = case when final_status then actor end,
      resolution_note = case when final_status then trimmed_note end,
      resolved_at = case when final_status then now() end
  where id = report_row.id;

  insert into public.report_events (report_id, org_id, actor_id, action, note)
  values (report_row.id, report_row.org_id, actor, 'status_changed', coalesce(trimmed_note, new_status));
end;
$$;

revoke execute on function
  public.set_profile_picture(uuid, integer),
  public.remove_profile_picture(uuid),
  public.get_profile_picture_rules(uuid),
  public.set_profile_picture_policy(uuid, text),
  public.update_report(uuid, text, text, boolean, boolean, boolean, integer)
from public, anon;
grant execute on function
  public.set_profile_picture(uuid, integer),
  public.remove_profile_picture(uuid),
  public.get_profile_picture_rules(uuid),
  public.set_profile_picture_policy(uuid, text),
  public.update_report(uuid, text, text, boolean, boolean, boolean, integer)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 26);
$$;
