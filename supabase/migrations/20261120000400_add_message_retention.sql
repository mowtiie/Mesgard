-- Retention: an organization admin chooses how long messages are kept (NPC Circular 2023-06: personal data is stored
-- only as long as necessary). A job erases what is older: the text is blanked like a deleted message, the archived
-- originals and earlier versions are purged, and the files go. The files cannot be deleted with SQL, so the job
-- (node scripts/server/index.mjs retention) lists them with list_expired_attachment_paths, removes them through the
-- Storage API and then calls expire_messages, which can be repeated after a failure. A legal hold or an open report
-- protects a message, so retention never destroys what a case needs. Reports keep their own frozen copy until they are
-- closed. api_level is 11.

alter table public.organizations
  add column message_retention_days integer
    check (message_retention_days is null or message_retention_days between 30 and 3650);

create index messages_org_id_created_at_idx on public.messages (org_id, created_at);

create function private.message_is_protected(target_message_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.messages as message
    left join public.conversations as conversation on conversation.id = message.conversation_id
    where message.id = target_message_id
      and (
        exists (
          select 1 from public.legal_holds as hold
          where hold.released_at is null
            and hold.org_id = message.org_id
            and (
              (hold.user_id is null and hold.group_id is null)
              or hold.user_id = message.sender_id
              or (hold.group_id is not null and hold.group_id = conversation.group_id)
            )
        )
        or exists (
          select 1 from public.reports as report
          where report.status in ('open', 'in_review')
            and report.org_id = message.org_id
            and (
              report.subject_user_id = message.sender_id
              or report.reporter_id = message.sender_id
              or report.conversation_id = message.conversation_id
            )
        )
      )
  );
$$;

-- The messages the next batch deals with. A message is done once it is deleted and nothing readable is left of it.
create function private.expired_message_ids(batch_limit integer)
returns setof uuid
language sql
stable
security definer
set search_path = ''
as $$
  select message.id
  from public.messages as message
  join public.organizations as organization
    on organization.id = message.org_id and organization.message_retention_days is not null
  where message.created_at < now() - make_interval(days => organization.message_retention_days)
    and (
      message.deleted_at is null
      or exists (select 1 from public.deleted_message_bodies as archived where archived.message_id = message.id)
      or exists (select 1 from public.message_edits as edit where edit.message_id = message.id)
      or exists (select 1 from public.attachments as attachment where attachment.message_id = message.id)
    )
    and not private.message_is_protected(message.id)
  order by message.created_at, message.id
  limit least(greatest(batch_limit, 1), 1000);
$$;

create function public.list_expired_attachment_paths(batch_limit integer default 200)
returns setof text
language sql
stable
security definer
set search_path = ''
as $$
  select attachment.storage_path
  from public.attachments as attachment
  where attachment.message_id in (select private.expired_message_ids(batch_limit));
$$;

create function public.expire_messages(batch_limit integer default 200)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  expired_ids uuid[] := array(select private.expired_message_ids(batch_limit));
begin
  if cardinality(expired_ids) = 0 then
    return 0;
  end if;
  -- Deleting a message archives its text for administrators, so the archive is purged right after.
  update public.messages set deleted_at = now() where id = any (expired_ids) and deleted_at is null;
  delete from public.deleted_message_bodies where message_id = any (expired_ids);
  delete from public.message_edits where message_id = any (expired_ids);
  delete from public.attachments where message_id = any (expired_ids);
  return cardinality(expired_ids);
end;
$$;

revoke execute on function public.list_expired_attachment_paths(integer), public.expire_messages(integer)
from public, anon, authenticated;
grant execute on function public.list_expired_attachment_paths(integer), public.expire_messages(integer)
to service_role;

-- Choosing the limit (organization admins only).

create function public.get_message_retention(target_org_id uuid)
returns integer
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not exists (
    select 1 from public.org_members where org_id = target_org_id and user_id = (select auth.uid())
  ) then
    raise exception 'Not a member of this organization' using errcode = '42501';
  end if;
  return (select message_retention_days from public.organizations where id = target_org_id);
end;
$$;

create function public.set_message_retention(target_org_id uuid, retention_days integer)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can set the retention' using errcode = '42501';
  end if;
  if retention_days is not null and retention_days not between 30 and 3650 then
    raise exception 'Keep messages for 30 to 3650 days, or keep them without a limit' using errcode = '22023';
  end if;
  if (select message_retention_days from public.organizations where id = target_org_id) is not distinct from retention_days then
    return;
  end if;

  update public.organizations set message_retention_days = retention_days where id = target_org_id;
  perform private.record_audit_event(
    target_org_id, 'organization.retention_changed', 'organization', target_org_id,
    jsonb_build_object('days', retention_days)
  );
end;
$$;

revoke execute on function public.get_message_retention(uuid), public.set_message_retention(uuid, integer)
from public, anon;
grant execute on function public.get_message_retention(uuid), public.set_message_retention(uuid, integer)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 11);
$$;
