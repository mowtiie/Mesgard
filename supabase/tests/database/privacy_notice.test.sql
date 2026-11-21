begin;
select plan(24);

-- Seed: organization a has the administrator a01, staff a02, students a04 and a05; organization b has b04.

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.list_privacy_notices()),
  1,
  'a person lists the notices of their own organization only'
);
select is(
  (select is_acknowledged from public.list_privacy_notices()),
  true,
  'an organization without a notice has nothing to acknowledge'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$select public.set_privacy_notice(%L, 'https://school.example/privacy', null)$$, tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'staff cannot set the privacy notice'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.set_privacy_notice(%L, 'javascript:alert(1)', null)$$, tests.seed_id('10', 'a', 0)),
  '23514',
  null,
  'a link has to be http or https'
);
select throws_ok(
  format($$select public.set_privacy_notice(%L, 'https://school.example/a b', null)$$, tests.seed_id('10', 'a', 0)),
  '23514',
  null,
  'a link has no spaces'
);
select throws_ok(
  format($$select public.set_privacy_notice(%L, 'https://school.example/privacy', null)$$, tests.seed_id('10', 'b', 0)),
  '42501',
  null,
  'an administrator cannot set the notice of another organization'
);
select lives_ok(
  format(
    $$select public.set_privacy_notice(%L, ' https://school.example/privacy ', 'http://intranet.local/kids')$$,
    tests.seed_id('10', 'a', 0)
  ),
  'an administrator sets the notice and the version for children'
);
select is(
  (select privacy_notice_version from public.organizations where id = tests.seed_id('10', 'a', 0)),
  1,
  'the first notice is version 1'
);
select is(
  (select count(*)::int from public.audit_logs where action = 'organization.privacy_notice_changed'),
  1,
  'setting the notice is recorded in the audit log'
);
select lives_ok(
  format(
    $$select public.set_privacy_notice(%L, 'https://school.example/privacy', 'http://intranet.local/kids')$$,
    tests.seed_id('10', 'a', 0)
  ),
  'saving the same links again is accepted'
);
select is(
  (select privacy_notice_version from public.organizations where id = tests.seed_id('10', 'a', 0)),
  1,
  'and does not ask everyone to acknowledge again'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select results_eq(
  $$select notice_url, child_notice_url, is_acknowledged from public.list_privacy_notices()$$,
  $$values ('https://school.example/privacy'::text, 'http://intranet.local/kids'::text, false)$$,
  'a member sees the links and has not acknowledged them yet'
);
select throws_ok(
  format($$select public.acknowledge_privacy_notice(%L)$$, tests.seed_id('10', 'b', 0)),
  '42501',
  null,
  'nobody acknowledges the notice of an organization they do not belong to'
);
select lives_ok(
  format($$select public.acknowledge_privacy_notice(%L)$$, tests.seed_id('10', 'a', 0)),
  'a member acknowledges the notice'
);
select is(
  (select is_acknowledged from public.list_privacy_notices()),
  true,
  'and it is remembered'
);
select is(
  (select count(*)::int from public.privacy_notice_acknowledgments),
  1,
  'a person reads their own acknowledgment'
);
select throws_ok(
  format(
    $$insert into public.privacy_notice_acknowledgments (org_id, user_id, version) values (%L, %L, 1)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'acknowledgments cannot be written directly'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  (select count(*)::int from public.privacy_notice_acknowledgments),
  0,
  'nobody reads the acknowledgments of another person'
);
select is(
  (select is_acknowledged from public.list_privacy_notices()),
  false,
  'another member still has to acknowledge'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format(
    $$select public.set_privacy_notice(%L, 'https://school.example/privacy-2026', 'http://intranet.local/kids')$$,
    tests.seed_id('10', 'a', 0)
  ),
  'an administrator changes the notice'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select is_acknowledged from public.list_privacy_notices()),
  false,
  'a changed notice has to be acknowledged again'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.set_privacy_notice(%L, '  ', null)$$, tests.seed_id('10', 'a', 0)),
  'an administrator removes the notice'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select is_acknowledged from public.list_privacy_notices()),
  true,
  'with no notice there is nothing to acknowledge'
);

reset role;
update private.mfa_settings set enforced = true;
select tests.authenticate_as(tests.seed_id('20', 'a', 1), 'aal1');
select throws_ok(
  'select * from public.list_privacy_notices()',
  '42501',
  'Two-step verification is required',
  'an administrator who has not entered the code cannot read the notices'
);

select * from finish();
rollback;
