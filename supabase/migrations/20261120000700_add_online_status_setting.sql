-- Online status: a person chooses whether others see when they are online. Until they choose, administrators and staff
-- are shown and students are not (NPC Advisory 2024-03: privacy by design for children). The choice is stored here and
-- the app honors it by not announcing itself on the presence channel, so there is nothing on the server to enforce
-- beyond keeping the choice private to the person. api_level is 14.

alter table public.user_settings add column show_online_status boolean;

grant insert (show_online_status) on public.user_settings to authenticated;
grant update (show_online_status) on public.user_settings to authenticated;

create function public.set_online_status_visibility(new_visibility boolean)
returns void
language sql
security invoker
set search_path = ''
as $$
  insert into public.user_settings (user_id, show_online_status)
  values ((select auth.uid()), new_visibility)
  on conflict (user_id) do update set show_online_status = excluded.show_online_status;
$$;

revoke execute on function public.set_online_status_visibility(boolean) from public, anon;
grant execute on function public.set_online_status_visibility(boolean) to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 14);
$$;
