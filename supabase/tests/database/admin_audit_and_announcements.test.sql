begin;
select plan(23);

-- A change made without a signed in user (a migration, a cascade, the service role) is not an admin action.
reset role;
insert into public.org_members (org_id, user_id, role)
values (tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 3), 'student');
select is(
  (select count(*)::int from public.audit_logs where action = 'member.added'),
  0,
  'a change without an actor is not recorded'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
insert into public.org_members (org_id, user_id, role)
values (tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 4), 'student');
select results_eq(
  format(
    $$select actor_id, target_id, metadata ->> 'role' from public.audit_logs
      where action = 'member.added' and target_id = %L$$,
    tests.seed_id('20', 'b', 4)
  ),
  format($$values (%L::uuid, %L::uuid, 'student')$$, tests.seed_id('20', 'a', 1), tests.seed_id('20', 'b', 4)),
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
  $$values ('student', 'staff')$$,
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
  (select count(*)::int from public.audit_logs where action = 'group_member.removed'),
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
  (select count(*)::int from public.audit_logs where action = 'group_member.removed'),
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

-- The announcement conversation of organization a is recreated from scratch.
reset role;
delete from public.conversations where org_id = tests.seed_id('10', 'a', 0) and type = 'announcement';

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
create temporary table announcements as
select public.create_announcement_conversation(tests.seed_id('10', 'a', 0)) as id;
grant all on announcements to authenticated;

select is(
  (select type || ':' || title from public.conversations where id = (select id from announcements)),
  'announcement:Announcements',
  'staff can create the announcement conversation of their organization'
);
select is(
  (select count(*)::int from public.conversation_participants where conversation_id = (select id from announcements)),
  (select count(*)::int from public.org_members where org_id = tests.seed_id('10', 'a', 0)),
  'every member of the organization becomes a participant'
);
select is(
  public.create_announcement_conversation(tests.seed_id('10', 'a', 0)),
  (select id from announcements),
  'creating the announcement conversation again is idempotent'
);
select is(
  (select count(*)::int from public.conversations where org_id = tests.seed_id('10', 'a', 0) and type = 'announcement'),
  1,
  'an organization has a single announcement conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format('select public.create_announcement_conversation(%L)', tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'a student cannot create the announcement conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select throws_ok(
  format('select public.create_announcement_conversation(%L)', tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'an admin of another organization cannot create it'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  format('select public.create_announcement_conversation(%L)', tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'anonymous users cannot create it'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
insert into public.org_members (org_id, user_id, role)
values (tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 5), 'student');
select ok(
  exists (
    select 1 from public.conversation_participants
    where conversation_id = (select id from announcements) and user_id = tests.seed_id('20', 'b', 5)
  ),
  'a new member of the organization joins the announcement conversation'
);
select ok(
  exists (select 1 from public.audit_logs where action = 'announcements.enabled' and target_id = (select id from announcements)),
  'enabling announcements is recorded'
);

select * from finish();
rollback;
