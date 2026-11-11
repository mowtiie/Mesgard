-- Set the notification preview of the signed-in user, updating only the preview column.

create function public.set_notification_preview(new_preview text)
returns void
language sql
security invoker
set search_path = ''
as $$
  insert into public.user_settings (user_id, notification_preview)
  values ((select auth.uid()), new_preview)
  on conflict (user_id) do update set notification_preview = excluded.notification_preview;
$$;

revoke execute on function public.set_notification_preview(text) from public, anon;
grant execute on function public.set_notification_preview(text) to authenticated;
