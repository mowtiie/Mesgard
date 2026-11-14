-- Audit entries for roster and group changes made by signed in users, and one announcement conversation per organization.

create function private.record_audit_event(
  target_org_id uuid,
  event_action text,
  event_target_type text,
  event_target_id uuid,
  event_metadata jsonb default '{}'
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  actor uuid := (select auth.uid());
begin
  -- Changes made by the system (cascades, migrations, the service role) have no actor and are not recorded.
  if actor is null then
    return;
  end if;
  insert into public.audit_logs (org_id, actor_id, action, target_type, target_id, metadata)
  values (target_org_id, actor, event_action, event_target_type, event_target_id, event_metadata);
end;
$$;

create function private.audit_org_member_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    perform private.record_audit_event(
      new.org_id, 'member.added', 'user', new.user_id, jsonb_build_object('role', new.role)
    );
  elsif tg_op = 'UPDATE' then
    perform private.record_audit_event(
      new.org_id, 'member.role_changed', 'user', new.user_id,
      jsonb_build_object('from', old.role, 'to', new.role)
    );
  else
    perform private.record_audit_event(
      old.org_id, 'member.removed', 'user', old.user_id, jsonb_build_object('role', old.role)
    );
  end if;
  return null;
end;
$$;

create trigger org_members_audit_insert_delete
  after insert or delete on public.org_members
  for each row execute function private.audit_org_member_change();

create trigger org_members_audit_role_change
  after update on public.org_members
  for each row when (old.role is distinct from new.role)
  execute function private.audit_org_member_change();

create function private.audit_group_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    perform private.record_audit_event(
      new.org_id, 'group.created', 'group', new.id, jsonb_build_object('name', new.name)
    );
  elsif tg_op = 'UPDATE' then
    perform private.record_audit_event(
      new.org_id, 'group.renamed', 'group', new.id, jsonb_build_object('from', old.name, 'to', new.name)
    );
  else
    perform private.record_audit_event(
      old.org_id, 'group.deleted', 'group', old.id, jsonb_build_object('name', old.name)
    );
  end if;
  return null;
end;
$$;

create trigger groups_audit_insert_delete
  after insert or delete on public.groups
  for each row execute function private.audit_group_change();

create trigger groups_audit_rename
  after update on public.groups
  for each row when (old.name is distinct from new.name)
  execute function private.audit_group_change();

create function private.audit_group_member_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    perform private.record_audit_event(
      new.org_id, 'group_member.added', 'user', new.user_id, jsonb_build_object('group_id', new.group_id)
    );
  elsif tg_op = 'UPDATE' then
    perform private.record_audit_event(
      new.org_id, 'group_member.role_changed', 'user', new.user_id,
      jsonb_build_object('group_id', new.group_id, 'from', old.role_in_group, 'to', new.role_in_group)
    );
  -- Deleting a group removes its members too; the group deletion is the entry that matters.
  elsif exists (select 1 from public.groups where id = old.group_id) then
    perform private.record_audit_event(
      old.org_id, 'group_member.removed', 'user', old.user_id, jsonb_build_object('group_id', old.group_id)
    );
  end if;
  return null;
end;
$$;

create trigger group_members_audit_insert_delete
  after insert or delete on public.group_members
  for each row execute function private.audit_group_member_change();

create trigger group_members_audit_role_change
  after update on public.group_members
  for each row when (old.role_in_group is distinct from new.role_in_group)
  execute function private.audit_group_member_change();

create unique index conversations_announcement_org_key
  on public.conversations (org_id)
  where type = 'announcement';

create function public.create_announcement_conversation(target_org_id uuid)
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
    where org_id = target_org_id
      and user_id = caller_id
      and role in ('org_admin', 'staff')
  ) then
    raise exception 'Only organization admins and staff can post announcements' using errcode = 'insufficient_privilege';
  end if;

  perform pg_advisory_xact_lock(hashtextextended(target_org_id::text, 0));

  select id into conversation_id
  from public.conversations
  where org_id = target_org_id and type = 'announcement';

  if conversation_id is null then
    insert into public.conversations (org_id, type, title, created_by)
    values (target_org_id, 'announcement', 'Announcements', caller_id)
    returning id into conversation_id;

    perform private.record_audit_event(target_org_id, 'announcements.enabled', 'conversation', conversation_id);
  end if;

  insert into public.conversation_participants (org_id, conversation_id, user_id)
  select target_org_id, conversation_id, member.user_id
  from public.org_members as member
  where member.org_id = target_org_id
  on conflict do nothing;

  return conversation_id;
end;
$$;

revoke execute on function public.create_announcement_conversation(uuid) from public, anon;
grant execute on function public.create_announcement_conversation(uuid) to authenticated;

create function private.add_member_to_announcements()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.conversation_participants (org_id, conversation_id, user_id)
  select new.org_id, conversation.id, new.user_id
  from public.conversations as conversation
  where conversation.org_id = new.org_id and conversation.type = 'announcement'
  on conflict do nothing;
  return new;
end;
$$;

create trigger org_members_add_to_announcements
  after insert on public.org_members
  for each row execute function private.add_member_to_announcements();
