-- Mark a conversation as read up to a message, updating the read marker and read receipts atomically.

create function public.mark_conversation_read(target_conversation_id uuid, up_to_message_id uuid)
returns void
language plpgsql
security invoker
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  marker_time timestamptz;
  previous_time timestamptz;
begin
  select message.created_at into marker_time
  from public.messages as message
  where message.id = up_to_message_id and message.conversation_id = target_conversation_id;
  if marker_time is null then
    raise exception 'The message was not found in this conversation' using errcode = 'no_data_found';
  end if;

  select coalesce(previous.created_at, '-infinity') into previous_time
  from public.conversation_participants as participant
  left join public.messages as previous on previous.id = participant.last_read_message_id
  where participant.conversation_id = target_conversation_id and participant.user_id = caller_id;
  if not found then
    raise exception 'Only participants can mark a conversation as read' using errcode = 'insufficient_privilege';
  end if;

  if marker_time <= previous_time then
    return;
  end if;

  update public.conversation_participants
  set last_read_message_id = up_to_message_id
  where conversation_id = target_conversation_id and user_id = caller_id;

  insert into public.message_receipts (org_id, message_id, user_id)
  select message.org_id, message.id, caller_id
  from public.messages as message
  where message.conversation_id = target_conversation_id
    and message.sender_id <> caller_id
    and message.created_at > previous_time
    and message.created_at <= marker_time
  on conflict (message_id, user_id) do nothing;

  update public.message_receipts
  set read_at = now()
  where user_id = caller_id
    and read_at is null
    and message_id in (
      select message.id from public.messages as message
      where message.conversation_id = target_conversation_id and message.created_at <= marker_time
    );
end;
$$;

revoke execute on function public.mark_conversation_read(uuid, uuid) from public, anon;
grant execute on function public.mark_conversation_read(uuid, uuid) to authenticated;
