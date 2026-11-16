-- Let signed in users join the private Realtime channel that carries message changes. Row Level Security on public.messages
-- still decides which changes each person receives, and nobody can send on this channel.

create policy "Signed in users can join the message sync channel"
  on realtime.messages for select to authenticated
  using (
    extension = 'broadcast'
    and (select realtime.topic()) = 'messages-sync'
  );
