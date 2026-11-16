begin;
select plan(5);

insert into realtime.messages (topic, extension, payload, event, private)
values
  ('messages-sync', 'broadcast', '{}', 'test', true),
  ('other-channel', 'broadcast', '{}', 'test', true);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select set_config('realtime.topic', 'messages-sync', true);
select is(
  (select count(*)::int from realtime.messages where topic = 'messages-sync'),
  1,
  'a signed in user can join the message sync channel'
);
select throws_ok(
  $$insert into realtime.messages (topic, extension, payload, event, private)
    values ('messages-sync', 'broadcast', '{}', 'test', true)$$,
  '42501',
  null,
  'a signed in user cannot send on the message sync channel'
);

select set_config('realtime.topic', 'other-channel', true);
select is(
  (select count(*)::int from realtime.messages where topic = 'other-channel'),
  0,
  'the policy does not open any other channel'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select set_config('realtime.topic', 'messages-sync', true);
select is(
  (select count(*)::int from realtime.messages where topic = 'messages-sync'),
  1,
  'a user of another organization can join too, and row level security filters the changes they receive'
);

select tests.authenticate_as_anonymous();
select set_config('realtime.topic', 'messages-sync', true);
select is(
  (select count(*)::int from realtime.messages where topic = 'messages-sync'),
  0,
  'anonymous users cannot join the message sync channel'
);

select * from finish();
rollback;
