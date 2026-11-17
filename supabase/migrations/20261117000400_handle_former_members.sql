-- When someone leaves an organization, their messages stay and their conversations lose them as a participant.
-- A direct conversation with one person left cannot be posted to, and the names of the people who wrote the messages
-- a person can still read stay readable, so old messages do not lose their sender.

create or replace function private.can_post_to_conversation(target_conversation_id uuid)
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
      and (
        conversation.type <> 'direct'
        or (
          select count(*)
          from public.conversation_participants as everyone
          where everyone.conversation_id = conversation.id
        ) = 2
      )
  );
$$;

create index messages_sender_id_conversation_id_idx on public.messages (sender_id, conversation_id);

create function private.shares_conversation_with(other_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.messages as message
    join public.conversation_participants as mine on mine.conversation_id = message.conversation_id
    where message.sender_id = other_user_id
      and mine.user_id = (select auth.uid())
  );
$$;

revoke execute on function private.shares_conversation_with(uuid) from public, anon;
grant execute on function private.shares_conversation_with(uuid) to authenticated;

create policy "Users can read the profiles of the people who wrote the messages they can read"
  on public.profiles for select to authenticated
  using (private.shares_conversation_with(id));
