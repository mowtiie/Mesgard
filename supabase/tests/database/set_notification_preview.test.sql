begin;
select plan(6);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  $$select public.set_notification_preview('hidden')$$,
  'a user can set their notification preview for the first time'
);
select is(
  (select notification_preview from public.user_settings where user_id = tests.seed_id('20', 'a', 4)),
  'hidden',
  'the preview is stored for the signed in user'
);
select lives_ok(
  $$select public.set_notification_preview('sender_and_text')$$,
  'a user can change their notification preview again'
);
select is(
  (select count(*)::int from public.user_settings where user_id = tests.seed_id('20', 'a', 4)),
  1,
  'changing the preview keeps a single settings row'
);
select throws_ok(
  $$select public.set_notification_preview('everything')$$,
  '23503',
  null,
  'an unknown preview mode is rejected'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  $$select public.set_notification_preview('hidden')$$,
  '42501',
  null,
  'anonymous users cannot set a notification preview'
);

select * from finish();
rollback;
