-- The conversation list tells the app which group a conversation belongs to and which picture the group has, so the
-- list can show group pictures without a request for each group. api_level stays 27.

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
  group_path text,
  group_id uuid,
  group_avatar_version integer
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
      case when conversation.group_id is not null then private.group_path(conversation.group_id) end as group_path,
      conversation.group_id,
      (select grp.avatar_version from public.groups as grp where grp.id = conversation.group_id) as group_avatar_version
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
