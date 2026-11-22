begin;
select plan(8);

-- A group has a kind, only organization admins change it, and a conversation of a group says where the group sits.
insert into public.organizations (id, name, slug)
values ('00000000-0000-4000-8000-00000000b001', 'Kind School', 'kind-school');
insert into public.org_members (org_id, user_id, role) values
  ('00000000-0000-4000-8000-00000000b001', tests.seed_id('20', 'a', 1), 'org_admin'),
  ('00000000-0000-4000-8000-00000000b001', tests.seed_id('20', 'a', 2), 'staff'),
  ('00000000-0000-4000-8000-00000000b001', tests.seed_id('20', 'a', 4), 'member');

insert into public.groups (id, org_id, parent_id, name, kind, includes_everyone) values
  ('30000000-0000-4000-8000-0000000b0001', '00000000-0000-4000-8000-00000000b001', null, 'Grade 11', 'class', true);
insert into public.groups (id, org_id, parent_id, name, kind, includes_everyone) values
  ('30000000-0000-4000-8000-0000000b0002', '00000000-0000-4000-8000-00000000b001', '30000000-0000-4000-8000-0000000b0001', 'STEM', 'section', true);
insert into public.groups (id, org_id, parent_id, name) values
  ('30000000-0000-4000-8000-0000000b0003', '00000000-0000-4000-8000-00000000b001', null, 'Chess');

select is(
  (select kind from public.groups where id = '30000000-0000-4000-8000-0000000b0003'),
  'other',
  'a group is of the kind other until its kind is chosen'
);
select throws_ok(
  $$update public.groups set kind = 'house' where id = '30000000-0000-4000-8000-0000000b0003'$$,
  '23514',
  null,
  'only the known kinds are accepted'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select group_path from public.list_conversation_summaries() where title = 'STEM'),
  'Grade 11',
  'the conversation of a group inside another group says which one'
);
select is(
  (select group_path from public.list_conversation_summaries() where title = 'Grade 11'),
  null,
  'a group at the top has no path'
);

reset role;
insert into public.group_members (org_id, group_id, user_id, role_in_group) values
  ('00000000-0000-4000-8000-00000000b001', '30000000-0000-4000-8000-0000000b0003', tests.seed_id('20', 'a', 2), 'owner');

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  $$update public.groups set name = 'Chess Club' where id = '30000000-0000-4000-8000-0000000b0003'$$,
  'an owner can still rename the group'
);
select throws_ok(
  $$update public.groups set kind = 'club' where id = '30000000-0000-4000-8000-0000000b0003'$$,
  '42501',
  null,
  'an owner cannot change its kind'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  $$update public.groups set kind = 'club' where id = '30000000-0000-4000-8000-0000000b0003'$$,
  'an organization admin can'
);
select is(
  (select kind from public.groups where id = '30000000-0000-4000-8000-0000000b0003'),
  'club',
  'and the kind is saved'
);

select * from finish();
rollback;
