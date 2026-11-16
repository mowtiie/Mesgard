-- Every organization gets its announcement conversation without anyone having to enable it: it is created when the first
-- member joins (an organization has no one to own it before that) and every member joins it. Existing organizations are
-- brought up to date at the end.

create function private.add_member_to_announcements_creating_them()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform pg_advisory_xact_lock(hashtextextended(new.org_id::text, 0));

  insert into public.conversations (org_id, type, title, created_by)
  values (new.org_id, 'announcement', 'Announcements', new.user_id)
  on conflict (org_id) where type = 'announcement' do nothing;

  insert into public.conversation_participants (org_id, conversation_id, user_id)
  select new.org_id, conversation.id, new.user_id
  from public.conversations as conversation
  where conversation.org_id = new.org_id and conversation.type = 'announcement'
  on conflict do nothing;

  return new;
end;
$$;

drop trigger org_members_add_to_announcements on public.org_members;
drop function private.add_member_to_announcements();

create trigger org_members_add_to_announcements
  after insert on public.org_members
  for each row execute function private.add_member_to_announcements_creating_them();

insert into public.conversations (org_id, type, title, created_by)
select organization.id, 'announcement', 'Announcements', creator.user_id
from public.organizations as organization
cross join lateral (
  select member.user_id
  from public.org_members as member
  where member.org_id = organization.id
  order by (member.role = 'org_admin') desc, member.created_at asc
  limit 1
) as creator
where not exists (
  select 1 from public.conversations as existing
  where existing.org_id = organization.id and existing.type = 'announcement'
);

insert into public.conversation_participants (org_id, conversation_id, user_id)
select member.org_id, conversation.id, member.user_id
from public.conversations as conversation
join public.org_members as member on member.org_id = conversation.org_id
where conversation.type = 'announcement'
on conflict do nothing;
