-- FCM device tokens, readable and writable only by their owner.

create table public.device_platforms (
  platform text primary key
);

insert into public.device_platforms (platform) values ('android');

create table public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  fcm_token text not null check (char_length(fcm_token) > 0),
  platform text not null default 'android' references public.device_platforms (platform),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, fcm_token)
);

create trigger device_tokens_set_updated_at
  before update on public.device_tokens
  for each row execute function private.set_updated_at();

alter table public.device_platforms enable row level security;
alter table public.device_tokens enable row level security;

revoke all on public.device_platforms, public.device_tokens from anon, authenticated;

grant select on public.device_platforms to authenticated;
grant select, delete on public.device_tokens to authenticated;
grant insert (user_id, fcm_token, platform) on public.device_tokens to authenticated;
grant update (fcm_token) on public.device_tokens to authenticated;

create policy "Authenticated users can read device platforms"
  on public.device_platforms for select to authenticated
  using (true);

create policy "Users can read their own device tokens"
  on public.device_tokens for select to authenticated
  using (user_id = (select auth.uid()));

create policy "Users can register their own device tokens"
  on public.device_tokens for insert to authenticated
  with check (user_id = (select auth.uid()));

create policy "Users can update their own device tokens"
  on public.device_tokens for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

create policy "Users can delete their own device tokens"
  on public.device_tokens for delete to authenticated
  using (user_id = (select auth.uid()));
