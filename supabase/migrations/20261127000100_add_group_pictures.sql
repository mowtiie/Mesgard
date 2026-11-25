-- Group pictures and group name reports. A group picture lives in the avatars bucket under
-- <organization id>/groups/<group id>/<version>. Organization admins and the owners and moderators of a group set it,
-- and the organization picture policy applies: when it is off, no group has a picture. Members read only the current
-- version. A report about a group picture or a group name has no subject person, and a reviewer closing it can remove
-- the picture or rename the group; the owners of the group are told. api_level is 27.

alter table public.groups add column avatar_version integer check (avatar_version > 0);

alter table public.reports alter column subject_user_id drop not null;
alter table public.reports add constraint reports_subject_user_check check (
  (subject in ('group_picture', 'group_name')) = (subject_user_id is null)
);

alter table public.profile_notices drop constraint profile_notices_kind_check;
alter table public.profile_notices add constraint profile_notices_kind_check
  check (kind in ('username_reset', 'picture_removed', 'group_picture_removed', 'group_renamed'));

alter table public.report_events drop constraint report_events_action_check;
alter table public.report_events add constraint report_events_action_check check (
  action in (
    'submitted', 'viewed', 'status_changed', 'message_removed', 'reporter_revealed', 'username_reset',
    'picture_removed', 'group_renamed'
  )
);

-- Object names are <organization id>/groups/<group id>/<version>.

create function private.group_picture_org(object_name text)
returns uuid
language sql
immutable
set search_path = ''
as $$
  select case
    when object_name ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/groups/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/[0-9]{1,9}$'
      then split_part(object_name, '/', 1)::uuid
  end;
$$;

create function private.group_picture_group(object_name text)
returns uuid
language sql
immutable
set search_path = ''
as $$
  select case when private.group_picture_org(object_name) is not null then split_part(object_name, '/', 3)::uuid end;
$$;

create function private.group_picture_version(object_name text)
returns integer
language sql
immutable
set search_path = ''
as $$
  select case when private.group_picture_org(object_name) is not null then split_part(object_name, '/', 4)::integer end;
$$;

create function private.can_manage_group(target_group_id uuid, target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.groups as grp
    where grp.id = target_group_id
      and (
        exists (
          select 1 from public.org_members as member
          where member.org_id = grp.org_id and member.user_id = target_user_id and member.role = 'org_admin'
        )
        or exists (
          select 1 from public.group_members as membership
          where membership.group_id = grp.id and membership.user_id = target_user_id
            and membership.role_in_group in ('owner', 'moderator')
        )
      )
  );
$$;

