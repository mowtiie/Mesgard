-- Blank the body of soft deleted messages and archive the original for organization admins.

create or replace function private.stamp_message_edit()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.body is distinct from old.body and new.deleted_at is null then
    new.edited_at = now();
  end if;
  return new;
end;
$$;

create table public.deleted_message_bodies (
  message_id uuid primary key,
  org_id uuid not null,
  body text not null,
  deleted_by uuid not null references public.profiles (id),
  deleted_at timestamptz not null default now(),
  foreign key (org_id, message_id) references public.messages (org_id, id) on delete cascade
);

alter table public.messages drop constraint messages_body_check;
alter table public.messages add constraint messages_body_length_check
  check (char_length(body) <= 4000 and (deleted_at is not null or char_length(body) >= 1));

insert into public.deleted_message_bodies (message_id, org_id, body, deleted_by, deleted_at)
select id, org_id, body, sender_id, deleted_at
from public.messages
where deleted_at is not null and body <> '';

update public.messages set body = '' where deleted_at is not null and body <> '';

create function private.hide_deleted_message_body()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if old.deleted_at is not null then
    if new.deleted_at is distinct from old.deleted_at or new.body is distinct from old.body then
      raise exception 'A deleted message cannot be changed' using errcode = 'restrict_violation';
    end if;
    return new;
  end if;
  if new.deleted_at is null then
    return new;
  end if;
  insert into public.deleted_message_bodies (message_id, org_id, body, deleted_by)
  values (old.id, old.org_id, old.body, coalesce((select auth.uid()), old.sender_id));
  new.body = '';
  return new;
end;
$$;

create trigger messages_hide_deleted_body
  before update on public.messages
  for each row execute function private.hide_deleted_message_body();

alter table public.deleted_message_bodies enable row level security;

revoke all on public.deleted_message_bodies from anon, authenticated;
grant select on public.deleted_message_bodies to authenticated;

create policy "Organization admins can read the original text of deleted messages"
  on public.deleted_message_bodies for select to authenticated
  using (private.has_org_role(org_id, array['org_admin']));
