-- Message receipts and attachments, with Row Level Security.

create function private.can_read_message(target_message_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.messages as message
    join public.conversation_participants as participant
      on participant.conversation_id = message.conversation_id
    where message.id = target_message_id
      and participant.user_id = (select auth.uid())
  );
$$;

create function private.is_message_sender(target_message_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.messages
    where id = target_message_id
      and sender_id = (select auth.uid())
  );
$$;

create table public.message_receipts (
  org_id uuid not null,
  message_id uuid not null,
  user_id uuid not null references public.profiles (id) on delete cascade,
  delivered_at timestamptz not null default now(),
  read_at timestamptz,
  primary key (message_id, user_id),
  foreign key (org_id, message_id) references public.messages (org_id, id) on delete cascade
);

create index message_receipts_user_id_idx on public.message_receipts (user_id);

create function private.stamp_message_read()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.read_at = coalesce(old.read_at, case when new.read_at is not null then now() end);
  return new;
end;
$$;

create trigger message_receipts_stamp_read
  before update on public.message_receipts
  for each row execute function private.stamp_message_read();

create table public.attachments (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null,
  message_id uuid not null,
  storage_path text not null unique,
  mime_type text not null,
  size_bytes bigint not null check (size_bytes > 0),
  created_at timestamptz not null default now(),
  foreign key (org_id, message_id) references public.messages (org_id, id) on delete cascade
);

create index attachments_message_id_idx on public.attachments (message_id);

alter table public.message_receipts enable row level security;
alter table public.attachments enable row level security;

revoke all on public.message_receipts, public.attachments from anon, authenticated;

grant select on public.message_receipts to authenticated;
grant insert (org_id, message_id, user_id) on public.message_receipts to authenticated;
grant update (read_at) on public.message_receipts to authenticated;
grant select, delete on public.attachments to authenticated;
grant insert (id, org_id, message_id, storage_path, mime_type, size_bytes)
  on public.attachments to authenticated;

create policy "Users can read their receipts and receipts of their messages"
  on public.message_receipts for select to authenticated
  using (user_id = (select auth.uid()) or private.is_message_sender(message_id));

create policy "Users can record receipts for messages they can read"
  on public.message_receipts for insert to authenticated
  with check (user_id = (select auth.uid()) and private.can_read_message(message_id));

create policy "Users can mark messages as read for themselves"
  on public.message_receipts for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

create policy "Participants can read attachments of messages they can read"
  on public.attachments for select to authenticated
  using (private.can_read_message(message_id));

create policy "Senders can add attachments to their messages"
  on public.attachments for insert to authenticated
  with check (private.is_message_sender(message_id));

create policy "Senders can remove attachments from their messages"
  on public.attachments for delete to authenticated
  using (private.is_message_sender(message_id));
