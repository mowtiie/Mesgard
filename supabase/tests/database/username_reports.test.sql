begin;
select plan(14);

-- Seed: organization a has the administrator a01 (the reviewer, as nobody is designated), staff a02 and a03, and
-- students a04, a05 and a06.

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format(
    $$select public.submit_report(%L, %L, 'impersonation', null, %L, 'Looks like the principal', false, 'username')$$,
    tests.seed_id('70', 'a', 31), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)
  ),
  'a person reports the username of another person'
);
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', null, %L, null, false, 'username')$$,
    tests.seed_id('70', 'a', 32), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)
  ),
  '23505',
  null,
  'the same username cannot be reported twice by the same person'
);
select throws_ok(
  format(
    $$select public.update_report(%L, 'resolved', 'Not appropriate', false, true)$$, tests.seed_id('70', 'a', 31)
  ),
  '42501',
  null,
  'a person who is not a reviewer cannot reset a username'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  public.get_report(tests.seed_id('70', 'a', 31)) ->> 'subject_text',
  (select username from public.org_members
   where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'a', 5)),
  'the reviewer sees the frozen username'
);
select throws_ok(
  format($$select public.update_report(%L, 'in_review', null, false, true)$$, tests.seed_id('70', 'a', 31)),
  '22023',
  null,
  'a username is reset only when the report is closed'
);
select lives_ok(
  format(
    $$select public.update_report(%L, 'resolved', 'Not appropriate', false, true)$$, tests.seed_id('70', 'a', 31)
  ),
  'a reviewer resets the username while closing the report'
);
select ok(
  (select username ~ '^user[0-9]{6}$' from public.org_members
   where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'a', 5)),
  'the username becomes a neutral one'
);

reset role;
select is(
  (select count(*)::int from public.report_events
   where report_id = tests.seed_id('70', 'a', 31) and action = 'username_reset'),
  1,
  'the reset is in the history of the report'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.list_profile_notices()),
  0,
  'the reporter gets no notice'
);
select throws_ok(
  format(
    $$insert into public.profile_notices (user_id, org_id, kind) values (%L, %L, 'username_reset')$$,
    tests.seed_id('20', 'a', 4), tests.seed_id('10', 'a', 0)
  ),
  '42501',
  null,
  'a notice cannot be written directly'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  (select kind from public.list_profile_notices()),
  'username_reset',
  'the person gets a notice that carries no reporter and no report'
);
select lives_ok(
  $$select public.acknowledge_profile_notices(array(select id from public.list_profile_notices()))$$,
  'the person acknowledges the notice'
);
select is(
  (select count(*)::int from public.list_profile_notices()),
  0,
  'an acknowledged notice is not shown again'
);

reset role;
update private.mfa_settings set enforced = true;
select tests.authenticate_as(tests.seed_id('20', 'a', 2), 'aal1');
select throws_ok(
  $$select * from public.list_profile_notices()$$,
  '42501',
  'Two-step verification is required',
  'a session without the two-step verification code cannot read notices'
);

select * from finish();
rollback;
