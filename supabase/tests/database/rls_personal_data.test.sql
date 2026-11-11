begin;
select plan(5);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  (select count(*)::int from public.push_subscriptions),
  0,
  'a user cannot read the push subscriptions of another user'
);
select throws_ok(
  format(
    $$insert into public.push_subscriptions (user_id, endpoint, p256dh, auth_secret) values (%L, 'https://example.invalid/x', 'p256dh-key-stolen-0', 'auth-secret-stolen-0')$$,
    tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a user cannot register a push subscription for another user'
);
select is_empty(
  format(
    'update public.profiles set display_name = ''Renamed'' where id = %L returning 1',
    tests.seed_id('20', 'a', 4)
  ),
  'a user cannot update the profile of another user'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.push_subscriptions),
  1,
  'a user can read their own push subscriptions'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  'select * from public.push_subscriptions',
  '42501',
  null,
  'anonymous users cannot read push subscriptions'
);

select * from finish();
rollback;
