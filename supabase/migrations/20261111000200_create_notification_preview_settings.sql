-- Notification preview mode: an organization default and a per user choice, decided on the server by send-push.

create table public.notification_preview_modes (
  mode text primary key
);

insert into public.notification_preview_modes (mode) values ('sender_and_text'), ('sender_only'), ('hidden');

alter table public.organizations
  add column notification_preview_default text not null default 'sender_only'
  references public.notification_preview_modes (mode);

create table public.user_settings (
  user_id uuid primary key references public.profiles (id) on delete cascade,
  notification_preview text references public.notification_preview_modes (mode),
  updated_at timestamptz not null default now()
);

create trigger user_settings_set_updated_at
  before update on public.user_settings
  for each row execute function private.set_updated_at();

alter table public.notification_preview_modes enable row level security;
alter table public.user_settings enable row level security;

revoke all on public.notification_preview_modes, public.user_settings from anon, authenticated;

grant select on public.notification_preview_modes to authenticated;
grant select on public.user_settings to authenticated;
grant insert (user_id, notification_preview) on public.user_settings to authenticated;
grant update (notification_preview) on public.user_settings to authenticated;
grant update (notification_preview_default) on public.organizations to authenticated;

create policy "Authenticated users can read notification preview modes"
  on public.notification_preview_modes for select to authenticated
  using (true);

create policy "Users can read their own settings"
  on public.user_settings for select to authenticated
  using (user_id = (select auth.uid()));

create policy "Users can create their own settings"
  on public.user_settings for insert to authenticated
  with check (user_id = (select auth.uid()));

create policy "Users can update their own settings"
  on public.user_settings for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));
