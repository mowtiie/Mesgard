-- Edit with a private archive of the earlier text, reply to a message, mentions, pinned messages and search in a
-- conversation. A mention reaches a person even when they muted the conversation. The app needs all of it, so
-- api_level is 4. The earlier versions of an edited message are readable by organization admins only, like the text
-- of a deleted message.

create extension if not exists pg_trgm with schema extensions;

-- Edit: the text before each change is archived.

create table public.message_edits (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null,
  message_id uuid not null,
  body text not null,
  edited_by uuid not null references public.profiles (id),
  edited_at timestamptz not null default now(),
  foreign key (org_id, message_id) references public.messages (org_id, id) on delete cascade
);

create index message_edits_message_id_idx on public.message_edits (message_id, edited_at desc);

create function private.archive_message_edit()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if old.deleted_at is null and new.deleted_at is null and new.body is distinct from old.body then
    insert into public.message_edits (org_id, message_id, body, edited_by)
    values (old.org_id, old.id, old.body, coalesce((select auth.uid()), old.sender_id));
  end if;
  return new;
end;
$$;

create trigger messages_archive_edit
  before update on public.messages
  for each row execute function private.archive_message_edit();

alter table public.message_edits enable row level security;
revoke all on public.message_edits from anon, authenticated;
grant select on public.message_edits to authenticated;

create policy "Organization admins can read the earlier versions of messages"
  on public.message_edits for select to authenticated
  using (private.has_org_role(org_id, array['org_admin']));

create policy "Require two-step verification" on public.message_edits as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));

-- Reply and mentions.

alter table public.messages
  add column reply_to_message_id uuid,
  add column mentioned_user_ids uuid[] not null default '{}' check (cardinality(mentioned_user_ids) <= 20),
  add constraint messages_conversation_id_id_key unique (conversation_id, id);

alter table public.messages
  add constraint messages_reply_to_message_fkey
  foreign key (conversation_id, reply_to_message_id) references public.messages (conversation_id, id)
  on delete set null (reply_to_message_id);

create index messages_reply_to_message_id_idx
  on public.messages (reply_to_message_id) where reply_to_message_id is not null;

grant insert (reply_to_message_id, mentioned_user_ids) on public.messages to authenticated;

create function private.check_message_mentions()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if cardinality(new.mentioned_user_ids) > 0 and exists (
    select 1
    from unnest(new.mentioned_user_ids) as mentioned (user_id)
    where not exists (
      select 1 from public.conversation_participants as participant
      where participant.conversation_id = new.conversation_id and participant.user_id = mentioned.user_id
    )
  ) then
    raise exception 'Only people in the conversation can be mentioned' using errcode = '22023';
  end if;
  return new;
end;
$$;

create trigger messages_check_mentions
  before insert on public.messages
  for each row execute function private.check_message_mentions();

drop function public.send_message(uuid, uuid, text, jsonb);

CREATE FUNCTION public.send_message(new_message_id uuid, target_conversation_id uuid, new_body text, new_attachments jsonb DEFAULT '[]'::jsonb, new_reply_to uuid DEFAULT NULL, new_mentions uuid[] DEFAULT '{}')
 RETURNS timestamp with time zone
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $$
declare
  sender uuid := (select auth.uid());
  conversation_org uuid;
  attachment jsonb;
  object_path text;
  object_row storage.objects;
  image_width integer;
  image_height integer;
  sent_at timestamptz;
