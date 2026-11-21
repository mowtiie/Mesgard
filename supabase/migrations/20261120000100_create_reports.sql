-- Reports: a person reports a message or another person to the reviewers their organization designates (the
-- organization admins when it designates none). A report about a message keeps a frozen copy of the message, the
-- messages before it and the file details, so the evidence survives an edit, a deletion or an erasure. An anonymous
-- report hides the reporter from reviewers; the server still keeps who reported, and only an organization admin can
-- reveal the reporter, with a reason that is recorded. Reviewers read cases through functions only, so the reporter
-- column is never reachable. api_level is 8.

create table public.report_reviewers (
  org_id uuid not null,
  user_id uuid not null,
  created_at timestamptz not null default now(),
  primary key (org_id, user_id),
  foreign key (org_id, user_id) references public.org_members (org_id, user_id) on delete cascade
);

create function private.is_report_reviewer(target_org_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select case
    when exists (select 1 from public.report_reviewers where org_id = target_org_id)
      then exists (
        select 1 from public.report_reviewers
        where org_id = target_org_id and user_id = (select auth.uid())
      )
    else private.has_org_role(target_org_id, array['org_admin'])
  end;
$$;

create table public.reports (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations (id),
  reporter_id uuid not null references public.profiles (id),
  anonymous boolean not null default false,
  category text not null check (category in ('bullying', 'harassment', 'child_safety', 'other')),
  subject_user_id uuid not null references public.profiles (id),
  message_id uuid,
  conversation_id uuid,
  note text check (char_length(note) <= 1000),
  status text not null default 'open' check (status in ('open', 'in_review', 'resolved', 'dismissed')),
  resolved_by uuid references public.profiles (id),
  resolution_note text check (char_length(resolution_note) <= 1000),
  resolved_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index reports_org_id_created_at_idx on public.reports (org_id, created_at desc, id desc);
create index reports_org_id_status_created_at_idx on public.reports (org_id, status, created_at desc, id desc);
create index reports_reporter_id_created_at_idx on public.reports (reporter_id, created_at desc);
create index reports_subject_user_id_idx on public.reports (subject_user_id);
create unique index reports_reporter_id_message_id_key on public.reports (reporter_id, message_id)
  where message_id is not null;

create table public.report_events (
  id uuid primary key default gen_random_uuid(),
  report_id uuid not null references public.reports (id),
  org_id uuid not null references public.organizations (id),
  actor_id uuid references public.profiles (id),
  action text not null check (action in ('submitted', 'viewed', 'status_changed', 'message_removed', 'reporter_revealed')),
  note text check (char_length(note) <= 1000),
  created_at timestamptz not null default now()
);

create index report_events_report_id_created_at_idx on public.report_events (report_id, created_at, id);

create table public.report_evidence (
  report_id uuid primary key references public.reports (id),
  message_id uuid not null,
  conversation_id uuid not null,
  conversation_title text,
  sender_id uuid not null,
  sender_name text not null,
  body text not null,
  sent_at timestamptz not null,
  edited_at timestamptz,
  context jsonb not null default '[]',
  captured_at timestamptz not null default now()
);

create table public.report_evidence_files (
  id uuid primary key default gen_random_uuid(),
  report_id uuid not null references public.reports (id),
  storage_path text not null,
  file_name text not null,
  mime_type text not null,
  size_bytes bigint not null
);

create index report_evidence_files_report_id_idx on public.report_evidence_files (report_id);
create index report_evidence_files_storage_path_idx on public.report_evidence_files (storage_path);

create function private.reject_report_record_changes()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  raise exception 'Report evidence and history cannot be changed' using errcode = 'restrict_violation';
end;
$$;

create trigger report_events_reject_row_changes
  before update or delete on public.report_events
  for each row execute function private.reject_report_record_changes();
create trigger report_events_reject_truncate
  before truncate on public.report_events
  for each statement execute function private.reject_report_record_changes();

create trigger report_evidence_reject_row_changes
  before update or delete on public.report_evidence
  for each row execute function private.reject_report_record_changes();
create trigger report_evidence_reject_truncate
  before truncate on public.report_evidence
  for each statement execute function private.reject_report_record_changes();

create trigger report_evidence_files_reject_row_changes
  before update or delete on public.report_evidence_files
  for each row execute function private.reject_report_record_changes();
create trigger report_evidence_files_reject_truncate
  before truncate on public.report_evidence_files
  for each statement execute function private.reject_report_record_changes();

alter table public.report_reviewers enable row level security;
alter table public.reports enable row level security;
alter table public.report_events enable row level security;
alter table public.report_evidence enable row level security;
alter table public.report_evidence_files enable row level security;

revoke all on public.report_reviewers, public.reports, public.report_events, public.report_evidence,
  public.report_evidence_files from anon, authenticated;

grant select on public.report_reviewers to authenticated;
-- A reporter follows the status of their own reports. The reporter and the notes of reviewers are never granted.
grant select (id, org_id, anonymous, category, message_id, conversation_id, status, created_at, updated_at)
  on public.reports to authenticated;

create policy "Administrators and the reviewers themselves can read the reviewers"
  on public.report_reviewers for select to authenticated
  using (private.has_org_role(org_id, array['org_admin']) or user_id = (select auth.uid()));

create policy "Reporters can read the status of their own reports"
  on public.reports for select to authenticated
  using (reporter_id = (select auth.uid()));

create policy "Require two-step verification" on public.report_reviewers as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));
create policy "Require two-step verification" on public.reports as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));
create policy "Require two-step verification" on public.report_events as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));
create policy "Require two-step verification" on public.report_evidence as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));
create policy "Require two-step verification" on public.report_evidence_files as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));

