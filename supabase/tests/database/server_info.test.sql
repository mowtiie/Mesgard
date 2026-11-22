begin;
select plan(4);

select tests.authenticate_as_anonymous();
select is(
  (public.server_info() ->> 'api_level')::int,
  18,
  'anyone can read the API level, even before signing in'
);

reset role;
update private.mfa_settings set enforced = true;

select tests.authenticate_as(tests.seed_id('20', 'a', 2), 'aal1');
select is(
  (public.server_info() ->> 'api_level')::int,
  18,
  'a session that has not entered the two-step verification code can read it'
);

reset role;
select is(
  (select prosecdef from pg_proc where proname = 'server_info' and pronamespace = 'public'::regnamespace),
  false,
  'it runs with the rights of the caller, so it cannot read any table by mistake'
);
select is(
  has_function_privilege('public', 'public.server_info()', 'execute'),
  false,
  'only the anon and authenticated roles can call it'
);

select * from finish();
rollback;
