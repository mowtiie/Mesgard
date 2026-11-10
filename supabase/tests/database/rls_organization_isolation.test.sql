begin;
select plan(12);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  (select count(*)::int from public.org_members where org_id = tests.seed_id('10', 'b', 0)),
  0,
  'an organization admin cannot read the roster of another organization'
);
select is(
  (select count(*)::int from public.organizations where id = tests.seed_id('10', 'b', 0)),
  0,
  'an organization admin cannot read another organization'
);
select is(
  (select count(*)::int from public.groups where org_id = tests.seed_id('10', 'b', 0)),
  0,
  'an organization admin cannot read groups of another organization'
);
select is(
  (select count(*)::int from public.group_members where org_id = tests.seed_id('10', 'b', 0)),
  0,
  'an organization admin cannot read group members of another organization'
);
select is(
  (select count(*)::int from public.profiles where id = tests.seed_id('20', 'b', 4)),
  0,
  'an organization admin cannot read profiles of another organization'
);
select is(
  (select count(*)::int from public.audit_logs where org_id = tests.seed_id('10', 'b', 0)),
  0,
  'an organization admin cannot read the audit log of another organization'
);
select is(
  (select count(*)::int from public.audit_logs where org_id = tests.seed_id('10', 'a', 0)),
  1,
  'an organization admin can read the audit log of their own organization'
);
select throws_ok(
  format(
    $$insert into public.org_members (org_id, user_id, role) values (%L, %L, 'org_admin')$$,
    tests.seed_id('10', 'b', 0), tests.seed_id('20', 'a', 1)
  ),
  '42501',
  null,
  'an organization admin cannot add members to another organization'
);
select throws_ok(
  format(
    $$insert into public.audit_logs (org_id, action) values (%L, 'group.create')$$,
    tests.seed_id('10', 'b', 0)
  ),
  '42501',
  null,
  'an organization admin cannot write audit entries for another organization'
);
select is(
  (
    with changed as (
      update public.organizations set name = 'Hijacked' where id = tests.seed_id('10', 'b', 0)
      returning 1
    )
    select count(*)::int from changed
  ),
  0,
  'an organization admin cannot update another organization'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is(
  (select count(*)::int from public.audit_logs),
  0,
  'a staff member cannot read the audit log'
);

reset role;
select throws_ok(
  'update public.audit_logs set action = ''changed''',
  '23001',
  'audit_logs is append-only',
  'audit log entries cannot be modified'
);

select * from finish();
rollback;
