-- send_message keeps the attachment ids chosen by the client so the device and the server describe the same rows.

create or replace function public.send_message(
  new_message_id uuid,
  target_conversation_id uuid,
  new_body text,
  new_attachments jsonb default '[]'
)
returns timestamptz
language plpgsql
security definer
set search_path = ''
as $$
declare
  sender uuid := (select auth.uid());
  conversation_org uuid;
  attachment jsonb;
  object_path text;
  object_row storage.objects;
  sent_at timestamptz;
begin
  if sender is null or not private.can_post_to_conversation(target_conversation_id) then
    raise exception 'Not allowed to post to this conversation' using errcode = '42501';
  end if;
  if jsonb_typeof(new_attachments) is distinct from 'array' or jsonb_array_length(new_attachments) > 10 then
    raise exception 'A message can carry at most 10 attachments' using errcode = '22023';
  end if;

  select org_id into conversation_org from public.conversations where id = target_conversation_id;

  insert into public.messages (id, org_id, conversation_id, sender_id, body, has_attachments)
  values (
    new_message_id, conversation_org, target_conversation_id, sender, new_body,
    jsonb_array_length(new_attachments) > 0
  )
  on conflict (id) do nothing;

  select created_at into sent_at from public.messages
  where id = new_message_id and sender_id = sender and conversation_id = target_conversation_id;
  if sent_at is null then
    raise exception 'Message id already used' using errcode = '23505';
  end if;

  for attachment in select * from jsonb_array_elements(new_attachments) loop
    object_path := attachment ->> 'storage_path';
    if object_path is null or object_path not like target_conversation_id::text || '/' || new_message_id::text || '/%' then
      raise exception 'Attachment path does not belong to this message' using errcode = '22023';
    end if;

    select * into object_row from storage.objects
    where bucket_id = 'message-attachments' and name = object_path and owner_id = sender::text;
    if not found then
      raise exception 'Attachment has not been uploaded' using errcode = '22023';
    end if;

    insert into public.attachments (id, org_id, message_id, storage_path, mime_type, size_bytes, file_name)
    values (
      coalesce((attachment ->> 'id')::uuid, gen_random_uuid()), conversation_org, new_message_id, object_path,
      object_row.metadata ->> 'mimetype', (object_row.metadata ->> 'size')::bigint,
      coalesce(attachment ->> 'file_name', 'attachment')
    )
    on conflict (storage_path) do nothing;
  end loop;

  return sent_at;
end;
$$;

revoke all on function public.send_message(uuid, uuid, text, jsonb) from public, anon;
grant execute on function public.send_message(uuid, uuid, text, jsonb) to authenticated;
