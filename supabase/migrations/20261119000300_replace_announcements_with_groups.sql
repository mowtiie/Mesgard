-- Announcements are an ordinary group: an administrator creates a group, names it, and chooses who can post. A group can
-- limit posting to its moderators (organization admins always can post), and a group can include everyone, which adds
-- every current member and every new one. The announcement conversation type and its automatic creation go away; an
-- existing announcement conversation becomes such a group with its messages, pins and read marks. api_level is 7.

alter table public.groups
  add column posting_policy text not null default 'everyone' check (posting_policy in ('everyone', 'moderators')),
  add column includes_everyone boolean not null default false;

grant insert (posting_policy, includes_everyone) on public.groups to authenticated;
grant update (posting_policy, includes_everyone) on public.groups to authenticated;

create function private.group_role_for(organization_role text)
returns text
language sql
immutable
set search_path = ''
as $$
  select case when organization_role in ('org_admin', 'staff') then 'moderator' else 'member' end;
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
  );
$$;

-- A group that includes everyone has a conversation from the start, and every member of the organization in it.
create function private.add_everyone_to_group()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform pg_advisory_xact_lock(hashtextextended(new.id::text, 0));

  insert into public.conversations (org_id, type, group_id, title, created_by)
  select new.org_id, 'group', new.id, new.name, creator.user_id
  from (
    select member.user_id
    from public.org_members as member
    where member.org_id = new.org_id
    order by (member.role = 'org_admin') desc, member.created_at asc
    limit 1
  ) as creator
  where not exists (
    select 1 from public.conversations as existing where existing.group_id = new.id and existing.type = 'group'
  );

  insert into public.group_members (org_id, group_id, user_id, role_in_group)
  select member.org_id, new.id, member.user_id, private.group_role_for(member.role)
  from public.org_members as member
  where member.org_id = new.org_id
  on conflict do nothing;

  return null;
end;
$$;

create trigger groups_add_everyone_on_insert
  after insert on public.groups
  for each row
  when (new.includes_everyone)
  execute function private.add_everyone_to_group();

create trigger groups_add_everyone_on_update
  after update of includes_everyone on public.groups
  for each row
  when (new.includes_everyone and not old.includes_everyone)
  execute function private.add_everyone_to_group();

create function private.add_member_to_everyone_groups()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.group_members (org_id, group_id, user_id, role_in_group)
  select new.org_id, grp.id, new.user_id, private.group_role_for(new.role)
  from public.groups as grp
  where grp.org_id = new.org_id and grp.includes_everyone
  on conflict do nothing;
  return null;
end;
$$;

drop trigger org_members_add_to_announcements on public.org_members;
drop function private.add_member_to_announcements_creating_them();

create trigger org_members_add_to_everyone_groups
  after insert on public.org_members
  for each row execute function private.add_member_to_everyone_groups();

-- Existing announcement conversations become groups that include everyone and limit posting to moderators.
do $$
declare
  announcement record;
  new_group_id uuid;
begin
  for announcement in select id, org_id, title from public.conversations where type = 'announcement' loop
    insert into public.groups (org_id, name, posting_policy)
    values (announcement.org_id, coalesce(announcement.title, 'Announcements'), 'moderators')
    returning id into new_group_id;

    update public.conversations set type = 'group', group_id = new_group_id where id = announcement.id;
    update public.groups set includes_everyone = true where id = new_group_id;
  end loop;
end;
$$;

drop index public.conversations_announcement_org_key;

drop policy "Members can create group and announcement conversations as themselves" on public.conversations;

create policy "Members can create group conversations as themselves"
  on public.conversations for insert to authenticated
  with check (
    created_by = (select auth.uid())
    and type = 'group'
    and private.is_org_member(org_id)
  );

delete from public.conversation_types where type = 'announcement';

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 7);
$$;
