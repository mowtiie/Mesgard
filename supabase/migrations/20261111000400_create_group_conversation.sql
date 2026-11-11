-- Create one conversation per group through a function, and keep its participants in step with the group roster.

create unique index conversations_group_id_key
  on public.conversations (group_id)
  where type = 'group';

create function public.create_group_conversation(target_group_id uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  target_group public.groups;
  conversation_id uuid;
begin
  select * into target_group from public.groups where id = target_group_id;
  if caller_id is null or target_group.id is null or not exists (
    select 1 from public.org_members
    where org_id = target_group.org_id
      and user_id = caller_id
      and role in ('org_admin', 'staff')
  ) then
    raise exception 'Only organization admins and staff can start a group chat' using errcode = 'insufficient_privilege';
  end if;

  perform pg_advisory_xact_lock(hashtextextended(target_group_id::text, 0));

  select id into conversation_id
  from public.conversations
  where group_id = target_group_id and type = 'group';

  if conversation_id is null then
    insert into public.conversations (org_id, type, group_id, title, created_by)
    values (target_group.org_id, 'group', target_group_id, target_group.name, caller_id)
    returning id into conversation_id;

    insert into public.conversation_participants (org_id, conversation_id, user_id)
    select target_group.org_id, conversation_id, member.user_id
    from public.group_members as member
    where member.group_id = target_group_id
    on conflict do nothing;
  end if;

  insert into public.conversation_participants (org_id, conversation_id, user_id)
  values (target_group.org_id, conversation_id, caller_id)
  on conflict do nothing;

  return conversation_id;
end;
$$;

revoke execute on function public.create_group_conversation(uuid) from public, anon;
grant execute on function public.create_group_conversation(uuid) to authenticated;

create function private.add_group_member_to_conversation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.conversation_participants (org_id, conversation_id, user_id)
  select new.org_id, conversation.id, new.user_id
  from public.conversations as conversation
  where conversation.group_id = new.group_id and conversation.type = 'group'
  on conflict do nothing;
  return new;
end;
$$;

create function private.remove_group_member_from_conversation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  delete from public.conversation_participants as participant
  using public.conversations as conversation
  where participant.conversation_id = conversation.id
    and conversation.group_id = old.group_id
    and conversation.type = 'group'
    and participant.user_id = old.user_id;
  return old;
end;
$$;

create trigger group_members_add_to_conversation
  after insert on public.group_members
  for each row execute function private.add_group_member_to_conversation();

create trigger group_members_remove_from_conversation
  after delete on public.group_members
  for each row execute function private.remove_group_member_from_conversation();
