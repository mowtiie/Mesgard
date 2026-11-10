begin;
select plan(11);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select unread_count from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 3)),
  2::bigint,
  'the announcement conversation starts with two unread messages'
);
select lives_ok(
  format(
    'select public.mark_conversation_read(%L, %L)',
    tests.seed_id('40', 'a', 3), tests.seed_id('50', 'a', 6)
  ),
  'a participant can mark a conversation as read up to its newest message'
);
select is(
  (select unread_count from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 3)),
  0::bigint,
  'the unread count drops to zero'
);
select is(
  (
    select count(*)::int from public.message_receipts
    where user_id = tests.seed_id('20', 'a', 4)
      and message_id in (tests.seed_id('50', 'a', 5), tests.seed_id('50', 'a', 6))
      and read_at is not null
  ),
  2,
  'read receipts are recorded for the messages from other senders'
);

select lives_ok(
  format(
    'select public.mark_conversation_read(%L, %L)',
    tests.seed_id('40', 'a', 3), tests.seed_id('50', 'a', 5)
  ),
  'marking an older message as read is accepted'
);
select is(
  (
    select last_read_message_id from public.conversation_participants
    where conversation_id = tests.seed_id('40', 'a', 3) and user_id = tests.seed_id('20', 'a', 4)
  ),
  tests.seed_id('50', 'a', 6),
  'the read marker never moves backwards'
);

select lives_ok(
  format(
    'select public.mark_conversation_read(%L, %L)',
    tests.seed_id('40', 'a', 2), tests.seed_id('50', 'a', 4)
  ),
  'a participant can mark a conversation as read up to their own message'
);
select is(
  (
    select count(*)::int from public.message_receipts
    where user_id = tests.seed_id('20', 'a', 4)
      and message_id in (tests.seed_id('50', 'a', 3), tests.seed_id('50', 'a', 4))
  ),
  1,
  'no read receipt is created for the own messages of the user'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is(
  (
    select count(*)::int from public.message_receipts
    where message_id = tests.seed_id('50', 'a', 3) and read_at is not null
  ),
  1,
  'a sender can see that another participant read their message'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 6));
select throws_ok(
  format(
    'select public.mark_conversation_read(%L, %L)',
    tests.seed_id('40', 'a', 1), tests.seed_id('50', 'a', 2)
  ),
  'P0002',
  'The message was not found in this conversation',
  'a non-participant cannot mark a conversation as read'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  format(
    'select public.mark_conversation_read(%L, %L)',
    tests.seed_id('40', 'a', 3), tests.seed_id('50', 'a', 6)
  ),
  '42501',
  null,
  'anonymous users cannot mark conversations as read'
);

select * from finish();
rollback;
