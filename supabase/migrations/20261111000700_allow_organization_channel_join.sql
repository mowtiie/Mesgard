-- Joining a Realtime channel also checks read access to its broadcast extension, so members need to read it on organization channels.

drop policy "Members can see presence in their organizations" on realtime.messages;

create policy "Members can join the channels of their organizations"
  on realtime.messages for select to authenticated
  using (
    extension in ('presence', 'broadcast')
    and private.is_org_member(private.organization_id_from_topic((select realtime.topic())))
  );
