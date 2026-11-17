begin;
select plan(8);

select is(
  (
    select count(*)::int
    from pg_proc as procedure
    where procedure.pronamespace = 'public'::regnamespace
      and procedure.prosecdef
      and has_function_privilege('authenticated', procedure.oid, 'execute')
      and procedure.prorettype <> 'event_trigger'::regtype
      and procedure.proname <> 'is_mfa_required'
      and procedure.prosrc not like '%private.require_mfa()%'
  ),
  0,
  'every security definer function callable by signed in users checks two-step verification'
);

update private.mfa_settings set enforced = true;

select tests.authenticate_as(tests.seed_id('20', 'a', 1), 'aal1');
select throws_ok(
  format('select public.create_direct_conversation(%L, %L)', tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2)),
  '42501',
  'Two-step verification is required',
  'an aal1 administrator cannot start a direct conversation'
);
select throws_ok(
  format('select public.create_group_conversation(%L)', tests.seed_id('30', 'a', 1)),
  '42501',
  'Two-step verification is required',
  'an aal1 administrator cannot start a group conversation'
);
select throws_ok(
  $$ select public.register_push_subscription('https://push.example/endpoint', 'key', 'secret') $$,
  '42501',
  'Two-step verification is required',
  'an aal1 administrator cannot register a push subscription'
);
select throws_ok(
  format('select public.send_message(gen_random_uuid(), %L, %L)', gen_random_uuid(), 'hello'),
  '42501',
  'Two-step verification is required',
  'an aal1 administrator cannot send a message'
);
select throws_ok(
  'select public.complete_password_change()',
  '42501',
  'Two-step verification is required',
  'an aal1 administrator cannot complete a password change'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1), 'aal2');
select lives_ok('select public.complete_password_change()', 'an aal2 administrator can complete a password change');

select tests.authenticate_as(tests.seed_id('20', 'a', 4), 'aal1');
select lives_ok('select public.complete_password_change()', 'a member without a factor is not held to two-step verification');

select * from finish();
rollback;
