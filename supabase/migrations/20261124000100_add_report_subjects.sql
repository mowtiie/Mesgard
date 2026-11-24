-- Reports name what is reported (subject): a message, the conduct of a person, or the name of a person. Subjects for
-- usernames, profile pictures and groups are accepted by the schema and refused by submit_report until those features
-- exist. A report about a name keeps a frozen copy of the name. Adds the categories impersonation and
-- offensive_content. api_level is 23.

alter table public.reports add column subject text;
alter table public.reports add column target_group_id uuid references public.groups (id);

update public.reports set subject = case when message_id is null then 'person' else 'message' end;

alter table public.reports alter column subject set not null;
alter table public.reports add constraint reports_subject_check check (
  subject in ('message', 'person', 'name', 'username', 'profile_picture', 'group_picture', 'group_name')
);
alter table public.reports add constraint reports_subject_target_check check (
  (subject = 'message' and message_id is not null and target_group_id is null)
  or (subject in ('person', 'name', 'username', 'profile_picture') and message_id is null and target_group_id is null)
  or (subject in ('group_picture', 'group_name') and message_id is null and target_group_id is not null)
);

alter table public.reports drop constraint reports_category_check;
alter table public.reports add constraint reports_category_check check (
  category in ('bullying', 'harassment', 'child_safety', 'impersonation', 'offensive_content', 'other')
);

create unique index reports_reporter_id_subject_user_id_key on public.reports (reporter_id, subject, subject_user_id)
  where message_id is null and target_group_id is null;
create unique index reports_reporter_id_subject_target_group_id_key on public.reports (reporter_id, subject, target_group_id)
  where target_group_id is not null;
create index reports_target_group_id_idx on public.reports (target_group_id) where target_group_id is not null;

alter table public.report_evidence alter column message_id drop not null;
alter table public.report_evidence alter column conversation_id drop not null;
alter table public.report_evidence alter column sender_id drop not null;
alter table public.report_evidence alter column sender_name drop not null;
alter table public.report_evidence alter column body drop not null;
alter table public.report_evidence alter column sent_at drop not null;
alter table public.report_evidence add column subject_text text;
alter table public.report_evidence add constraint report_evidence_content_check check (
  (message_id is not null and body is not null and sender_id is not null and sender_name is not null
    and sent_at is not null and conversation_id is not null and subject_text is null)
  or (message_id is null and subject_text is not null)
);

drop function public.submit_report(uuid, uuid, text, uuid, uuid, text, boolean);

create function public.submit_report(
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
  if chosen_subject not in ('message', 'person', 'name') then
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
  end if;

  insert into public.report_events (report_id, org_id, actor_id, action)
  values (new_report_id, target_org_id, case when coalesce(report_anonymous, false) then null else reporter end, 'submitted');

  return new_report_id;
end;
$$;

drop function public.list_reports(uuid, text, integer, timestamptz, uuid);

create function public.list_reports(
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
    report.id, report.category, report.status, report.subject, report.subject_user_id, subject_profile.display_name,
    report.message_id, report.anonymous, case when report.anonymous then null else reporter.display_name end,
    report.created_at
  from public.reports as report
  join public.profiles as subject_profile on subject_profile.id = report.subject_user_id
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
    'subject_name', subject_profile.display_name,
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
  from public.profiles as subject_profile, public.profiles as reporter
  where subject_profile.id = report_row.subject_user_id and reporter.id = report_row.reporter_id;

  return result;
end;
$$;

revoke execute on function
  public.submit_report(uuid, uuid, text, uuid, uuid, text, boolean, text),
  public.list_reports(uuid, text, integer, timestamptz, uuid)
from public, anon;
grant execute on function
  public.submit_report(uuid, uuid, text, uuid, uuid, text, boolean, text),
  public.list_reports(uuid, text, integer, timestamptz, uuid)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 23);
$$;
