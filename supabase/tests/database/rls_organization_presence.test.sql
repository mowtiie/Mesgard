begin;
select plan(8);

select is(
  private.organization_id_from_topic('org:' || tests.seed_id('10', 'a', 0)),
  tests.seed_id('10', 'a', 0),
  'an organization topic resolves to the organization identifier'
);
select is(private.organization_id_from_topic('org:not-a-uuid'), null, 'a malformed topic resolves to nothing');
select is(private.organization_id_from_topic('conversation:' || tests.seed_id('10', 'a', 0)), null, 'a foreign topic resolves to nothing');

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select set_config('realtime.topic', 'org:' || tests.seed_id('10', 'a', 0), true);
select lives_ok(
  $$insert into realtime.messages (topic, extension, payload, event, private)
    values ('org:' || tests.seed_id('10', 'a', 0), 'presence', '{}', 'presence_state', true)$$,
  'a member can track presence in the organization'
);
select is(
  (select count(*)::int from realtime.messages where topic = 'org:' || tests.seed_id('10', 'a', 0)),
  1,
  'a member can see presence in the organization'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select set_config('realtime.topic', 'org:' || tests.seed_id('10', 'a', 0), true);
select throws_ok(
  $$insert into realtime.messages (topic, extension, payload, event, private)
    values ('org:' || tests.seed_id('10', 'a', 0), 'presence', '{}', 'presence_state', true)$$,
  '42501',
  null,
  'a member of another organization cannot track presence in the organization'
);
select is(
  (select count(*)::int from realtime.messages where topic = 'org:' || tests.seed_id('10', 'a', 0)),
  0,
  'a member of another organization cannot see presence in the organization'
);

select tests.authenticate_as_anonymous();
select set_config('realtime.topic', 'org:' || tests.seed_id('10', 'a', 0), true);
select throws_ok(
  $$insert into realtime.messages (topic, extension, payload, event, private)
    values ('org:' || tests.seed_id('10', 'a', 0), 'presence', '{}', 'presence_state', true)$$,
  '42501',
  null,
  'anonymous users cannot track presence in the organization'
);

select * from finish();
rollback;
