begin;
select plan(9);

-- Seed: student a04 and staff a02 of organization a.

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.user_settings where show_online_status is not null),
  0,
  'nobody has chosen yet'
);
select lives_ok($$select public.set_online_status_visibility(true)$$, 'a student chooses to show their online status');
select is(
  (select show_online_status from public.user_settings),
  true,
  'the choice is stored'
);
select lives_ok($$select public.set_notification_preview('hidden')$$, 'they also choose a notification preview');
select is(
  (select show_online_status::text || ' ' || notification_preview from public.user_settings),
  'true hidden',
  'neither choice replaces the other'
);
select lives_ok($$select public.set_online_status_visibility(null)$$, 'they go back to the default for their role');
select is(
  (select notification_preview from public.user_settings),
  'hidden',
  'which leaves the notification preview alone'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is(
  (select count(*)::int from public.user_settings),
  0,
  'nobody reads the choices of someone else'
);
select tests.authenticate_as_anonymous();
select throws_ok(
  $$select public.set_online_status_visibility(true)$$,
  '42501',
  null,
  'someone who is not signed in cannot choose'
);

select * from finish();
rollback;
