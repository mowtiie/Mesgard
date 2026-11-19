-- Favorite contacts: people a person keeps at hand when they open Contacts. Each person sees only their own list.
-- api_level is 6.

create table public.favorite_contacts (
  org_id uuid not null,
  user_id uuid not null,
  favorite_user_id uuid not null,
  created_at timestamptz not null default now(),
  primary key (org_id, user_id, favorite_user_id),
  foreign key (org_id, user_id) references public.org_members (org_id, user_id) on delete cascade,
  foreign key (org_id, favorite_user_id) references public.org_members (org_id, user_id) on delete cascade,
  check (user_id <> favorite_user_id)
);

create index favorite_contacts_user_id_idx on public.favorite_contacts (user_id);

create function private.limit_favorite_contacts()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if (
    select count(*) from public.favorite_contacts as favorite
    where favorite.user_id = new.user_id and favorite.org_id = new.org_id
  ) >= 50 then
    raise exception 'At most 50 favorite contacts' using errcode = '54000';
  end if;
  return new;
end;
$$;

create trigger favorite_contacts_limit
  before insert on public.favorite_contacts
  for each row execute function private.limit_favorite_contacts();

alter table public.favorite_contacts enable row level security;

revoke all on public.favorite_contacts from anon, authenticated;
grant select, delete on public.favorite_contacts to authenticated;
grant insert (org_id, user_id, favorite_user_id) on public.favorite_contacts to authenticated;

create policy "People can read their own favorite contacts"
  on public.favorite_contacts for select to authenticated
  using (user_id = (select auth.uid()));

create policy "People can add their own favorite contacts"
  on public.favorite_contacts for insert to authenticated
  with check (user_id = (select auth.uid()));

create policy "People can remove their own favorite contacts"
  on public.favorite_contacts for delete to authenticated
  using (user_id = (select auth.uid()));

create policy "Require two-step verification" on public.favorite_contacts as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 6);
$$;
