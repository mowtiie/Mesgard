-- Conversations and participants, with Row Level Security.

create table public.conversation_types (
  type text primary key
);

insert into public.conversation_types (type) values ('direct'), ('group'), ('announcement');

create table public.conversations (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations (id) on delete cascade,
  type text not null references public.conversation_types (type),
  group_id uuid,
  title text check (char_length(title) <= 200),
  created_by uuid not null default auth.uid() references public.profiles (id),
  created_at timestamptz not null default now(),
  unique (org_id, id),
  check (type <> 'direct' or group_id is null),
  foreign key (org_id, group_id) references public.groups (org_id, id) on delete cascade
);

create index conversations_org_id_group_id_idx on public.conversations (org_id, group_id);
create index conversations_created_by_idx on public.conversations (created_by);

create table public.conversation_participants (
  org_id uuid not null,
  conversation_id uuid not null,
  user_id uuid not null,
  last_read_message_id uuid,
  muted boolean not null default false,
  joined_at timestamptz not null default now(),
  primary key (conversation_id, user_id),
  foreign key (org_id, conversation_id) references public.conversations (org_id, id) on delete cascade,
  foreign key (org_id, user_id) references public.org_members (org_id, user_id) on delete cascade
);

create index conversation_participants_user_id_idx on public.conversation_participants (user_id);

create function private.is_conversation_participant(target_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.conversation_participants
    where conversation_id = target_conversation_id
      and user_id = (select auth.uid())
  );
$$;

create function private.is_conversation_creator(target_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.conversations
    where id = target_conversation_id
      and created_by = (select auth.uid())
  );
$$;

create function private.can_post_to_conversation(target_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.conversation_participants as participant
    join public.conversations as conversation on conversation.id = participant.conversation_id
    join public.org_members as member
      on member.org_id = conversation.org_id
     and member.user_id = participant.user_id
    where participant.conversation_id = target_conversation_id
      and participant.user_id = (select auth.uid())
      and (conversation.type <> 'announcement' or member.role in ('org_admin', 'staff'))
  );
$$;

alter table public.conversation_types enable row level security;
alter table public.conversations enable row level security;
alter table public.conversation_participants enable row level security;

revoke all on public.conversation_types, public.conversations, public.conversation_participants
  from anon, authenticated;

grant select on public.conversation_types to authenticated;
grant select on public.conversations to authenticated;
grant insert (id, org_id, type, group_id, title, created_by) on public.conversations to authenticated;
grant select, delete on public.conversation_participants to authenticated;
grant insert (org_id, conversation_id, user_id) on public.conversation_participants to authenticated;
grant update (muted, last_read_message_id) on public.conversation_participants to authenticated;

create policy "Authenticated users can read conversation types"
  on public.conversation_types for select to authenticated
  using (true);

create policy "Participants can read their conversations"
  on public.conversations for select to authenticated
  using (private.is_conversation_participant(id));

create policy "Members can create conversations as themselves"
  on public.conversations for insert to authenticated
  with check (
    created_by = (select auth.uid())
    and private.is_org_member(org_id)
    and (type <> 'announcement' or private.has_org_role(org_id, array['org_admin', 'staff']))
  );

create policy "Participants can read the participants of their conversations"
  on public.conversation_participants for select to authenticated
  using (private.is_conversation_participant(conversation_id));

create policy "Creators and organization admins can add participants"
  on public.conversation_participants for insert to authenticated
  with check (
    private.is_conversation_creator(conversation_id)
    or private.has_org_role(org_id, array['org_admin'])
  );

create policy "Participants can update their own participation settings"
  on public.conversation_participants for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

create policy "Participants can leave and organization admins can remove participants"
  on public.conversation_participants for delete to authenticated
  using (
    user_id = (select auth.uid())
    or private.has_org_role(org_id, array['org_admin'])
  );
