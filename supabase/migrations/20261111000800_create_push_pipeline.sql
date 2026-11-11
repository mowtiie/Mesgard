-- Push pipeline: device token registration, the deliveries of a message and the webhook that calls send-push.

create extension if not exists pg_net;

create function public.register_device_token(new_token text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
begin
  if caller_id is null then
    raise exception 'Only signed in users can register a device' using errcode = 'insufficient_privilege';
  end if;

  delete from public.device_tokens
  where fcm_token = new_token and user_id <> caller_id;

  insert into public.device_tokens (user_id, fcm_token)
  values (caller_id, new_token)
  on conflict (user_id, fcm_token) do nothing;
end;
$$;

revoke execute on function public.register_device_token(text) from public, anon;
grant execute on function public.register_device_token(text) to authenticated;

-- The text of a message is only returned for recipients whose preview mode allows it, and the sender only when the
-- mode is not hidden, so send-push never handles content it must not forward.
create function public.list_push_deliveries(target_message_id uuid)
returns table (
  fcm_token text,
  recipient_id uuid,
  preview text,
  conversation_id uuid,
  conversation_type text,
  conversation_title text,
  sender_name text,
  body text
)
language sql
stable
security definer
set search_path = ''
as $$
  select
    token.fcm_token,
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
   and not participant.muted
  join public.device_tokens as token on token.user_id = participant.user_id
  left join public.user_settings as settings on settings.user_id = participant.user_id
  cross join lateral (
    select coalesce(settings.notification_preview, organization.notification_preview_default) as preview
  ) as mode
  where message.id = target_message_id
    and message.deleted_at is null;
$$;

revoke execute on function public.list_push_deliveries(uuid) from public, anon, authenticated;
grant execute on function public.list_push_deliveries(uuid) to service_role;

create table private.push_settings (
  singleton boolean primary key default true check (singleton),
  function_url text not null check (char_length(function_url) > 0),
  webhook_secret text not null check (char_length(webhook_secret) >= 16)
);

alter table private.push_settings enable row level security;
revoke all on private.push_settings from public, anon, authenticated;

create function private.dispatch_push()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  settings private.push_settings;
begin
  select * into settings from private.push_settings;
  if not found then
    return new;
  end if;

  begin
    perform net.http_post(
      url := settings.function_url,
      headers := jsonb_build_object('Content-Type', 'application/json', 'x-webhook-secret', settings.webhook_secret),
      body := jsonb_build_object('message_id', new.id),
      timeout_milliseconds := 2000
    );
  exception when others then
    -- A failed push must never block sending a message.
    raise warning 'Push dispatch failed: %', sqlerrm;
  end;
  return new;
end;
$$;

create trigger messages_dispatch_push
  after insert on public.messages
  for each row execute function private.dispatch_push();
