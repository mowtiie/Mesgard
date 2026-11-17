-- Erasing the personal data of a person who left their last organization. The Edge Function admin-users removes the
-- membership, replaces the email and disables the login through the Auth API, then calls erase_user_data for the rest.
-- Each server decides what erasure means with: update private.erasure_settings set mode = '...';
--   off             nobody can be erased
--   keep_messages   personal data is erased, messages stay under the name Former member (default)
--   blank_messages  as keep_messages, and their messages are blanked as well

create table private.erasure_settings (
  singleton boolean primary key default true check (singleton),
  mode text not null default 'keep_messages' check (mode in ('off', 'keep_messages', 'blank_messages'))
);

insert into private.erasure_settings default values;

create function public.erasure_policy()
returns text
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  return (select mode from private.erasure_settings);
end;
$$;

revoke execute on function public.erasure_policy() from public, anon;
grant execute on function public.erasure_policy() to authenticated;

create function public.find_login_by_email(login_email text)
returns table (user_id uuid, is_disabled boolean, membership_count integer)
language sql
stable
security definer
set search_path = ''
as $$
  select
    users.id,
    coalesce(users.banned_until > now(), false),
    (select count(*)::int from public.org_members as member where member.user_id = users.id)
  from auth.users as users
  where lower(users.email) = lower(login_email);
$$;

revoke execute on function public.find_login_by_email(text) from public, anon, authenticated;
grant execute on function public.find_login_by_email(text) to service_role;

create function public.erase_user_data(target_user_id uuid, old_email text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  old_name text;
  placeholder_name constant text := 'Former member';
  placeholder_email text;
begin
  if (select mode from private.erasure_settings) = 'off' then
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
end;
$$;

revoke execute on function public.erase_user_data(uuid, text) from public, anon, authenticated;
grant execute on function public.erase_user_data(uuid, text) to service_role;