-- Files of a message removed after a report stop being readable by the participants. Reviewers read the files kept as
-- evidence.

alter table public.attachments add column removed_at timestamptz;

create function private.attachment_is_removed(object_name text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.attachments where storage_path = object_name and removed_at is not null
  );
$$;

create function private.is_report_evidence_file_reviewer(object_name text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.report_evidence_files as file
    join public.reports as report on report.id = file.report_id
    where file.storage_path = object_name and private.is_report_reviewer(report.org_id)
  );
$$;

drop policy "Participants can read attachments of their conversations" on storage.objects;
create policy "Participants can read attachments of their conversations"
  on storage.objects for select to authenticated
  using (
    bucket_id = 'message-attachments'
    and private.is_conversation_participant(private.conversation_id_from_object_name(name))
    and not private.attachment_is_removed(name)
  );

create policy "Report reviewers can read the files kept as evidence"
  on storage.objects for select to authenticated
  using (bucket_id = 'message-attachments' and private.is_report_evidence_file_reviewer(name));

drop policy "Participants can read attachments of messages they can read" on public.attachments;
create policy "Participants can read attachments of messages they can read"
  on public.attachments for select to authenticated
  using (removed_at is null and private.can_read_message(message_id));

-- Submitting a report.

create function public.submit_report(
  new_report_id uuid,
  target_org_id uuid,
  report_category text,
  target_message_id uuid default null,
  target_user_id uuid default null,
  report_note text default null,
  report_anonymous boolean default false
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  reporter uuid := (select auth.uid());
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
  if (target_message_id is null) = (target_user_id is null) then
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
    id, org_id, reporter_id, anonymous, category, subject_user_id, message_id, conversation_id, note
  )
  values (
    new_report_id, target_org_id, reporter, coalesce(report_anonymous, false), report_category, subject,
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
  end if;

  insert into public.report_events (report_id, org_id, actor_id, action)
  values (new_report_id, target_org_id, case when coalesce(report_anonymous, false) then null else reporter end, 'submitted');

  return new_report_id;
end;
$$;

-- Reading and handling cases (reviewers only).

create function public.can_review_reports(target_org_id uuid)
returns boolean
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  return private.is_report_reviewer(target_org_id);
end;
$$;

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
    report.id, report.category, report.status, report.subject_user_id, subject.display_name, report.message_id,
    report.anonymous, case when report.anonymous then null else reporter.display_name end, report.created_at
  from public.reports as report
  join public.profiles as subject on subject.id = report.subject_user_id
  join public.profiles as reporter on reporter.id = report.reporter_id
  where report.org_id = target_org_id
    and (status_filter is null or report.status = status_filter)
    and (before_created_at is null or (report.created_at, report.id) < (before_created_at, before_report_id))
  order by report.created_at desc, report.id desc
  limit least(greatest(page_size, 1), 50);
end;
$$;

create function public.get_report(target_report_id uuid)
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
    'status', report_row.status,
    'note', report_row.note,
    'anonymous', report_row.anonymous,
    'reporter_name', case when report_row.anonymous then null else reporter.display_name end,
    'subject_user_id', report_row.subject_user_id,
    'subject_name', subject.display_name,
    'message_removed', exists (
      select 1 from public.report_events where report_id = report_row.id and action = 'message_removed'
    ),
    'resolution_note', report_row.resolution_note,
    'created_at', report_row.created_at,
    'resolved_at', report_row.resolved_at,
    'evidence', (
      select to_jsonb(evidence) - 'report_id' from public.report_evidence as evidence
      where evidence.report_id = report_row.id
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
  from public.profiles as subject, public.profiles as reporter
  where subject.id = report_row.subject_user_id and reporter.id = report_row.reporter_id;

  return result;
end;
$$;

create function public.update_report(
  target_report_id uuid,
  new_status text,
  action_note text default null,
  remove_message boolean default false
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

  if remove_message then
    update public.messages set deleted_at = now() where id = report_row.message_id and deleted_at is null;
    update public.attachments set removed_at = now() where message_id = report_row.message_id and removed_at is null;
    insert into public.report_events (report_id, org_id, actor_id, action)
    values (report_row.id, report_row.org_id, actor, 'message_removed');
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

create function public.reveal_report_reporter(target_report_id uuid, reveal_reason text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  report_row public.reports;
  trimmed_reason text := trim(reveal_reason);
  reporter_name text;
begin
  perform private.require_mfa();
  select * into report_row from public.reports where id = target_report_id;
  if not found or not private.has_org_role(report_row.org_id, array['org_admin']) then
    raise exception 'Only an organization admin can reveal a reporter' using errcode = '42501';
  end if;
  if char_length(coalesce(trimmed_reason, '')) < 10 then
    raise exception 'Revealing a reporter needs a reason of at least 10 characters' using errcode = '22023';
  end if;

  insert into public.report_events (report_id, org_id, actor_id, action, note)
  values (report_row.id, report_row.org_id, (select auth.uid()), 'reporter_revealed', left(trimmed_reason, 1000));
  perform private.record_audit_event(report_row.org_id, 'report.reporter_revealed', 'report', report_row.id);

  select display_name into reporter_name from public.profiles where id = report_row.reporter_id;
  return jsonb_build_object('reporter_id', report_row.reporter_id, 'reporter_name', reporter_name);
end;
$$;

create function public.list_report_reviewers(target_org_id uuid)
returns table (user_id uuid, display_name text)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can read the reviewers' using errcode = '42501';
  end if;
  return query
  select reviewer.user_id, profile.display_name
  from public.report_reviewers as reviewer
  join public.profiles as profile on profile.id = reviewer.user_id
  where reviewer.org_id = target_org_id
  order by profile.display_name, reviewer.user_id;
end;
$$;

create function public.set_report_reviewers(target_org_id uuid, reviewer_ids uuid[])
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  chosen uuid[] := coalesce(reviewer_ids, '{}');
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can choose the reviewers' using errcode = '42501';
  end if;
  if cardinality(chosen) > 20 then
    raise exception 'At most 20 reviewers' using errcode = '54000';
  end if;
  if exists (
    select 1 from unnest(chosen) as chosen_id
    where not exists (
      select 1 from public.org_members as member
      where member.org_id = target_org_id and member.user_id = chosen_id and member.role in ('org_admin', 'staff')
    )
  ) then
    raise exception 'Reviewers must be administrators or staff of the organization' using errcode = '22023';
  end if;

  delete from public.report_reviewers where org_id = target_org_id and not (user_id = any (chosen));
  insert into public.report_reviewers (org_id, user_id)
  select target_org_id, chosen_id from unnest(chosen) as chosen_id
  on conflict do nothing;

  perform private.record_audit_event(
    target_org_id, 'report_reviewers.changed', 'organization', target_org_id,
    jsonb_build_object('count', cardinality(chosen))
  );
end;
$$;

revoke execute on function
  public.submit_report(uuid, uuid, text, uuid, uuid, text, boolean),
  public.can_review_reports(uuid),
  public.list_reports(uuid, text, integer, timestamptz, uuid),
  public.get_report(uuid),
  public.update_report(uuid, text, text, boolean),
  public.reveal_report_reporter(uuid, text),
  public.list_report_reviewers(uuid),
  public.set_report_reviewers(uuid, uuid[])
from public, anon;
grant execute on function
  public.submit_report(uuid, uuid, text, uuid, uuid, text, boolean),
  public.can_review_reports(uuid),
  public.list_reports(uuid, text, integer, timestamptz, uuid),
  public.get_report(uuid),
  public.update_report(uuid, text, text, boolean),
  public.reveal_report_reporter(uuid, text),
  public.list_report_reviewers(uuid),
  public.set_report_reviewers(uuid, uuid[])
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 8);
$$;
