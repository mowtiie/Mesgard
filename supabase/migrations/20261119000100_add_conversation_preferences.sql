-- Per person conversation preferences: pin, archive, mark as unread and delete for me (hide the history until a new
-- message arrives). They live on the participant row, so nobody else sees them. api_level is 5.

alter table public.conversation_participants
  add column pinned_at timestamptz,
  add column archived_at timestamptz,
  add column marked_unread boolean not null default false,
  add column hidden_before timestamptz;

grant update (pinned_at, archived_at, marked_unread, hidden_before) on public.conversation_participants to authenticated;

create function private.limit_pinned_conversations()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if (
    select count(*) from public.conversation_participants as participant
    where participant.user_id = new.user_id and participant.pinned_at is not null
  ) > 3 then
    raise exception 'At most 3 conversations can be pinned' using errcode = 'check_violation';
  end if;
  return null;
end;
$$;

-- An after trigger runs once the whole statement is done, so pinning several conversations at once is counted too.
create trigger limit_pinned_conversations
  after update of pinned_at on public.conversation_participants
  for each row
  when (new.pinned_at is not null)
  execute function private.limit_pinned_conversations();

-- A new message brings an archived conversation back, unless the person muted it. It has to write other people's
-- participant rows, which they alone can update, so it runs with the rights of its owner.
create function private.unarchive_on_new_message()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  update public.conversation_participants
  set archived_at = null
  where conversation_id = new.conversation_id
    and archived_at is not null
    and not muted;
  return null;
end;
$$;

create trigger unarchive_on_new_message
  after insert on public.messages
  for each row
  execute function private.unarchive_on_new_message();

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
  hidden_before timestamptz
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
      membership.hidden_before
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
  select jsonb_build_object('api_level', 5);
$$;
