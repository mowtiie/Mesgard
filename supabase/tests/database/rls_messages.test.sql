begin;
select plan(12);

select tests.authenticate_as(tests.seed_id('20', 'a', 6));
select is(
  (select count(*)::int from public.messages where conversation_id = tests.seed_id('40', 'a', 1)),
  0,
  'a non-participant cannot read messages'
);
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, 'Hello')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 6)
  ),
  '42501',
  null,
  'a non-participant cannot send a message to a conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.messages where conversation_id = tests.seed_id('40', 'a', 1)),
  2,
  'a participant can read messages'
);
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, 'Impersonation')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 2)
  ),
  '42501',
  null,
  'a participant cannot send a message as another user'
);
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body, created_at)
      values (gen_random_uuid(), %L, %L, %L, 'Backdated', now() - interval '1 day')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a participant cannot set the server timestamp'
);
select lives_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (%L, %L, %L, %L, 'Hello')
      on conflict (id) do nothing$$,
    tests.seed_id('50', 'a', 99), tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 4)
  ),
  'a participant can send a message with a client-generated identifier'
);
select lives_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (%L, %L, %L, %L, 'Hello')
      on conflict (id) do nothing$$,
    tests.seed_id('50', 'a', 99), tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 4)
  ),
  'retrying a send with the same identifier is idempotent'
);
select lives_ok(
  format('update public.messages set body = ''Edited'' where id = %L', tests.seed_id('50', 'a', 99)),
  'a sender can edit the body of their own message'
);
select isnt(
  (select edited_at from public.messages where id = tests.seed_id('50', 'a', 99)),
  null,
  'editing the body of a message stamps edited_at'
);
select throws_ok(
  format('delete from public.messages where id = %L', tests.seed_id('50', 'a', 99)),
  '42501',
  null,
  'a sender cannot hard delete a message'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is_empty(
  format('update public.messages set body = ''Tampered'' where id = %L returning 1', tests.seed_id('50', 'a', 99)),
  'a participant cannot edit the message of another user'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select is(
  (select count(*)::int from public.messages where org_id = tests.seed_id('10', 'a', 0)),
  0,
  'a member of another organization cannot read messages'
);

select * from finish();
rollback;
