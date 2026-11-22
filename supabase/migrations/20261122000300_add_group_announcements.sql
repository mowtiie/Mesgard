-- Post to a group and everything under it: a parent group is only an audience (membership is never inherited), so an
-- announcement is delivered as one message in the conversation of the group and of every group inside it. Organization
-- admins and the owners and moderators of the parent group can send one. It is online only, text only, and idempotent on
-- the message id. api_level is 18.

create function public.send_to_group_tree(parent_group_id uuid, new_message_id uuid, new_body text)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  parent_group public.groups%rowtype;
  trimmed_body text := btrim(coalesce(new_body, ''));
  reached integer := 0;
  target record;
begin
  perform private.require_mfa();
  select * into parent_group from public.groups where id = parent_group_id;
  if not found then
    raise exception 'The group does not exist' using errcode = 'no_data_found';
  end if;
  if caller_id is null
     or not (
       private.has_org_role(parent_group.org_id, array['org_admin'])
       or exists (
         select 1 from public.group_members
         where group_id = parent_group_id and user_id = caller_id and role_in_group in ('owner', 'moderator')
       )
     ) then
    raise exception 'Only an organization admin, an owner or a moderator can send to a group and the groups inside it'
      using errcode = 'insufficient_privilege';
  end if;
  if trimmed_body = '' then
    raise exception 'A message needs text' using errcode = 'invalid_parameter_value';
  end if;
  if not private.has_org_role(parent_group.org_id, array['org_admin'])
     and private.quiet_hours_active(parent_group.org_id) then
    raise exception 'Messages cannot be sent during quiet hours' using errcode = 'insufficient_privilege';
  end if;

  for target in
    with recursive tree as (
      select id from public.groups where id = parent_group_id
      union all
      select child.id from public.groups as child join tree on child.parent_id = tree.id
    )
    select conversation.id as conversation_id, conversation.org_id, conversation.group_id
    from tree
    join public.conversations as conversation on conversation.group_id = tree.id and conversation.type = 'group'
    order by conversation.id
    limit 200
  loop
    insert into public.messages (id, org_id, conversation_id, sender_id, body)
    values (
      case
        when target.group_id = parent_group_id then new_message_id
        else md5(new_message_id::text || target.conversation_id::text)::uuid
      end,
      target.org_id, target.conversation_id, caller_id, trimmed_body
    )
    on conflict (id) do nothing;
    reached := reached + 1;
  end loop;

  if reached = 0 then
    raise exception 'No group here has a conversation yet' using errcode = 'no_data_found';
  end if;
  perform private.record_audit_event(
    parent_group.org_id, 'group.announced', 'group', parent_group_id,
    jsonb_build_object('conversations', reached)
  );
  return reached;
end;
$$;

revoke execute on function public.send_to_group_tree(uuid, uuid, text) from public, anon;
grant execute on function public.send_to_group_tree(uuid, uuid, text) to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 18);
$$;
