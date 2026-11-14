begin;
select plan(4);

reset role;
update auth.users
set raw_app_meta_data = raw_app_meta_data || '{"must_change_password": true}'
where id in (tests.seed_id('20', 'a', 4), tests.seed_id('20', 'a', 5));

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok('select public.complete_password_change()', 'a signed in user can clear their own mark');

reset role;
select is(
  (select raw_app_meta_data ? 'must_change_password' from auth.users where id = tests.seed_id('20', 'a', 4)),
  false,
  'the mark is removed for the caller'
);
select is(
  (select raw_app_meta_data ->> 'provider' from auth.users where id = tests.seed_id('20', 'a', 4)),
  'email',
  'the rest of the app metadata is kept'
);
select is(
  (select raw_app_meta_data ? 'must_change_password' from auth.users where id = tests.seed_id('20', 'a', 5)),
  true,
  'the mark of another user is not touched'
);

select * from finish();
rollback;
