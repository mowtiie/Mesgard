-- Messaging rules an organization can turn on (DepEd Order 49 s. 2022: contact between personnel and learners stays
-- about school matters): staff and students cannot start or keep direct conversations with each other, and quiet
-- hours stop everyone but the organization admins from sending messages at night. Group conversations stay open
-- for staff and students, and reading is never blocked. Both rules are enforced where posting is decided, so no
-- client can go around them. api_level is 13.

alter table public.organizations
  add column block_staff_student_direct_messages boolean not null default false,
  add column quiet_hours_start time,
  add column quiet_hours_end time,
  add column quiet_hours_timezone text not null default 'Asia/Manila',
  add constraint organizations_quiet_hours_check
    check (
      (quiet_hours_start is null and quiet_hours_end is null)
      or (quiet_hours_start is not null and quiet_hours_end is not null and quiet_hours_start <> quiet_hours_end)
    );

create function private.quiet_hours_active(target_org_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce((
    select case
      when organization.quiet_hours_start is null then false
      when organization.quiet_hours_start < organization.quiet_hours_end
        then local.time_of_day >= organization.quiet_hours_start and local.time_of_day < organization.quiet_hours_end
      else local.time_of_day >= organization.quiet_hours_start or local.time_of_day < organization.quiet_hours_end
    end
    from public.organizations as organization
    cross join lateral (select (now() at time zone organization.quiet_hours_timezone)::time as time_of_day) as local
    where organization.id = target_org_id
  ), false);
$$;

create function private.is_staff_student_pair(target_org_id uuid, first_user_id uuid, second_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.org_members as first_member
    join public.org_members as second_member on second_member.org_id = first_member.org_id
    where first_member.org_id = target_org_id
      and first_member.user_id = first_user_id
      and second_member.user_id = second_user_id
      and (
        (first_member.role = 'staff' and second_member.role = 'member')
        or (first_member.role = 'member' and second_member.role = 'staff')
      )
  );
$$;

create function private.staff_student_direct_blocked(target_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.conversations as conversation
    join public.organizations as organization on organization.id = conversation.org_id
    join public.conversation_participants as first_participant on first_participant.conversation_id = conversation.id
    join public.conversation_participants as second_participant
      on second_participant.conversation_id = conversation.id and second_participant.user_id > first_participant.user_id
    where conversation.id = target_conversation_id
      and conversation.type = 'direct'
      and organization.block_staff_student_direct_messages
      and private.is_staff_student_pair(conversation.org_id, first_participant.user_id, second_participant.user_id)
  );
$$;

create or replace function private.can_post_to_conversation(target_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.conversation_participants as participant
    join public.conversations as conversation on conversation.id = participant.conversation_id
    join public.org_members as member
      on member.org_id = conversation.org_id
     and member.user_id = participant.user_id
    left join public.groups as grp on grp.id = conversation.group_id
    where participant.conversation_id = target_conversation_id
      and participant.user_id = (select auth.uid())
      and (
        conversation.type <> 'group'
        or coalesce(grp.posting_policy, 'everyone') = 'everyone'
        or member.role = 'org_admin'
        or exists (
          select 1
          from public.group_members as group_member
          where group_member.group_id = conversation.group_id
            and group_member.user_id = participant.user_id
            and group_member.role_in_group = 'moderator'
        )
      )
      and (
        conversation.type <> 'direct'
        or (
          select count(*)
          from public.conversation_participants as everyone
          where everyone.conversation_id = conversation.id
        ) = 2
      )
      and (member.role = 'org_admin' or not private.quiet_hours_active(conversation.org_id))
      and not private.staff_student_direct_blocked(conversation.id)
  );
$$;

create or replace function public.create_direct_conversation(target_org_id uuid, other_user_id uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  conversation_id uuid;
begin
  perform private.require_mfa();
  if caller_id is null or not exists (
    select 1 from public.org_members
    where org_id = target_org_id and user_id = caller_id
  ) then
    raise exception 'Only organization members can start a conversation' using errcode = 'insufficient_privilege';
  end if;
  if other_user_id = caller_id then
    raise exception 'A direct conversation needs another member' using errcode = 'invalid_parameter_value';
  end if;
  if not exists (
    select 1 from public.org_members
    where org_id = target_org_id and user_id = other_user_id
  ) then
    raise exception 'The other user is not a member of this organization' using errcode = 'insufficient_privilege';
  end if;

  perform pg_advisory_xact_lock(
    hashtextextended(least(caller_id, other_user_id)::text || greatest(caller_id, other_user_id)::text || target_org_id::text, 0)
  );

  select conversation.id into conversation_id
  from public.conversations as conversation
  join public.conversation_participants as mine
    on mine.conversation_id = conversation.id and mine.user_id = caller_id
  join public.conversation_participants as theirs
    on theirs.conversation_id = conversation.id and theirs.user_id = other_user_id
  where conversation.type = 'direct' and conversation.org_id = target_org_id
  limit 1;

  if conversation_id is not null then
    return conversation_id;
  end if;

  if (select block_staff_student_direct_messages from public.organizations where id = target_org_id)
    and private.is_staff_student_pair(target_org_id, caller_id, other_user_id) then
    raise exception 'Direct messages between staff and students are turned off' using errcode = 'insufficient_privilege';
  end if;

  insert into public.conversations (org_id, type, created_by)
  values (target_org_id, 'direct', caller_id)
  returning id into conversation_id;

  insert into public.conversation_participants (org_id, conversation_id, user_id)
  values (target_org_id, conversation_id, caller_id), (target_org_id, conversation_id, other_user_id);

  return conversation_id;
end;
$$;

create function public.get_messaging_rules(target_org_id uuid)
returns table (
  block_staff_student_direct_messages boolean,
  quiet_hours_start time,
  quiet_hours_end time,
  quiet_hours_timezone text,
  quiet_hours_active boolean
)
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
  return query
  select
    organization.block_staff_student_direct_messages,
    organization.quiet_hours_start,
    organization.quiet_hours_end,
    organization.quiet_hours_timezone,
    private.quiet_hours_active(organization.id)
  from public.organizations as organization
  where organization.id = target_org_id;
end;
$$;

create function public.set_messaging_rules(
  target_org_id uuid,
  block_staff_student boolean,
  quiet_start time,
  quiet_end time,
  quiet_timezone text default 'Asia/Manila'
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can set the messaging rules' using errcode = '42501';
  end if;
  if (quiet_start is null) <> (quiet_end is null) or quiet_start is not distinct from quiet_end and quiet_start is not null then
    raise exception 'Quiet hours need a start and a different end, or neither' using errcode = '22023';
  end if;
  perform now() at time zone coalesce(quiet_timezone, 'Asia/Manila');

  update public.organizations
  set block_staff_student_direct_messages = coalesce(block_staff_student, false),
      quiet_hours_start = quiet_start,
      quiet_hours_end = quiet_end,
      quiet_hours_timezone = coalesce(quiet_timezone, 'Asia/Manila')
  where id = target_org_id;

  perform private.record_audit_event(
    target_org_id, 'organization.messaging_rules_changed', 'organization', target_org_id,
    jsonb_build_object(
      'block_staff_student_direct_messages', coalesce(block_staff_student, false),
      'quiet_hours', quiet_start is not null
    )
  );
end;
$$;

revoke execute on function
  public.get_messaging_rules(uuid),
  public.set_messaging_rules(uuid, boolean, time, time, text)
from public, anon;
grant execute on function
  public.get_messaging_rules(uuid),
  public.set_messaging_rules(uuid, boolean, time, time, text)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 13);
$$;
