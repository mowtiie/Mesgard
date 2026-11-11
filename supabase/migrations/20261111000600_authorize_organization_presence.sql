-- Let only the members of an organization track and see presence on its Realtime channel.

create function private.organization_id_from_topic(topic text)
returns uuid
language sql
immutable
set search_path = ''
as $$
  select case
    when topic ~ '^org:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
      then substring(topic from 5)::uuid
  end;
$$;

create policy "Members can see presence in their organizations"
  on realtime.messages for select to authenticated
  using (
    extension = 'presence'
    and private.is_org_member(private.organization_id_from_topic((select realtime.topic())))
  );

create policy "Members can track presence in their organizations"
  on realtime.messages for insert to authenticated
  with check (
    extension = 'presence'
    and private.is_org_member(private.organization_id_from_topic((select realtime.topic())))
  );
