-- Data export for an access request (Data Privacy Act, right of access and data portability). An organization admin
-- exports what the organization holds about one person: the account and profile, memberships, conversations, the
-- messages the person sent (with the originals of deleted ones, the earlier versions of edited ones and the details of
-- the files), and the audit entries about them. Messages other people sent are theirs, so they are not included, and
-- neither are reports and legal holds, which the Data Protection Officer handles. Every export is recorded in the audit
-- log. The messages come in pages so a large history never has to fit in one answer. api_level is 12.

create function private.person_is_known_in_org(target_org_id uuid, target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (select 1 from public.org_members where org_id = target_org_id and user_id = target_user_id)
    or exists (select 1 from public.messages where org_id = target_org_id and sender_id = target_user_id)
    or exists (
      select 1 from public.audit_logs
      where org_id = target_org_id and (target_id = target_user_id or actor_id = target_user_id)
    );
$$;

create function public.export_person_overview(target_org_id uuid, target_user_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  result jsonb;
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can export data' using errcode = '42501';
  end if;
  if not private.person_is_known_in_org(target_org_id, target_user_id) then
    raise exception 'Person not found in this organization' using errcode = 'P0002';
  end if;

  select jsonb_build_object(
    'format_version', 1,
    'exported_at', now(),
    'organization', (select jsonb_build_object('id', organization.id, 'name', organization.name)
                     from public.organizations as organization where organization.id = target_org_id),
    'profile', (select jsonb_build_object('display_name', profile.display_name, 'status', profile.status,
                                           'created_at', profile.created_at)
                from public.profiles as profile where profile.id = target_user_id),
    'account', (select jsonb_build_object(
                  'email', account.email,
                  'created_at', account.created_at,
                  'last_sign_in_at', account.last_sign_in_at,
                  'two_step_verification', exists (
                    select 1 from auth.mfa_factors as factor
                    where factor.user_id = target_user_id and factor.status = 'verified'
                  ))
                from auth.users as account where account.id = target_user_id),
    'membership', (select jsonb_build_object('role', member.role, 'joined_at', member.created_at)
                   from public.org_members as member
                   where member.org_id = target_org_id and member.user_id = target_user_id),
    'groups', coalesce((
      select jsonb_agg(jsonb_build_object('name', target_group.name, 'role', membership.role_in_group,
                                          'joined_at', membership.created_at) order by target_group.name)
      from public.group_members as membership
      join public.groups as target_group on target_group.id = membership.group_id
      where membership.org_id = target_org_id and membership.user_id = target_user_id
    ), '[]'::jsonb),
    'conversations', coalesce((
      select jsonb_agg(jsonb_build_object(
        'type', conversation.type, 'title', conversation.title, 'muted', participant.muted,
        'archived', participant.archived_at is not null, 'joined_at', participant.joined_at
      ) order by participant.joined_at)
      from public.conversation_participants as participant
      join public.conversations as conversation on conversation.id = participant.conversation_id
      where participant.org_id = target_org_id and participant.user_id = target_user_id
    ), '[]'::jsonb),
    'favorite_contacts', coalesce((
      select jsonb_agg(profile.display_name order by profile.display_name)
      from public.favorite_contacts as favorite
      join public.profiles as profile on profile.id = favorite.favorite_user_id
      where favorite.org_id = target_org_id and favorite.user_id = target_user_id
    ), '[]'::jsonb),
    'notification_preview', (select settings.notification_preview from public.user_settings as settings
                             where settings.user_id = target_user_id),
    'privacy_notice_acknowledged_version', (
      select acknowledgment.version from public.privacy_notice_acknowledgments as acknowledgment
      where acknowledgment.org_id = target_org_id and acknowledgment.user_id = target_user_id
    ),
    'push_devices', (select count(*) from public.push_subscriptions as subscription
                     where subscription.user_id = target_user_id),
    'messages_sent', (select count(*) from public.messages as message
                      where message.org_id = target_org_id and message.sender_id = target_user_id),
    'messages_read', (select count(*) from public.message_receipts as receipt
                      where receipt.org_id = target_org_id and receipt.user_id = target_user_id
                        and receipt.read_at is not null),
    'audit_entries', coalesce((
      select jsonb_agg(jsonb_build_object(
        'action', entry.action, 'as', case when entry.actor_id = target_user_id then 'actor' else 'subject' end,
        'at', entry.created_at
      ) order by entry.created_at desc)
      from (
        select audit.action, audit.actor_id, audit.created_at
        from public.audit_logs as audit
        where audit.org_id = target_org_id and (audit.actor_id = target_user_id or audit.target_id = target_user_id)
        order by audit.created_at desc
        limit 500
      ) as entry
    ), '[]'::jsonb)
  )
  into result;

  perform private.record_audit_event(target_org_id, 'member.data_exported', 'user', target_user_id);
  return result;
end;
$$;

create function public.export_person_messages(
  target_org_id uuid,
  target_user_id uuid,
  page_size integer default 200,
  before_created_at timestamptz default null,
  before_message_id uuid default null
)
returns table (
  message_id uuid,
  conversation_type text,
  conversation_title text,
  created_at timestamptz,
  edited_at timestamptz,
  deleted_at timestamptz,
  body text,
  deleted_original text,
  earlier_versions jsonb,
  attachments jsonb
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can export data' using errcode = '42501';
  end if;
  if not private.person_is_known_in_org(target_org_id, target_user_id) then
    raise exception 'Person not found in this organization' using errcode = 'P0002';
  end if;
  return query
  select
    message.id,
    conversation.type,
    conversation.title,
    message.created_at,
    message.edited_at,
    message.deleted_at,
    message.body,
    archived.body,
    coalesce((
      select jsonb_agg(jsonb_build_object('text', edit.body, 'edited_at', edit.edited_at) order by edit.edited_at)
      from public.message_edits as edit where edit.message_id = message.id
    ), '[]'::jsonb),
    coalesce((
      select jsonb_agg(jsonb_build_object(
        'file_name', attachment.file_name, 'mime_type', attachment.mime_type, 'size_bytes', attachment.size_bytes
      ) order by attachment.file_name)
      from public.attachments as attachment where attachment.message_id = message.id
    ), '[]'::jsonb)
  from public.messages as message
  join public.conversations as conversation on conversation.id = message.conversation_id
  left join public.deleted_message_bodies as archived on archived.message_id = message.id
  where message.org_id = target_org_id
    and message.sender_id = target_user_id
    and (before_created_at is null or (message.created_at, message.id) < (before_created_at, before_message_id))
  order by message.created_at desc, message.id desc
  limit least(greatest(page_size, 1), 500);
end;
$$;

revoke execute on function
  public.export_person_overview(uuid, uuid),
  public.export_person_messages(uuid, uuid, integer, timestamptz, uuid)
from public, anon;
grant execute on function
  public.export_person_overview(uuid, uuid),
  public.export_person_messages(uuid, uuid, integer, timestamptz, uuid)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 12);
$$;
