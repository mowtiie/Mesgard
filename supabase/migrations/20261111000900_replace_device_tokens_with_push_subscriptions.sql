-- Replace FCM device tokens with UnifiedPush subscriptions: a Web Push endpoint and the key set used to encrypt to it.

drop function public.register_device_token(text);
drop function public.list_push_deliveries(uuid);

-- FCM tokens cannot be used with UnifiedPush, so existing rows are removed.
delete from public.device_tokens;

alter table public.device_tokens rename to push_subscriptions;
alter table public.push_subscriptions rename column fcm_token to endpoint;

alter table public.push_subscriptions rename constraint device_tokens_pkey to push_subscriptions_pkey;
alter table public.push_subscriptions rename constraint device_tokens_user_id_fkey to push_subscriptions_user_id_fkey;
alter table public.push_subscriptions rename constraint device_tokens_platform_fkey to push_subscriptions_platform_fkey;
alter table public.push_subscriptions
  rename constraint device_tokens_user_id_fcm_token_key to push_subscriptions_user_id_endpoint_key;
alter table public.push_subscriptions drop constraint device_tokens_fcm_token_check;
alter trigger device_tokens_set_updated_at on public.push_subscriptions rename to push_subscriptions_set_updated_at;

alter table public.push_subscriptions
  add column p256dh text not null check (char_length(p256dh) between 16 and 128),
  add column auth_secret text not null check (char_length(auth_secret) between 16 and 64),
  add constraint push_subscriptions_endpoint_check
    check (char_length(endpoint) <= 1000 and endpoint ~ '^https?://[^[:space:]]+$');

drop policy "Users can register their own device tokens" on public.push_subscriptions;
drop policy "Users can update their own device tokens" on public.push_subscriptions;
alter policy "Users can read their own device tokens" on public.push_subscriptions
  rename to "Users can read their own push subscriptions";
alter policy "Users can delete their own device tokens" on public.push_subscriptions
  rename to "Users can delete their own push subscriptions";

-- Subscriptions are only created through register_push_subscription.
revoke insert, update on public.push_subscriptions from authenticated;

create function public.register_push_subscription(new_endpoint text, new_p256dh text, new_auth_secret text)
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

  delete from public.push_subscriptions
  where endpoint = new_endpoint and user_id <> caller_id;

  insert into public.push_subscriptions (user_id, endpoint, p256dh, auth_secret)
  values (caller_id, new_endpoint, new_p256dh, new_auth_secret)
  on conflict (user_id, endpoint)
  do update set p256dh = excluded.p256dh, auth_secret = excluded.auth_secret;
end;
$$;

revoke execute on function public.register_push_subscription(text, text, text) from public, anon;
grant execute on function public.register_push_subscription(text, text, text) to authenticated;

-- The text of a message is only returned for recipients whose preview mode allows it, and the sender only when the
-- mode is not hidden, so send-push never handles content it must not forward.
create function public.list_push_deliveries(target_message_id uuid)
returns table (
  endpoint text,
  p256dh text,
  auth_secret text,
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
   and not participant.muted
  join public.push_subscriptions as subscription on subscription.user_id = participant.user_id
  left join public.user_settings as settings on settings.user_id = participant.user_id
  cross join lateral (
    select coalesce(settings.notification_preview, organization.notification_preview_default) as preview
  ) as mode
  where message.id = target_message_id
    and message.deleted_at is null;
$$;

revoke execute on function public.list_push_deliveries(uuid) from public, anon, authenticated;
grant execute on function public.list_push_deliveries(uuid) to service_role;