begin
  perform private.require_mfa();
  if sender is null or not private.can_post_to_conversation(target_conversation_id) then
    raise exception 'Not allowed to post to this conversation' using errcode = '42501';
  end if;
  if jsonb_typeof(new_attachments) is distinct from 'array' or jsonb_array_length(new_attachments) > 10 then
    raise exception 'A message can carry at most 10 attachments' using errcode = '22023';
  end if;

  select org_id into conversation_org from public.conversations where id = target_conversation_id;

  insert into public.messages (
    id, org_id, conversation_id, sender_id, body, has_attachments, reply_to_message_id, mentioned_user_ids
  )
  values (
    new_message_id, conversation_org, target_conversation_id, sender, new_body,
    jsonb_array_length(new_attachments) > 0, new_reply_to, coalesce(new_mentions, '{}')
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

    -- The size is only a layout hint from the client, so it is kept for images and ignored when out of range.
    image_width := null;
    image_height := null;
    if (object_row.metadata ->> 'mimetype') like 'image/%'
      and (attachment ->> 'width') ~ '^[0-9]{1,5}$' and (attachment ->> 'height') ~ '^[0-9]{1,5}$'
      and (attachment ->> 'width')::integer between 1 and 20000
      and (attachment ->> 'height')::integer between 1 and 20000
    then
      image_width := (attachment ->> 'width')::integer;
      image_height := (attachment ->> 'height')::integer;
    end if;

    insert into public.attachments (id, org_id, message_id, storage_path, mime_type, size_bytes, file_name, width, height)
    values (
      coalesce((attachment ->> 'id')::uuid, gen_random_uuid()), conversation_org, new_message_id, object_path,
      object_row.metadata ->> 'mimetype', (object_row.metadata ->> 'size')::bigint,
      coalesce(attachment ->> 'file_name', 'attachment'), image_width, image_height
    )
    on conflict (storage_path) do nothing;
  end loop;

  return sent_at;
end;
$$;

revoke all on function public.send_message(uuid, uuid, text, jsonb, uuid, uuid[]) from public, anon;
grant execute on function public.send_message(uuid, uuid, text, jsonb, uuid, uuid[]) to authenticated;

CREATE OR REPLACE FUNCTION public.list_push_deliveries(target_message_id uuid)
 RETURNS TABLE(endpoint text, p256dh text, auth_secret text, recipient_id uuid, preview text, conversation_id uuid, conversation_type text, conversation_title text, sender_name text, body text)
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $$
  select
    subscription.endpoint,
    subscription.p256dh,
    subscription.auth_secret,
    participant.user_id as recipient_id,
    mode.preview,
    conversation.id as conversation_id,
    conversation.type as conversation_type,
    case when conversation.type = 'direct' then null else conversation.title end as conversation_title,
    case when mode.preview = 'hidden' then null else sender.display_name end as sender_name,
    case when mode.preview = 'sender_and_text' then message.body end as body
  from public.messages as message
  join public.conversations as conversation on conversation.id = message.conversation_id
  join public.organizations as organization on organization.id = conversation.org_id
  join public.profiles as sender on sender.id = message.sender_id
  join public.conversation_participants as participant
    on participant.conversation_id = conversation.id
   and participant.user_id <> message.sender_id
   and (not participant.muted or participant.user_id = any (message.mentioned_user_ids))
  join public.push_subscriptions as subscription on subscription.user_id = participant.user_id
  left join public.user_settings as settings on settings.user_id = participant.user_id
  cross join lateral (
    select coalesce(settings.notification_preview, organization.notification_preview_default) as preview
  ) as mode
  where message.id = target_message_id
    and message.deleted_at is null;
$$;

-- Pinned messages: administrators and staff pin up to five messages in a conversation they belong to.

create table public.pinned_messages (
  message_id uuid primary key,
  org_id uuid not null,
  conversation_id uuid not null,
  pinned_by uuid not null default auth.uid() references public.profiles (id),
  pinned_at timestamptz not null default now(),
  foreign key (conversation_id, message_id) references public.messages (conversation_id, id) on delete cascade,
  foreign key (org_id, conversation_id) references public.conversations (org_id, id) on delete cascade
);

create index pinned_messages_conversation_id_idx on public.pinned_messages (conversation_id, pinned_at desc);

create function private.limit_pinned_messages()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if (select count(*) from public.pinned_messages where conversation_id = new.conversation_id) >= 5 then
    raise exception 'A conversation can have at most 5 pinned messages' using errcode = '54000';
  end if;
  return new;
end;
$$;

create trigger pinned_messages_limit
  before insert on public.pinned_messages
  for each row execute function private.limit_pinned_messages();

create function private.audit_pinned_message()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    perform private.record_audit_event(
      new.org_id, 'message.pinned', 'message', new.message_id, jsonb_build_object('conversation_id', new.conversation_id)
    );
  else
    perform private.record_audit_event(
      old.org_id, 'message.unpinned', 'message', old.message_id, jsonb_build_object('conversation_id', old.conversation_id)
    );
  end if;
  return null;
end;
$$;

create trigger pinned_messages_audit
  after insert or delete on public.pinned_messages
  for each row execute function private.audit_pinned_message();

create function private.unpin_deleted_message()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  delete from public.pinned_messages where message_id = new.id;
  return null;
end;
$$;

create trigger messages_unpin_when_deleted
  after update of deleted_at on public.messages
  for each row when (old.deleted_at is null and new.deleted_at is not null)
  execute function private.unpin_deleted_message();

alter table public.pinned_messages enable row level security;
revoke all on public.pinned_messages from anon, authenticated;
grant select, delete on public.pinned_messages to authenticated;
grant insert (message_id, org_id, conversation_id) on public.pinned_messages to authenticated;

create policy "Participants can read the pinned messages of their conversations"
  on public.pinned_messages for select to authenticated
  using (private.is_conversation_participant(conversation_id));

create policy "Administrators and staff can pin a message in their conversations"
  on public.pinned_messages for insert to authenticated
  with check (
    private.has_org_role(org_id, array['org_admin', 'staff'])
    and private.is_conversation_participant(conversation_id)
    and exists (
      select 1 from public.messages as message where message.id = message_id and message.deleted_at is null
    )
  );

create policy "Administrators and staff can unpin a message in their conversations"
  on public.pinned_messages for delete to authenticated
  using (
    private.has_org_role(org_id, array['org_admin', 'staff'])
    and private.is_conversation_participant(conversation_id)
  );

create policy "Require two-step verification" on public.pinned_messages as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));

-- Search in a conversation: a part of a word is enough, and the person only finds what Row Level Security lets them read.

create index messages_body_trigram_idx on public.messages using gin (body extensions.gin_trgm_ops);

create function public.search_messages(
  target_conversation_id uuid,
  search_text text,
  page_size integer default 30,
  before_created_at timestamptz default null,
  before_message_id uuid default null
)
returns table (message_id uuid, sender_name text, body text, created_at timestamptz)
language plpgsql
stable
security invoker
set search_path = ''
as $$
begin
  if char_length(trim(search_text)) < 2 then
    raise exception 'Search for at least 2 characters' using errcode = '22023';
  end if;
  return query
  select message.id, profile.display_name, message.body, message.created_at
  from public.messages as message
  join public.profiles as profile on profile.id = message.sender_id
  where message.conversation_id = target_conversation_id
    and message.deleted_at is null
    and message.body ilike '%' || replace(replace(replace(trim(search_text), '\', '\\'), '%', '\%'), '_', '\_') || '%'
    and (before_created_at is null or (message.created_at, message.id) < (before_created_at, before_message_id))
  order by message.created_at desc, message.id desc
  limit least(greatest(page_size, 1), 50);
end;
$$;

revoke execute on function public.search_messages(uuid, text, integer, timestamptz, uuid) from public, anon;
grant execute on function public.search_messages(uuid, text, integer, timestamptz, uuid) to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 4);
$$;
