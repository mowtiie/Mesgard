begin;
select plan(11);

-- Seed: administrator a01, staff a02 and a03, members a04 to a06. Organization b has the same shape.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
delete from public.org_members
where org_id = tests.seed_id('10', 'a', 0) and user_id in (tests.seed_id('20', 'a', 4), tests.seed_id('20', 'a', 5));

select is(
  (select count(*)::int from public.list_former_members(tests.seed_id('10', 'a', 0))),
  2,
  'an administrator sees the people the organization removed'
);
select is(
  (select user_id from public.list_former_members(tests.seed_id('10', 'a', 0), 1)),
  tests.seed_id('20', 'a', 5),
  'removed together, the higher id comes first, and a page of one stops there'
);
select is(
  (select count(*)::int from public.list_former_members(tests.seed_id('10', 'a', 0))
   where user_id = tests.seed_id('20', 'a', 2)),
  0,
  'a current member is not listed'
);
select results_eq(
  format(
    $$select count(*)::int from public.list_former_members(%L, 50, now() + interval '1 minute', %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)
  ),
  $$values (2)$$,
  'a page after a later cursor still reaches both people'
);
select is(
  (select count(*)::int from public.list_former_members(tests.seed_id('10', 'a', 0), 50, now() - interval '1 minute', null)),
  0,
  'a cursor before every removal leaves nothing'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  (select can_erase from public.list_former_members(tests.seed_id('10', 'a', 0)) where user_id = tests.seed_id('20', 'a', 4)),
  true,
  'someone who belongs to no organization can be erased'
);

reset role;
insert into public.org_members (org_id, user_id, role)
values (tests.seed_id('10', 'b', 0), tests.seed_id('20', 'a', 5), 'member');
insert into public.audit_logs (org_id, actor_id, action, target_type, target_id)
values (tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 1), 'member.erased', 'user', tests.seed_id('20', 'a', 4));
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select results_eq(
  format(
    $$select is_erased, can_erase from public.list_former_members(%L) where user_id = %L$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)
  ),
  $$values (true, false)$$,
  'an erased person is marked and cannot be erased again'
);
select is(
  (select can_erase from public.list_former_members(tests.seed_id('10', 'a', 0)) where user_id = tests.seed_id('20', 'a', 5)),
  false,
  'someone who belongs to another organization cannot be erased'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select throws_ok(
  format('select * from public.list_former_members(%L)', tests.seed_id('10', 'a', 0)),
  '42501',
  'Only an administrator can list former members',
  'an administrator of another organization is refused'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format('select * from public.list_former_members(%L)', tests.seed_id('10', 'a', 0)),
  '42501',
  'Only an administrator can list former members',
  'staff are refused'
);

reset role;
update private.mfa_settings set enforced = true;
select tests.authenticate_as(tests.seed_id('20', 'a', 1), 'aal1');
select throws_ok(
  format('select * from public.list_former_members(%L)', tests.seed_id('10', 'a', 0)),
  '42501',
  'Two-step verification is required',
  'a session that has not entered the code is refused'
);

select * from finish();
rollback;
