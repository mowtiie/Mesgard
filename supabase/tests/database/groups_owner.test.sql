begin;
select plan(11);

-- Group 30-a-90 in organization A: a02 (staff) owns it, a03 is a member, a04 is a member.
insert into public.groups (id, org_id, name, posting_policy) values
  (tests.seed_id('30', 'a', 90), tests.seed_id('10', 'a', 0), 'Robotics Club', 'moderators');
insert into public.group_members (org_id, group_id, user_id, role_in_group) values
  (tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 2), 'owner'),
  (tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 3), 'member'),
  (tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 4), 'member');

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is_empty(
  format(
    $$update public.groups set name = 'Hijacked' where id = %L returning 1$$,
    tests.seed_id('30', 'a', 90)
  ),
  'a member cannot rename the group'
);
select throws_ok(
  format(
    $$insert into public.group_members (org_id, group_id, user_id) values (%L, %L, %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 1)
  ),
  '42501',
  null,
  'a member cannot add people'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  format($$update public.groups set name = 'Robotics and Coding' where id = %L$$, tests.seed_id('30', 'a', 90)),
  'the owner can rename the group'
);
select throws_ok(
  format($$update public.groups set parent_id = %L where id = %L$$, tests.seed_id('30', 'a', 4), tests.seed_id('30', 'a', 90)),
  '42501',
  null,
  'the owner cannot move the group in the tree'
);
select lives_ok(
  format(
    $$insert into public.group_members (org_id, group_id, user_id) values (%L, %L, %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 1)
  ),
  'the owner can add a member'
);
select throws_ok(
  format(
    $$insert into public.group_members (org_id, group_id, user_id, role_in_group) values (%L, %L, %L, 'owner')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 1)
  ),
  '42501',
  null,
  'a new person cannot be added as an owner directly'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format($$delete from public.group_members where group_id = %L and user_id = %L$$, tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 4)),
  'a member can leave'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$delete from public.group_members where group_id = %L and user_id = %L$$, tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 2)),
  '23514',
  null,
  'the last owner cannot leave'
);
select lives_ok(
  format(
    $$update public.group_members set role_in_group = 'owner' where group_id = %L and user_id = %L$$,
    tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 3)
  ),
  'an owner can make another member an owner'
);
select lives_ok(
  format($$delete from public.group_members where group_id = %L and user_id = %L$$, tests.seed_id('30', 'a', 90), tests.seed_id('20', 'a', 2)),
  'an owner can leave when there is another owner'
);

reset role;
select is(
  (select count(*)::int from public.group_members where group_id = tests.seed_id('30', 'a', 90) and role_in_group = 'owner'),
  1,
  'one owner is left'
);

select * from finish();
rollback;
