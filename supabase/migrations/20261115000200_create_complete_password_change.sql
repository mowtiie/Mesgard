-- An admin gives a new user a temporary password and marks the login with must_change_password. App metadata can only be
-- written on the server, so the signed in user clears the mark through this function after choosing a password.

create function public.complete_password_change()
returns void
language sql
security definer
set search_path = ''
as $$
  update auth.users
  set raw_app_meta_data = raw_app_meta_data - 'must_change_password'
  where id = (select auth.uid());
$$;

revoke execute on function public.complete_password_change() from public, anon;
grant execute on function public.complete_password_change() to authenticated;
