-- Let only the participants of a conversation send and receive broadcasts on its Realtime channel.

create function private.conversation_id_from_topic(topic text)
returns uuid
language sql
immutable
set search_path = ''
as $$
  select case
    when topic ~ '^conversation:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
      then substring(topic from 14)::uuid
  end;
$$;

create policy "Participants can receive broadcasts of their conversations"
  on realtime.messages for select to authenticated
  using (
    extension = 'broadcast'
    and private.is_conversation_participant(private.conversation_id_from_topic((select realtime.topic())))
  );

create policy "Participants can send broadcasts to their conversations"
  on realtime.messages for insert to authenticated
  with check (
    extension = 'broadcast'
    and private.is_conversation_participant(private.conversation_id_from_topic((select realtime.topic())))
  );
