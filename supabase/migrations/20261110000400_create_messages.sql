-- Messages with client-generated identifiers, soft delete and keyset pagination index.

create table public.messages (
  id uuid primary key,
  org_id uuid not null,
  conversation_id uuid not null,
  sender_id uuid not null references public.profiles (id),
  body text not null check (char_length(body) between 1 and 4000),
  created_at timestamptz not null default now(),
  edited_at timestamptz,
  deleted_at timestamptz,
  unique (org_id, id),
  foreign key (org_id, conversation_id) references public.conversations (org_id, id) on delete cascade
);

create index messages_conversation_id_created_at_idx
  on public.messages (conversation_id, created_at desc, id desc);

alter table public.conversation_participants
  add constraint conversation_participants_last_read_message_id_fkey
  foreign key (last_read_message_id) references public.messages (id) on delete set null;

create function private.stamp_message_edit()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.body is distinct from old.body then
    new.edited_at = now();
  end if;
  return new;
end;
$$;

create trigger messages_stamp_edit
  before update on public.messages
  for each row execute function private.stamp_message_edit();

alter table public.messages enable row level security;

revoke all on public.messages from anon, authenticated;

grant select on public.messages to authenticated;
grant insert (id, org_id, conversation_id, sender_id, body) on public.messages to authenticated;
grant update (body, deleted_at) on public.messages to authenticated;

create policy "Participants can read messages of their conversations"
  on public.messages for select to authenticated
  using (private.is_conversation_participant(conversation_id));

create policy "Participants can send messages as themselves"
  on public.messages for insert to authenticated
  with check (
    sender_id = (select auth.uid())
    and private.can_post_to_conversation(conversation_id)
  );

create policy "Senders can edit and delete their own messages"
  on public.messages for update to authenticated
  using (
    sender_id = (select auth.uid())
    and private.is_conversation_participant(conversation_id)
  )
  with check (sender_id = (select auth.uid()));

alter publication supabase_realtime add table public.messages;
