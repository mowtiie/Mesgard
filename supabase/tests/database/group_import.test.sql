begin;
select plan(11);

create temporary table seed_emails as select id, email from auth.users;
grant select on seed_emails to authenticated;

-- Organization A: a01 is the admin, a02 is staff and a04 a member. Their emails come from the seed.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format($$select public.import_groups(%L, '[{"line": 2, "group": "Grade 11"}]'::jsonb)$$, tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'a member cannot import groups'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
create temporary table import_result (result jsonb);
grant all on import_result to authenticated;

insert into import_result
select public.import_groups(
  tests.seed_id('10', 'a', 0),
  jsonb_build_array(
    jsonb_build_object('line', 2, 'group', 'Grade 11 Test', 'kind', 'class'),
    jsonb_build_object('line', 3, 'group', 'STEM Test', 'parent', 'Grade 11 Test', 'kind', 'section',
      'email', (select email from seed_emails where id = tests.seed_id('20', 'a', 4)), 'role', 'member'),
    jsonb_build_object('line', 4, 'group', 'Grade 11 Test',
      'email', (select email from seed_emails where id = tests.seed_id('20', 'a', 2)), 'role', 'owner'),
    jsonb_build_object('line', 5, 'group', 'Lonely', 'email', 'nobody@nowhere.example'),
    jsonb_build_object('line', 6, 'group', 'Orphan Test', 'parent', 'No Such Group'),
    jsonb_build_object('line', 7, 'group', 'Bad Role', 'email', 'x@y.example', 'role', 'boss')
  )
);

select is(
  (select (result ->> 'created_groups')::int from import_result),
  3,
  'three groups were created: Grade 11 Test, STEM Test and Lonely'
);
select is(
  (select (result ->> 'added_members')::int from import_result),
  2,
  'two people were added'
);
select is(
  (select parent.name from public.groups as child join public.groups as parent on parent.id = child.parent_id
   where child.name = 'STEM Test'),
  'Grade 11 Test',
  'STEM Test is inside Grade 11 Test, which the file named only by its name'
);
select is(
  (select kind from public.groups where name = 'STEM Test'),
  'section',
  'and has the kind of the file'
);
select is(
  (select role_in_group from public.group_members as membership
   join public.groups as grp on grp.id = membership.group_id
   where grp.name = 'Grade 11 Test' and membership.user_id = tests.seed_id('20', 'a', 2)),
  'owner',
  'a person gets the role of the file'
);
select is(
  (select result -> 'rows' -> 3 ->> 'member' from import_result),
  'not_found',
  'an email that belongs to nobody in the organization is reported, the group is still made'
);
select ok(
  (select (result -> 'rows' -> 4 ->> 'error') like '%No Such Group%' from import_result),
  'a parent that does not exist is an error on that row only'
);
select ok(
  (select (result -> 'rows' -> 5 ->> 'error') like '%role%' from import_result),
  'and so is a role that does not exist'
);

delete from import_result;
insert into import_result
select public.import_groups(
  tests.seed_id('10', 'a', 0),
  jsonb_build_array(
    jsonb_build_object('line', 2, 'group', 'Grade 11 Test', 'kind', 'class'),
    jsonb_build_object('line', 3, 'group', 'STEM Test', 'parent', 'Grade 11 Test',
      'email', (select email from seed_emails where id = tests.seed_id('20', 'a', 4)))
  )
);
select is(
  (select (result ->> 'created_groups')::int + (result ->> 'added_members')::int from import_result),
  0,
  'running the file again changes nothing'
);

reset role;
select is(
  (select count(*)::int from public.audit_logs where action = 'group.imported'),
  2,
  'each import is recorded in the audit log'
);

select * from finish();
rollback;
