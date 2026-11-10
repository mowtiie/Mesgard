begin;
select plan(5);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  (select count(*)::int from public.device_tokens),
  0,
  'a user cannot read the device tokens of another user'
);
select throws_ok(
  format(
    $$insert into public.device_tokens (user_id, fcm_token) values (%L, 'stolen-token')$$,
    tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a user cannot register a device token for another user'
);
select is(
  (
    with changed as (
      update public.profiles set display_name = 'Renamed' where id = tests.seed_id('20', 'a', 4)
      returning 1
    )
    select count(*)::int from changed
  ),
  0,
  'a user cannot update the profile of another user'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.device_tokens),
  1,
  'a user can read their own device tokens'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  'select * from public.device_tokens',
  '42501',
  null,
  'anonymous users cannot read device tokens'
);

select * from finish();
rollback;
