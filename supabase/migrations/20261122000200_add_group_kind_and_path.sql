-- Group kind and the path of a group: a group is a section, a class, a department, a club or something else, and a
-- conversation of a group says where the group sits ("Grade 11" for the group STEM inside it). Only organization admins
-- set the kind or move a group, an owner may still rename it. api_level is 17.

alter table public.groups
  add column kind text not null default 'other'
  check (kind in ('section', 'class', 'department', 'club', 'other'));

grant insert (kind) on public.groups to authenticated;
grant update (kind) on public.groups to authenticated;

create or replace function private.keep_moves_with_admins()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if (new.parent_id is distinct from old.parent_id or new.kind is distinct from old.kind)
     and (select auth.uid()) is not null
     and not private.has_org_role(new.org_id, array['org_admin']) then
    raise exception 'Only an organization admin can move a group or change its kind' using errcode = 'insufficient_privilege';
  end if;
  return new;
end;
$$;

drop trigger groups_keep_moves_with_admins on public.groups;
create trigger groups_keep_moves_with_admins
  before update of parent_id, kind on public.groups
  for each row execute function private.keep_moves_with_admins();

create function private.group_path(target_group_id uuid)
returns text
language sql
stable
set search_path = ''
as $$
  with recursive ancestors as (
    select id, parent_id, name, 0 as depth
    from public.groups
    where id = (select parent_id from public.groups where id = target_group_id)
    union all
    select g.id, g.parent_id, g.name, a.depth + 1
    from public.groups g
    join ancestors a on g.id = a.parent_id
  )
  select nullif(string_agg(name, ' > ' order by depth desc), '') from ancestors;
$$;

drop function public.list_conversation_summaries(integer, timestamptz);

create function public.list_conversation_summaries(
  page_size integer default 100,
  activity_before timestamptz default null
)
returns table (
  conversation_id uuid,
  org_id uuid,
  type text,
  title text,
  created_at timestamptz,
  unread_count bigint,
  can_post boolean,
  last_message_id uuid,
  last_message_sender_id uuid,
  last_message_body text,
  last_message_created_at timestamptz,
  activity_at timestamptz,
  pinned_at timestamptz,
  archived_at timestamptz,
  marked_unread boolean,
  hidden_before timestamptz,
  group_path text
)
language sql
stable
security invoker
set search_path = ''
as $$
  select summary.*
  from (
    select
      conversation.id as conversation_id,
      conversation.org_id,
      conversation.type,
      case
        when conversation.type = 'direct' then (
          select profile.display_name
          from public.conversation_participants as other
          join public.profiles as profile on profile.id = other.user_id
          where other.conversation_id = conversation.id
            and other.user_id <> (select auth.uid())
          limit 1
        )
        else conversation.title
      end as title,
      conversation.created_at,
      (
        select count(*)
        from public.messages as unread
        where unread.conversation_id = conversation.id
          and unread.sender_id <> (select auth.uid())
          and unread.deleted_at is null
          and unread.created_at > coalesce(membership.hidden_before, '-infinity')
          and unread.created_at > coalesce(
            (select read_marker.created_at from public.messages as read_marker
             where read_marker.id = membership.last_read_message_id),
            '-infinity'
          )
      ) as unread_count,
      private.can_post_to_conversation(conversation.id) as can_post,
      latest.id as last_message_id,
      latest.sender_id as last_message_sender_id,
      latest.body as last_message_body,
      latest.created_at as last_message_created_at,
      coalesce(latest.created_at, conversation.created_at) as activity_at,
      membership.pinned_at,
      membership.archived_at,
      membership.marked_unread,
      membership.hidden_before,
      case when conversation.group_id is not null then private.group_path(conversation.group_id) end as group_path
    from public.conversation_participants as membership
    join public.conversations as conversation on conversation.id = membership.conversation_id
    left join lateral (
      select message.id, message.sender_id, message.body, message.created_at
      from public.messages as message
      where message.conversation_id = conversation.id
        and message.deleted_at is null
      order by message.created_at desc, message.id desc
      limit 1
    ) as latest on true
    where membership.user_id = (select auth.uid())
      and (membership.hidden_before is null or latest.created_at > membership.hidden_before)
  ) as summary
  where activity_before is null or summary.activity_at < activity_before
  order by summary.activity_at desc, summary.conversation_id desc
  limit least(greatest(page_size, 1), 100);
$$;

revoke execute on function public.list_conversation_summaries(integer, timestamptz) from public, anon;
grant execute on function public.list_conversation_summaries(integer, timestamptz) to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 17);
$$;
