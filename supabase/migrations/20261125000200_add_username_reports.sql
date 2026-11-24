-- A username can be reported. A report keeps a frozen copy of the username, and a reviewer can reset the username to a
-- neutral one while closing the report. The person gets a neutral notice the next time they open the app, with no
-- reporter and no report id. api_level is 25.

alter table public.report_events drop constraint report_events_action_check;
alter table public.report_events add constraint report_events_action_check check (
  action in ('submitted', 'viewed', 'status_changed', 'message_removed', 'reporter_revealed', 'username_reset')
);

create table public.profile_notices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  org_id uuid not null references public.organizations (id) on delete cascade,
  kind text not null check (kind in ('username_reset')),
  created_at timestamptz not null default now(),
  seen_at timestamptz
);

create index profile_notices_user_id_unseen_idx on public.profile_notices (user_id, created_at)
  where seen_at is null;

alter table public.profile_notices enable row level security;
revoke all on public.profile_notices from anon, authenticated;
grant select on public.profile_notices to authenticated;

create policy "People can read their own notices"
  on public.profile_notices for select to authenticated
  using (user_id = (select auth.uid()));
create policy "Require two-step verification" on public.profile_notices as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));

create function public.list_profile_notices()
returns table (id uuid, org_id uuid, kind text, created_at timestamptz)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  return query
  select notice.id, notice.org_id, notice.kind, notice.created_at
  from public.profile_notices as notice
  where notice.user_id = (select auth.uid()) and notice.seen_at is null
  order by notice.created_at, notice.id
  limit 20;
end;
$$;

create function public.acknowledge_profile_notices(notice_ids uuid[])
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  update public.profile_notices
  set seen_at = now()
  where user_id = (select auth.uid()) and id = any (coalesce(notice_ids, '{}')) and seen_at is null;
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
  if chosen_subject not in ('message', 'person', 'name', 'username') then
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
  end if;

  insert into public.report_events (report_id, org_id, actor_id, action)
  values (new_report_id, target_org_id, case when coalesce(report_anonymous, false) then null else reporter end, 'submitted');

  return new_report_id;
end;
$$;

drop function public.update_report(uuid, text, text, boolean);

create function public.update_report(
  target_report_id uuid,
  new_status text,
  action_note text default null,
  remove_message boolean default false,
  reset_username boolean default false
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
  public.list_profile_notices(),
  public.acknowledge_profile_notices(uuid[]),
  public.update_report(uuid, text, text, boolean, boolean)
from public, anon;
grant execute on function
  public.list_profile_notices(),
  public.acknowledge_profile_notices(uuid[]),
  public.update_report(uuid, text, text, boolean, boolean)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 25);
$$;
