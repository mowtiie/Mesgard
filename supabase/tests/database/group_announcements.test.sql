begin;
select plan(7);

-- Grade 11 holds the groups STEM and ABM. Everyone in the organization is in all three, a02 owns Grade 11.
insert into public.organizations (id, name, slug)
values ('00000000-0000-4000-8000-00000000c001', 'Tree School', 'tree-school-c');
insert into public.org_members (org_id, user_id, role) values
  ('00000000-0000-4000-8000-00000000c001', tests.seed_id('20', 'a', 1), 'org_admin'),
  ('00000000-0000-4000-8000-00000000c001', tests.seed_id('20', 'a', 2), 'staff'),
  ('00000000-0000-4000-8000-00000000c001', tests.seed_id('20', 'a', 4), 'member');
insert into public.groups (id, org_id, parent_id, name, includes_everyone) values
  ('30000000-0000-4000-8000-0000000c0001', '00000000-0000-4000-8000-00000000c001', null, 'Grade 11', true);
insert into public.groups (id, org_id, parent_id, name, includes_everyone) values
  ('30000000-0000-4000-8000-0000000c0002', '00000000-0000-4000-8000-00000000c001', '30000000-0000-4000-8000-0000000c0001', 'STEM', true),
  ('30000000-0000-4000-8000-0000000c0003', '00000000-0000-4000-8000-00000000c001', '30000000-0000-4000-8000-0000000c0001', 'ABM', true);
update public.group_members set role_in_group = 'owner'
where group_id = '30000000-0000-4000-8000-0000000c0001' and user_id = tests.seed_id('20', 'a', 2);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  $$select public.send_to_group_tree('30000000-0000-4000-8000-0000000c0001', gen_random_uuid(), 'Hello')$$,
  '42501',
  null,
  'a member cannot send to a group and the groups inside it'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is(
  public.send_to_group_tree('30000000-0000-4000-8000-0000000c0001', '50000000-0000-4000-8000-0000000c0001', 'Exams start Monday'),
  3,
  'an owner reaches the group and both groups inside it'
);
select is(
  public.send_to_group_tree('30000000-0000-4000-8000-0000000c0001', '50000000-0000-4000-8000-0000000c0001', 'Exams start Monday'),
  3,
  'sending the same message again changes nothing'
);
select throws_ok(
  $$select public.send_to_group_tree('30000000-0000-4000-8000-0000000c0001', gen_random_uuid(), '   ')$$,
  '22023',
  null,
  'a message needs text'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.messages where body = 'Exams start Monday'),
  3,
  'a member of all three groups finds the message in each conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  public.send_to_group_tree('30000000-0000-4000-8000-0000000c0002', gen_random_uuid(), 'STEM only'),
  1,
  'a group with nothing inside it reaches only itself, and an organization admin can send to any group'
);

reset role;
select is(
  (select count(*)::int from public.audit_logs where action = 'group.announced'),
  3,
  'each send is recorded in the audit log, a repeat too'
);

select * from finish();
rollback;
