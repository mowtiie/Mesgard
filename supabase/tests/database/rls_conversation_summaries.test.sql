begin;
select plan(11);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select results_eq(
  'select conversation_id from public.list_conversation_summaries()',
  format(
    'values (%L::uuid), (%L::uuid), (%L::uuid)',
    tests.seed_id('40', 'a', 3), tests.seed_id('40', 'a', 2), tests.seed_id('40', 'a', 1)
  ),
  'a participant sees only their conversations, newest activity first'
);
select is(
  (select title from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 1)),
  'Staff One RNHS',
  'a direct conversation is titled with the other participant'
);
select is(
  (select title from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 2)),
  'Grade 10 Section A',
  'a group conversation keeps its own title'
);
select is(
  (
    select last_message_body from public.list_conversation_summaries()
    where conversation_id = tests.seed_id('40', 'a', 1)
  ),
  'Of course. It is due on Friday.',
  'the last message is the newest message of the conversation'
);
select is(
  (select unread_count from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 3)),
  2::bigint,
  'unread messages from other senders are counted'
);
select is(
  (select unread_count from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 2)),
  1::bigint,
  'a message sent by the user is not counted as unread'
);
select is(
  (select count(*)::int from public.list_conversation_summaries(page_size => 2)),
  2,
  'the page size limits the number of conversations'
);
select is(
  (
    select count(*)::int from public.list_conversation_summaries(
      activity_before => (select min(activity_at) from public.list_conversation_summaries())
    )
  ),
  0,
  'activity_before returns only older conversations'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 6));
select results_eq(
  'select conversation_id from public.list_conversation_summaries()',
  format('values (%L::uuid), (%L::uuid)', tests.seed_id('40', 'a', 4), tests.seed_id('40', 'a', 3)),
  'a user does not see conversations they are not part of'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select is(
  (select count(*)::int from public.list_conversation_summaries() where org_id = tests.seed_id('10', 'a', 0)),
  0,
  'a member of another organization does not see these conversations'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  'select * from public.list_conversation_summaries()',
  '42501',
  null,
  'anonymous users cannot list conversations'
);

select * from finish();
rollback;
