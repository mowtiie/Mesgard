-- Group owners: an owner can rename the group, add and remove members, change roles and post even when posting is
-- limited to moderators. Anyone can leave a group, except the last owner. Organization admins keep every right they
-- had. Moving a group in the tree stays with the admins. api_level is 16.

insert into public.group_roles (role) values ('owner');

create function private.is_group_owner(target_group_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.group_members
    where group_id = target_group_id
      and user_id = (select auth.uid())
      and role_in_group = 'owner'
  );
$$;

create policy "Group owners can rename their group"
  on public.groups for update to authenticated
  using (private.is_group_owner(id))
  with check (private.is_group_owner(id));

create function private.keep_moves_with_admins()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.parent_id is distinct from old.parent_id
     and (select auth.uid()) is not null
     and not private.has_org_role(new.org_id, array['org_admin']) then
    raise exception 'Only an organization admin can move a group' using errcode = 'insufficient_privilege';
  end if;
  return new;
end;
$$;

create trigger groups_keep_moves_with_admins
  before update of parent_id on public.groups
  for each row execute function private.keep_moves_with_admins();

create policy "Group owners can add members and moderators"
  on public.group_members for insert to authenticated
  with check (private.is_group_owner(group_id) and role_in_group in ('member', 'moderator'));

create policy "Group owners can change member roles"
  on public.group_members for update to authenticated
  using (private.is_group_owner(group_id))
  with check (private.is_group_owner(group_id));

create policy "Group owners can remove members"
  on public.group_members for delete to authenticated
  using (private.is_group_owner(group_id));

create policy "Members can leave a group"
  on public.group_members for delete to authenticated
  using (user_id = (select auth.uid()));

create function private.keep_a_group_owner()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  -- A cascade (a group or a member removed from the organization) is not someone leaving.
  if pg_trigger_depth() > 1 then
    return case when tg_op = 'DELETE' then old else new end;
  end if;
  if old.role_in_group = 'owner'
     and old.user_id = (select auth.uid())
     and (tg_op = 'DELETE' or new.role_in_group <> 'owner')
     and not exists (
       select 1
       from public.group_members
       where group_id = old.group_id and role_in_group = 'owner' and user_id <> old.user_id
     )
  then
    raise exception 'The last owner cannot leave the group' using errcode = 'check_violation';
  end if;
  return case when tg_op = 'DELETE' then old else new end;
end;
$$;

create trigger group_members_keep_an_owner
  before update of role_in_group or delete on public.group_members
  for each row execute function private.keep_a_group_owner();

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
            and group_member.role_in_group in ('moderator', 'owner')
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

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 16);
$$;
