begin;
select plan(7);

select tests.authenticate_as(tests.seed_id('20', 'a', 6));
select is(
  (select count(*)::int from public.conversations where id = tests.seed_id('40', 'a', 1)),
  0,
  'a non-participant cannot read a direct conversation'
);
select is(
  (select count(*)::int from public.conversation_participants where conversation_id = tests.seed_id('40', 'a', 1)),
  0,
  'a non-participant cannot read the participants of a conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.conversations where id = tests.seed_id('40', 'a', 1)),
  1,
  'a participant can read their direct conversation'
);
select is(
  (select count(*)::int from public.conversation_participants where conversation_id = tests.seed_id('40', 'a', 1)),
  2,
  'a participant can read the participants of their conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select is(
  (select count(*)::int from public.conversations where org_id = tests.seed_id('10', 'a', 0)),
  0,
  'an admin of another organization cannot read conversations'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (
    with changed as (
      update public.conversation_participants
      set muted = true
      where conversation_id = tests.seed_id('40', 'a', 1)
        and user_id = tests.seed_id('20', 'a', 2)
      returning 1
    )
    select count(*)::int from changed
  ),
  0,
  'a participant cannot change the settings of another participant'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  'select * from public.conversations',
  '42501',
  null,
  'anonymous users cannot read conversations'
);

select * from finish();
rollback;