create function private.can_set_group_picture(target_group_id uuid, target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select private.can_manage_group(target_group_id, target_user_id)
    and exists (
      select 1
      from public.groups as grp
      join public.organizations as organization on organization.id = grp.org_id
      where grp.id = target_group_id and organization.profile_picture_policy <> 'off'
    );
$$;

create function private.can_manage_group_picture_object(object_name text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.groups as grp
    where grp.id = private.group_picture_group(object_name) and grp.org_id = private.group_picture_org(object_name)
  ) and private.can_set_group_picture(private.group_picture_group(object_name), (select auth.uid()));
$$;

create function private.can_view_group_picture(object_name text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select private.is_org_member(private.group_picture_org(object_name))
    and exists (
      select 1
      from public.groups as grp
      join public.organizations as organization on organization.id = grp.org_id
      where grp.id = private.group_picture_group(object_name)
        and grp.org_id = private.group_picture_org(object_name)
        and grp.avatar_version = private.group_picture_version(object_name)
        and organization.profile_picture_policy <> 'off'
    );
$$;

create policy "Group managers can upload a group picture"
  on storage.objects for insert to authenticated
  with check (bucket_id = 'avatars' and private.can_manage_group_picture_object(name));

create policy "Members can read the current group pictures of their organization"
  on storage.objects for select to authenticated
  using (bucket_id = 'avatars' and private.can_view_group_picture(name));

create policy "Group managers can read every version of their group picture"
  on storage.objects for select to authenticated
  using (bucket_id = 'avatars' and private.can_manage_group_picture_object(name));

create policy "Group managers can delete group pictures unless a report keeps them"
  on storage.objects for delete to authenticated
  using (
    bucket_id = 'avatars'
    and private.can_manage_group_picture_object(name)
    and not private.avatar_is_evidence(name)
  );

create function public.set_group_picture(target_group_id uuid, new_version integer)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  group_row public.groups;
begin
  perform private.require_mfa();
  select * into group_row from public.groups where id = target_group_id;
  if not found or not private.is_org_member(group_row.org_id) then
    raise exception 'Group not found' using errcode = 'P0002';
  end if;
  if not private.can_set_group_picture(target_group_id, caller_id) then
    raise exception 'You cannot set the picture of this group' using errcode = '42501';
  end if;
  if new_version is null or new_version <= coalesce(group_row.avatar_version, 0) then
    raise exception 'The picture version must be newer' using errcode = '22023';
  end if;
  if not exists (
    select 1 from storage.objects
    where bucket_id = 'avatars'
      and name = group_row.org_id::text || '/groups/' || target_group_id::text || '/' || new_version::text
      and owner_id = caller_id::text
  ) then
    raise exception 'The picture has not been uploaded' using errcode = '22023';
  end if;

  update public.groups set avatar_version = new_version where id = target_group_id;
end;
$$;

create function public.remove_group_picture(target_group_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.can_manage_group(target_group_id, (select auth.uid())) then
    raise exception 'You cannot change the picture of this group' using errcode = '42501';
  end if;
  update public.groups set avatar_version = null where id = target_group_id;
end;
$$;

create function public.get_group_picture_rules(target_group_id uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  group_row public.groups;
  chosen_policy text;
begin
  perform private.require_mfa();
  select * into group_row from public.groups where id = target_group_id;
  if not found or not private.is_org_member(group_row.org_id) then
    raise exception 'Group not found' using errcode = 'P0002';
  end if;
  select profile_picture_policy into chosen_policy from public.organizations where id = group_row.org_id;
  return jsonb_build_object(
    'org_id', group_row.org_id,
    'name', group_row.name,
    'policy', chosen_policy,
    'can_set', private.can_set_group_picture(target_group_id, (select auth.uid())),
    'can_remove', private.can_manage_group(target_group_id, (select auth.uid())),
    'version', group_row.avatar_version
  );
end;
$$;

create function private.freeze_group_picture(target_report_id uuid, target_group_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  group_row public.groups;
  object_path text;
  object_row storage.objects;
begin
  select * into group_row from public.groups where id = target_group_id;
  if group_row.avatar_version is null then
    raise exception 'This group has no picture' using errcode = '22023';
  end if;
  object_path := group_row.org_id::text || '/groups/' || target_group_id::text || '/' || group_row.avatar_version::text;
  select * into object_row from storage.objects where bucket_id = 'avatars' and name = object_path;
  if not found then
    raise exception 'This group has no picture' using errcode = '22023';
  end if;

  insert into public.report_evidence (report_id, subject_text) values (target_report_id, 'Group picture');
  insert into public.report_evidence_files (report_id, storage_path, file_name, mime_type, size_bytes)
  values (
    target_report_id, object_path, 'group-picture',
    coalesce(object_row.metadata ->> 'mimetype', 'image/jpeg'), coalesce((object_row.metadata ->> 'size')::bigint, 1)
  );
end;
$$;

drop function public.submit_report(uuid, uuid, text, uuid, uuid, text, boolean, text);

create function public.submit_report(
  new_report_id uuid,
  target_org_id uuid,
  report_category text,
  target_message_id uuid default null,
  target_user_id uuid default null,
  report_note text default null,
  report_anonymous boolean default false,
  report_subject text default null,
  target_group_id uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  reporter uuid := (select auth.uid());
  chosen_subject text := coalesce(report_subject, case when target_message_id is null then 'person' else 'message' end);
  is_group_subject boolean := chosen_subject in ('group_picture', 'group_name');
  subject uuid;
  message_row public.messages;
  conversation_row public.conversations;
  group_row public.groups;
  existing_reporter uuid;
begin
  perform private.require_mfa();
  if reporter is null or not exists (
    select 1 from public.org_members where org_id = target_org_id and user_id = reporter
  ) then
    raise exception 'Not allowed to report in this organization' using errcode = '42501';
  end if;
  if chosen_subject not in ('message', 'person', 'name', 'username', 'profile_picture', 'group_picture', 'group_name') then
    raise exception 'This cannot be reported yet' using errcode = '22023';
  end if;
  if is_group_subject then
    if target_group_id is null or target_message_id is not null or target_user_id is not null then
      raise exception 'Report a group' using errcode = '22023';
    end if;
  elsif target_group_id is not null
    or (target_message_id is null) = (target_user_id is null)
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

  if is_group_subject then
    select * into group_row from public.groups where id = target_group_id and org_id = target_org_id;
    if not found then
      raise exception 'Group not found' using errcode = 'P0002';
    end if;
    if exists (
      select 1 from public.group_members
      where group_id = target_group_id and user_id = reporter and role_in_group = 'owner'
    ) then
      raise exception 'You cannot report a group you own' using errcode = '22023';
    end if;
  elsif target_message_id is not null then
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
    id, org_id, reporter_id, anonymous, category, subject, subject_user_id, target_group_id, message_id,
    conversation_id, note
  )
  values (
    new_report_id, target_org_id, reporter, coalesce(report_anonymous, false), report_category, chosen_subject, subject,
    group_row.id, message_row.id, message_row.conversation_id, nullif(trim(report_note), '')
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
  elsif chosen_subject = 'group_name' then
    insert into public.report_evidence (report_id, subject_text) values (new_report_id, group_row.name);
  elsif chosen_subject = 'group_picture' then
    perform private.freeze_group_picture(new_report_id, group_row.id);
  end if;

  insert into public.report_events (report_id, org_id, actor_id, action)
  values (new_report_id, target_org_id, case when coalesce(report_anonymous, false) then null else reporter end, 'submitted');

  return new_report_id;
end;
$$;

create or replace function public.list_reports(
  target_org_id uuid,
  status_filter text default null,
  page_size integer default 30,
  before_created_at timestamptz default null,
  before_report_id uuid default null
)
returns table (
  report_id uuid,
  category text,
  status text,
  subject text,
  subject_user_id uuid,
  subject_name text,
  message_id uuid,
  anonymous boolean,
  reporter_name text,
  created_at timestamptz
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.is_report_reviewer(target_org_id) then
    raise exception 'Not allowed to read reports' using errcode = '42501';
  end if;
  return query
  select
    report.id, report.category, report.status, report.subject, report.subject_user_id,
    coalesce(subject_profile.display_name, group_row.name),
    report.message_id, report.anonymous, case when report.anonymous then null else reporter.display_name end,
    report.created_at
  from public.reports as report
  left join public.profiles as subject_profile on subject_profile.id = report.subject_user_id
  left join public.groups as group_row on group_row.id = report.target_group_id
  join public.profiles as reporter on reporter.id = report.reporter_id
  where report.org_id = target_org_id
    and (status_filter is null or report.status = status_filter)
    and (before_created_at is null or (report.created_at, report.id) < (before_created_at, before_report_id))
  order by report.created_at desc, report.id desc
  limit least(greatest(page_size, 1), 50);
end;
$$;

create or replace function public.get_report(target_report_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  report_row public.reports;
  result jsonb;
begin
  perform private.require_mfa();
  select * into report_row from public.reports where id = target_report_id;
  if not found or not private.is_report_reviewer(report_row.org_id) then
    raise exception 'Not allowed to read this report' using errcode = '42501';
  end if;

  insert into public.report_events (report_id, org_id, actor_id, action)
  values (report_row.id, report_row.org_id, (select auth.uid()), 'viewed');

  select jsonb_build_object(
    'id', report_row.id,
    'category', report_row.category,
    'subject', report_row.subject,
    'status', report_row.status,
    'note', report_row.note,
    'anonymous', report_row.anonymous,
    'reporter_name', case when report_row.anonymous then null else reporter.display_name end,
    'subject_user_id', report_row.subject_user_id,
    'subject_name', coalesce(subject_profile.display_name, group_row.name),
    'subject_text', (
      select evidence.subject_text from public.report_evidence as evidence where evidence.report_id = report_row.id
    ),
    'message_removed', exists (
      select 1 from public.report_events where report_id = report_row.id and action = 'message_removed'
    ),
    'resolution_note', report_row.resolution_note,
    'created_at', report_row.created_at,
    'resolved_at', report_row.resolved_at,
    'evidence', (
      select to_jsonb(evidence) - 'report_id' - 'subject_text' from public.report_evidence as evidence
      where evidence.report_id = report_row.id and evidence.message_id is not null
    ),
    'files', coalesce((
      select jsonb_agg(
        jsonb_build_object(
          'id', file.id, 'storage_path', file.storage_path, 'file_name', file.file_name,
          'mime_type', file.mime_type, 'size_bytes', file.size_bytes
        )
        order by file.file_name, file.id
      )
      from public.report_evidence_files as file where file.report_id = report_row.id
    ), '[]'::jsonb),
    'events', coalesce((
      select jsonb_agg(
        jsonb_build_object(
          'action', event.action, 'note', event.note, 'actor_name', actor.display_name, 'created_at', event.created_at
        )
        order by event.created_at, event.id
      )
      from public.report_events as event
      left join public.profiles as actor on actor.id = event.actor_id
      where event.report_id = report_row.id
    ), '[]'::jsonb)
  )
  into result
  from public.profiles as reporter
  left join public.profiles as subject_profile on subject_profile.id = report_row.subject_user_id
  left join public.groups as group_row on group_row.id = report_row.target_group_id
  where reporter.id = report_row.reporter_id;

  return result;
end;
$$;

drop function public.update_report(uuid, text, text, boolean, boolean, boolean, integer);

create function public.update_report(
  target_report_id uuid,
  new_status text,
  action_note text default null,
  remove_message boolean default false,
  reset_username boolean default false,
  remove_picture boolean default false,
  block_days integer default null,
  rename_group_to text default null
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
  trimmed_name text := nullif(trim(rename_group_to), '');
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
  if (remove_picture or block_days is not null)
    and (report_row.subject not in ('profile_picture', 'group_picture') or not final_status) then
    raise exception 'A picture is removed when a report about it is closed' using errcode = '22023';
  end if;
  if block_days is not null
    and (report_row.subject <> 'profile_picture' or not remove_picture or block_days not in (7, 30)) then
    raise exception 'Uploads are blocked for 7 or 30 days, together with removing a profile picture' using errcode = '22023';
  end if;
  if rename_group_to is not null and (report_row.subject <> 'group_name' or not final_status) then
    raise exception 'A group is renamed when a report about its name is closed' using errcode = '22023';
  end if;
  if rename_group_to is not null and char_length(coalesce(trimmed_name, '')) not between 1 and 200 then
    raise exception 'The group name must have 1 to 200 characters' using errcode = '22023';
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

  if remove_picture and report_row.subject = 'profile_picture' then
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

  if remove_picture and report_row.subject = 'group_picture' then
    update public.groups set avatar_version = null where id = report_row.target_group_id;
    perform private.record_audit_event(
      report_row.org_id, 'group.picture_removed', 'group', report_row.target_group_id, '{}'::jsonb
    );
    insert into public.profile_notices (user_id, org_id, kind)
    select membership.user_id, report_row.org_id, 'group_picture_removed'
    from public.group_members as membership
    where membership.group_id = report_row.target_group_id and membership.role_in_group = 'owner';
    insert into public.report_events (report_id, org_id, actor_id, action)
    values (report_row.id, report_row.org_id, actor, 'picture_removed');
  end if;

  if rename_group_to is not null then
    update public.groups set name = trimmed_name where id = report_row.target_group_id;
    update public.conversations set title = trimmed_name
    where group_id = report_row.target_group_id and type = 'group';
    insert into public.profile_notices (user_id, org_id, kind)
    select membership.user_id, report_row.org_id, 'group_renamed'
    from public.group_members as membership
    where membership.group_id = report_row.target_group_id and membership.role_in_group = 'owner';
    insert into public.report_events (report_id, org_id, actor_id, action)
    values (report_row.id, report_row.org_id, actor, 'group_renamed');
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
  public.set_group_picture(uuid, integer),
  public.remove_group_picture(uuid),
  public.get_group_picture_rules(uuid),
  public.submit_report(uuid, uuid, text, uuid, uuid, text, boolean, text, uuid),
  public.update_report(uuid, text, text, boolean, boolean, boolean, integer, text)
from public, anon;
grant execute on function
  public.set_group_picture(uuid, integer),
  public.remove_group_picture(uuid),
  public.get_group_picture_rules(uuid),
  public.submit_report(uuid, uuid, text, uuid, uuid, text, boolean, text, uuid),
  public.update_report(uuid, text, text, boolean, boolean, boolean, integer, text)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 27);
$$;
