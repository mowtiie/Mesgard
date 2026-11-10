-- Create direct conversations through a function that is idempotent and limits them to two participants.

create function public.create_direct_conversation(target_org_id uuid, other_user_id uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  conversation_id uuid;
begin
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

  insert into public.conversations (org_id, type, created_by)
  values (target_org_id, 'direct', caller_id)
  returning id into conversation_id;

  insert into public.conversation_participants (org_id, conversation_id, user_id)
  values (target_org_id, conversation_id, caller_id), (target_org_id, conversation_id, other_user_id);

  return conversation_id;
end;
$$;

revoke execute on function public.create_direct_conversation(uuid, uuid) from public, anon;
grant execute on function public.create_direct_conversation(uuid, uuid) to authenticated;

create function private.enforce_direct_participant_limit()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if exists (
    select 1 from public.conversations
    where id = new.conversation_id and type = 'direct'
  ) and (
    select count(*) from public.conversation_participants where conversation_id = new.conversation_id
  ) >= 2 then
    raise exception 'A direct conversation has at most two participants' using errcode = 'check_violation';
  end if;
  return new;
end;
$$;

create trigger conversation_participants_enforce_direct_limit
  before insert on public.conversation_participants
  for each row execute function private.enforce_direct_participant_limit();

drop policy "Members can create conversations as themselves" on public.conversations;

create policy "Members can create group and announcement conversations as themselves"
  on public.conversations for insert to authenticated
  with check (
    created_by = (select auth.uid())
    and type <> 'direct'
    and private.is_org_member(org_id)
    and (type <> 'announcement' or private.has_org_role(org_id, array['org_admin', 'staff']))
  );
