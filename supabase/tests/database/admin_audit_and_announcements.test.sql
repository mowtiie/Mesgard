begin;
select plan(15);

-- A change made without a signed in user (a migration, a cascade, the service role) is not an admin action.
reset role;
insert into public.org_members (org_id, user_id, role)
values (tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 3), 'member');
select is(
  (select count(*)::int from public.audit_logs where action = 'member.added'),
  0,
  'a change without an actor is not recorded'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
insert into public.org_members (org_id, user_id, role)
values (tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 4), 'member');
select results_eq(
  format(
    $$select actor_id, target_id, metadata ->> 'role' from public.audit_logs
      where action = 'member.added' and target_id = %L$$,
    tests.seed_id('20', 'b', 4)
  ),
  format($$values (%L::uuid, %L::uuid, 'member')$$, tests.seed_id('20', 'a', 1), tests.seed_id('20', 'b', 4)),
  'adding a member records who did it and the role'
);

update public.org_members set role = 'staff'
where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'b', 4);
select results_eq(
  format(
    $$select metadata ->> 'from', metadata ->> 'to' from public.audit_logs
      where action = 'member.role_changed' and target_id = %L$$,
    tests.seed_id('20', 'b', 4)
  ),
  $$values ('member', 'staff')$$,
  'changing a role records the old and the new role'
);

update public.org_members set role = 'staff'
where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'b', 4);
select is(
  (select count(*)::int from public.audit_logs where action = 'member.role_changed'),
  1,
  'an update that keeps the role is not recorded'
);

delete from public.org_members
where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'b', 4);
select is(
  (select count(*)::int from public.audit_logs where action = 'member.removed' and target_id = tests.seed_id('20', 'b', 4)),
  1,
  'removing a member is recorded'
);

insert into public.groups (id, org_id, name)
values (tests.seed_id('30', 'a', 50), tests.seed_id('10', 'a', 0), 'Audit Club');
select is(
  (select metadata ->> 'name' from public.audit_logs where action = 'group.created' and target_id = tests.seed_id('30', 'a', 50)),
  'Audit Club',
  'creating a group is recorded with its name'
);

update public.groups set name = 'Audit Society' where id = tests.seed_id('30', 'a', 50);
select results_eq(
  format(
    $$select metadata ->> 'from', metadata ->> 'to' from public.audit_logs
      where action = 'group.renamed' and target_id = %L$$,
    tests.seed_id('30', 'a', 50)
  ),
  $$values ('Audit Club', 'Audit Society')$$,
  'renaming a group records both names'
);

insert into public.group_members (org_id, group_id, user_id)
values (tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 50), tests.seed_id('20', 'a', 4)),
       (tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 50), tests.seed_id('20', 'a', 5));
select is(
  (select count(*)::int from public.audit_logs
   where action = 'group_member.added' and metadata ->> 'group_id' = tests.seed_id('30', 'a', 50)::text),
  2,
  'adding group members is recorded for each member'
);

update public.group_members set role_in_group = 'moderator'
where group_id = tests.seed_id('30', 'a', 50) and user_id = tests.seed_id('20', 'a', 4);
select results_eq(
  $$select metadata ->> 'from', metadata ->> 'to' from public.audit_logs where action = 'group_member.role_changed'$$,
  $$values ('member', 'moderator')$$,
  'changing a role in a group is recorded'
);

delete from public.group_members
where group_id = tests.seed_id('30', 'a', 50) and user_id = tests.seed_id('20', 'a', 5);
select is(
  (select count(*)::int from public.audit_logs
   where action = 'group_member.removed' and metadata ->> 'group_id' = tests.seed_id('30', 'a', 50)::text),
  1,
  'removing one member from a group is recorded'
);

delete from public.groups where id = tests.seed_id('30', 'a', 50);
select is(
  (select metadata ->> 'name' from public.audit_logs where action = 'group.deleted' and target_id = tests.seed_id('30', 'a', 50)),
  'Audit Society',
  'deleting a group is recorded'
);
select is(
  (select count(*)::int from public.audit_logs
   where action = 'group_member.removed' and metadata ->> 'group_id' = tests.seed_id('30', 'a', 50)::text),
  1,
  'deleting a group does not record each of its members as removed'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is(
  (select count(*)::int from public.audit_logs),
  0,
  'staff cannot read the audit log'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select is(
  (select count(*)::int from public.audit_logs where org_id = tests.seed_id('10', 'a', 0)),
  0,
  'an admin of another organization cannot read the audit log'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
insert into public.org_members (org_id, user_id, role)
values (tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 5), 'member');
select ok(
  exists (
    select 1 from public.conversation_participants
    where conversation_id = tests.seed_id('40', 'a', 3) and user_id = tests.seed_id('20', 'b', 5)
  ),
  'a new member of the organization joins the announcements group conversation'
);
select * from finish();
rollback;
