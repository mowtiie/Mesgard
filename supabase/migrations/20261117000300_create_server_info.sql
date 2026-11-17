-- Tells the app which version of the server API it talks to, so it can say when the server needs an update. Raise
-- api_level in a new migration whenever the app starts to need something the older server does not have.
-- It holds no data, so anyone may call it, even before signing in.

create function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 1);
$$;

revoke execute on function public.server_info() from public;
grant execute on function public.server_info() to anon, authenticated;
