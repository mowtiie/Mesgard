begin;
select plan(10);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select notification_preview_default from public.organizations where id = tests.seed_id('10', 'a', 0)),
  'sender_only',
  'an organization defaults to showing only the sender'
);
select lives_ok(
  format(
    $$insert into public.user_settings (user_id, notification_preview) values (%L, 'hidden')$$,
    tests.seed_id('20', 'a', 4)
  ),
  'a user can create their own settings'
);
select lives_ok(
  format(
    $$update public.user_settings set notification_preview = 'sender_and_text' where user_id = %L$$,
    tests.seed_id('20', 'a', 4)
  ),
  'a user can change their own notification preview'
);
select throws_ok(
  format(
    $$update public.user_settings set notification_preview = 'everything' where user_id = %L$$,
    tests.seed_id('20', 'a', 4)
  ),
  '23503',
  null,
  'an unknown preview mode is rejected'
);
select throws_ok(
  format(
    $$insert into public.user_settings (user_id, notification_preview) values (%L, 'hidden')$$,
    tests.seed_id('20', 'a', 5)
  ),
  '42501',
  null,
  'a user cannot create settings for another user'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  (select count(*)::int from public.user_settings),
  0,
  'a user cannot read the settings of another user'
);
select is_empty(
  format(
    $$update public.organizations set notification_preview_default = 'hidden' where id = %L returning 1$$,
    tests.seed_id('10', 'a', 0)
  ),
  'a student cannot change the organization default'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format(
    $$update public.organizations set notification_preview_default = 'hidden' where id = %L$$,
    tests.seed_id('10', 'a', 0)
  ),
  'an organization admin can change the organization default'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select is_empty(
  format(
    $$update public.organizations set notification_preview_default = 'hidden' where id = %L returning 1$$,
    tests.seed_id('10', 'a', 0)
  ),
  'an admin of another organization cannot change the default'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  'select * from public.user_settings',
  '42501',
  null,
  'anonymous users cannot read settings'
);

select * from finish();
rollback;
