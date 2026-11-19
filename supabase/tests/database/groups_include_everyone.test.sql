begin;
select plan(9);

-- A group that includes everyone has a conversation from the start, every member in it, and new members join it.
insert into public.organizations (id, name, slug)
values ('00000000-0000-4000-8000-00000000f001', 'New School', 'new-school');
insert into public.org_members (org_id, user_id, role) values
  ('00000000-0000-4000-8000-00000000f001', tests.seed_id('20', 'a', 1), 'org_admin'),
  ('00000000-0000-4000-8000-00000000f001', tests.seed_id('20', 'a', 2), 'staff'),
  ('00000000-0000-4000-8000-00000000f001', tests.seed_id('20', 'a', 4), 'member');

select is(
  (select count(*)::int from public.conversations where org_id = '00000000-0000-4000-8000-00000000f001'),
  0,
  'an organization starts without any conversation'
);

insert into public.groups (id, org_id, name, posting_policy, includes_everyone)
values ('30000000-0000-4000-8000-00000000f001', '00000000-0000-4000-8000-00000000f001', 'Announcements', 'moderators', true);

select is(
  (select count(*)::int from public.conversations
   where group_id = '30000000-0000-4000-8000-00000000f001' and type = 'group' and title = 'Announcements'),
  1,
  'the group gets its conversation'
);
select is(
  (select count(*)::int from public.group_members where group_id = '30000000-0000-4000-8000-00000000f001'),
  3,
  'every member of the organization is in the group'
);
select is(
  (select array_agg(user_id order by user_id) from public.group_members
   where group_id = '30000000-0000-4000-8000-00000000f001' and role_in_group = 'moderator'),
  array[tests.seed_id('20', 'a', 1), tests.seed_id('20', 'a', 2)],
  'organization admins and staff are the moderators'
);
select is(
  (select count(*)::int from public.conversation_participants as participant
   join public.conversations as conversation on conversation.id = participant.conversation_id
   where conversation.group_id = '30000000-0000-4000-8000-00000000f001'),
  3,
  'everyone takes part in the conversation'
);

insert into public.org_members (org_id, user_id, role)
values ('00000000-0000-4000-8000-00000000f001', tests.seed_id('20', 'a', 5), 'member');

select is(
  (select role_in_group from public.group_members
   where group_id = '30000000-0000-4000-8000-00000000f001' and user_id = tests.seed_id('20', 'a', 5)),
  'member',
  'a new member joins the group'
);
select is(
  (select count(*)::int from public.conversation_participants as participant
   join public.conversations as conversation on conversation.id = participant.conversation_id
   where conversation.group_id = '30000000-0000-4000-8000-00000000f001' and participant.user_id = tests.seed_id('20', 'a', 5)),
  1,
  'and its conversation'
);

-- Turning the setting on for an existing group adds the people already in the organization.
insert into public.groups (id, org_id, name)
values ('30000000-0000-4000-8000-00000000f002', '00000000-0000-4000-8000-00000000f001', 'Staff room');
select is((select count(*)::int from public.group_members where group_id = '30000000-0000-4000-8000-00000000f002'), 0, 'a plain group starts empty');
update public.groups set includes_everyone = true where id = '30000000-0000-4000-8000-00000000f002';
select is(
  (select count(*)::int from public.group_members where group_id = '30000000-0000-4000-8000-00000000f002'),
  4,
  'including everyone adds the members who are already there'
);

select * from finish();
rollback;
