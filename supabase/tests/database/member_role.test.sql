begin;
select plan(4);

select is(
  (select array_agg(role order by role) from public.org_roles),
  array['member', 'org_admin', 'staff'],
  'the roles are member, org_admin and staff'
);

select is(
  (select column_default from information_schema.columns
   where table_schema = 'public' and table_name = 'org_members' and column_name = 'role'),
  '''member''::text',
  'a person added without a role becomes a member'
);

select throws_ok(
  format(
    $$insert into public.org_members (org_id, user_id, role) values (%L, %L, 'student')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 3)
  ),
  '23503',
  null,
  'the old role name is no longer accepted'
);

select is(
  (select count(*)::int from public.org_members where role = 'member'),
  6,
  'the seeded people who were students are members'
);

select * from finish();
rollback;
