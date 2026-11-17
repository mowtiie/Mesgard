-- The blank_messages mode of erasure: the messages of an erased person are blanked like deleted messages, the original
-- text kept for administrators is purged, and the attachments go. The files in Storage cannot be deleted with SQL, so
-- admin-users removes them through the Storage API (it finds them with list_user_attachment_paths) before it calls
-- erase_user_data.

create function public.list_user_attachment_paths(target_user_id uuid)
returns setof text
language sql
stable
security definer
set search_path = ''
as $$
  select attachment.storage_path
  from public.attachments as attachment
  join public.messages as message on message.id = attachment.message_id and message.org_id = attachment.org_id
  where message.sender_id = target_user_id
  union
  select object.name
  from storage.objects as object
  where object.bucket_id = 'message-attachments' and object.owner_id = target_user_id::text;
$$;

revoke execute on function public.list_user_attachment_paths(uuid) from public, anon, authenticated;
grant execute on function public.list_user_attachment_paths(uuid) to service_role;

create or replace function public.erase_user_data(target_user_id uuid, old_email text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  erasure_mode text := (select mode from private.erasure_settings);
  old_name text;
  placeholder_name constant text := 'Former member';
  placeholder_email text;
begin
  if erasure_mode = 'off' then
    raise exception 'Erasure is turned off on this server' using errcode = 'insufficient_privilege';
  end if;
  if exists (select 1 from public.org_members where user_id = target_user_id) then
    raise exception 'Remove the person from every organization first' using errcode = 'insufficient_privilege';
  end if;

  select display_name into old_name from public.profiles where id = target_user_id;
  placeholder_email := 'erased-' || target_user_id || '@erased.invalid';

  update public.profiles
  set display_name = placeholder_name, avatar_path = null, status = null
  where id = target_user_id;
  delete from public.push_subscriptions where user_id = target_user_id;
  delete from public.user_settings where user_id = target_user_id;

  update auth.users set raw_user_meta_data = '{}'::jsonb, phone = null where id = target_user_id;
  delete from auth.sessions where user_id = target_user_id;
  delete from auth.mfa_factors where user_id = target_user_id;
  delete from auth.one_time_tokens where user_id = target_user_id;

  -- Auth's own log keeps the email, the name and the address of the device. Only the rows about this person change.
  update auth.audit_log_entries
  set
    payload = replace(
      replace(payload::text, lower(old_email), placeholder_email),
      coalesce(old_name, placeholder_name),
      placeholder_name
    )::json,
    ip_address = ''
  where payload::text ilike '%' || old_email || '%' or payload::text like '%' || target_user_id || '%';

  if erasure_mode = 'blank_messages' then
    -- Deleting a message archives its text for administrators, so the archive is purged for every message of the person.
    update public.messages set deleted_at = now() where sender_id = target_user_id and deleted_at is null;
    delete from public.deleted_message_bodies
    where message_id in (select id from public.messages where sender_id = target_user_id);
    delete from public.attachments
    where message_id in (select id from public.messages where sender_id = target_user_id);
  end if;
end;
$$;
