begin;
select plan(28);

-- Seed: organization a has the administrator a01, staff a02 and a03, students a04, a05 and a06. Organization b has
-- the student b04.

select is(
  (select count(*)::int from public.org_members where not private.is_valid_username(username)),
  0,
  'every member has a username in the allowed format'
);
select is(
  (select count(*)::int from (
    select org_id, username from public.org_members group by org_id, username having count(*) > 1
  ) as duplicated),
  0,
  'usernames are unique inside an organization'
);

delete from public.org_members
where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'a', 6);
insert into public.org_members (org_id, user_id, role)
values (tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 6), 'member');
select ok(
  (select private.is_valid_username(username) from public.org_members
   where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'a', 6)),
  'a member added without a username gets one made from the display name'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    $$update public.org_members set username = 'sneaky.name' where org_id = %L and user_id = %L$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a client cannot write the username directly'
);

select lives_ok(
  format($$select public.set_username(%L, ' New.Name ')$$, tests.seed_id('10', 'a', 0)),
  'a person changes their own username'
);
select is(
  (select username from public.org_members
   where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'a', 4)),
  'new.name',
  'the username is stored in lowercase without spaces around it'
);
select throws_ok(
  format($$select public.set_username(%L, 'another.one')$$, tests.seed_id('10', 'a', 0)),
  '54000',
  null,
  'a username changes once every 30 days'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select throws_ok(
  format($$select public.set_username(%L, 'ab')$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'a username has at least 3 characters'
);
select throws_ok(
  format($$select public.set_username(%L, 'bad..name')$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'a username has no double separators'
);
select throws_ok(
  format($$select public.set_username(%L, '.leading')$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'a username does not start with a separator'
);
select throws_ok(
  format($$select public.set_username(%L, 'has space')$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'a username has no spaces'
);
select throws_ok(
  format($$select public.set_username(%L, %L)$$, tests.seed_id('10', 'a', 0), repeat('a', 21)),
  '22023',
  null,
  'a username has at most 20 characters'
);
select throws_ok(
  format($$select public.set_username(%L, 'Administrator')$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'a reserved username is refused'
);
select throws_ok(
  format($$select public.set_username(%L, 'new.name')$$, tests.seed_id('10', 'a', 0)),
  '23505',
  null,
  'a username in use is refused'
);

reset role;
select is(
  (select count(*)::int from private.released_usernames
   where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'a', 4)),
  1,
  'the old username of a person who changed it is kept back'
);

create temp table released_name as
  select username from private.released_usernames
  where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'a', 4);
grant select on released_name to authenticated;

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select throws_ok(
  format($$select public.set_username(%L, %L)$$, tests.seed_id('10', 'a', 0), (select username from released_name)),
  '23505',
  null,
  'a released username cannot be taken by someone else for 90 days'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select lives_ok(
  format($$select public.set_username(%L, 'new.name')$$, tests.seed_id('10', 'b', 0)),
  'the same username can exist in another organization'
);

-- Policy.
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$select public.set_username_policy(%L, 'nobody')$$, tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'staff cannot change the username policy'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.set_username_policy(%L, 'sometimes')$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'an unknown policy is refused'
);
select lives_ok(
  format($$select public.set_username_policy(%L, 'staff_and_admins')$$, tests.seed_id('10', 'a', 0)),
  'an administrator limits changing usernames to staff and administrators'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select throws_ok(
  format($$select public.set_username(%L, 'student.name')$$, tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'a student cannot change a username when the policy keeps it to staff'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  format($$select public.set_username(%L, 'staff.name')$$, tests.seed_id('10', 'a', 0)),
  'staff can change a username under the same policy'
);

-- Administrators.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format(
    $$select public.admin_set_username(%L, %L, 'chosen.one')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)
  ),
  'an administrator sets the username of a member even when the policy keeps it to staff'
);
select ok(
  (select public.reset_username(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)) ~ '^user[0-9]{6}$'),
  'a reset gives a neutral username'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format(
    $$select public.admin_set_username(%L, %L, 'chosen.two')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)
  ),
  '42501',
  null,
  'staff cannot set the username of someone else'
);
select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select throws_ok(
  format($$select public.reset_username(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)),
  '42501',
  null,
  'an administrator of another organization cannot reset a username'
);

reset role;
select is(
  (select count(*)::int from public.audit_logs
   where action in ('member.username_changed', 'member.username_set', 'member.username_reset')),
  5,
  'every change of a username is in the audit log'
);

-- Two-step verification.
update private.mfa_settings set enforced = true;
select tests.authenticate_as(tests.seed_id('20', 'a', 2), 'aal1');
select throws_ok(
  format($$select public.set_username(%L, 'aal.one')$$, tests.seed_id('10', 'a', 0)),
  '42501',
  'Two-step verification is required',
  'a session without the two-step verification code cannot change a username'
);

select * from finish();
rollback;
