begin;
select plan(9);

select is(
  private.conversation_id_from_topic('conversation:' || tests.seed_id('40', 'a', 1)),
  tests.seed_id('40', 'a', 1),
  'a conversation topic resolves to the conversation identifier'
);
select is(private.conversation_id_from_topic('conversation:not-a-uuid'), null, 'a malformed topic resolves to nothing');
select is(private.conversation_id_from_topic('other:' || tests.seed_id('40', 'a', 1)), null, 'a foreign topic resolves to nothing');

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select set_config('realtime.topic', 'conversation:' || tests.seed_id('40', 'a', 1), true);
select lives_ok(
  $$insert into realtime.messages (topic, extension, payload, event, private)
    values ('conversation:' || tests.seed_id('40', 'a', 1), 'broadcast', '{}', 'typing', true)$$,
  'a participant can send a broadcast to the conversation'
);
select is(
  (select count(*)::int from realtime.messages where topic = 'conversation:' || tests.seed_id('40', 'a', 1)),
  1,
  'a participant can receive broadcasts of the conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select set_config('realtime.topic', 'conversation:' || tests.seed_id('40', 'a', 1), true);
select throws_ok(
  $$insert into realtime.messages (topic, extension, payload, event, private)
    values ('conversation:' || tests.seed_id('40', 'a', 1), 'broadcast', '{}', 'typing', true)$$,
  '42501',
  null,
  'a user outside the conversation cannot send a broadcast to it'
);
select is(
  (select count(*)::int from realtime.messages where topic = 'conversation:' || tests.seed_id('40', 'a', 1)),
  0,
  'a user outside the conversation cannot receive its broadcasts'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select set_config('realtime.topic', 'conversation:' || tests.seed_id('40', 'a', 1), true);
select throws_ok(
  $$insert into realtime.messages (topic, extension, payload, event, private)
    values ('conversation:' || tests.seed_id('40', 'a', 1), 'broadcast', '{}', 'typing', true)$$,
  '42501',
  null,
  'a user of another organization cannot send a broadcast to the conversation'
);

select tests.authenticate_as_anonymous();
select set_config('realtime.topic', 'conversation:' || tests.seed_id('40', 'a', 1), true);
select throws_ok(
  $$insert into realtime.messages (topic, extension, payload, event, private)
    values ('conversation:' || tests.seed_id('40', 'a', 1), 'broadcast', '{}', 'typing', true)$$,
  '42501',
  null,
  'anonymous users cannot send a broadcast to the conversation'
);

select * from finish();
rollback;
