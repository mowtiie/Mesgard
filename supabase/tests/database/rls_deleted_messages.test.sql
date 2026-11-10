begin;
select plan(10);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format('update public.messages set deleted_at = now() where id = %L', tests.seed_id('50', 'a', 1)),
  'a sender can soft delete their own message'
);
select is(
  (select edited_at from public.messages where id = tests.seed_id('50', 'a', 1)),
  null,
  'deleting a message does not mark it as edited'
);
select throws_ok(
  format('update public.messages set deleted_at = null where id = %L', tests.seed_id('50', 'a', 1)),
  '23001',
  'A deleted message cannot be changed',
  'a deleted message cannot be restored'
);
select throws_ok(
  format('update public.messages set body = ''Rewritten'' where id = %L', tests.seed_id('50', 'a', 1)),
  '23001',
  'A deleted message cannot be changed',
  'a deleted message cannot be edited'
);
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, '')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 4)
  ),
  '23514',
  null,
  'a new message cannot have an empty body'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is(
  (select body from public.messages where id = tests.seed_id('50', 'a', 1)),
  '',
  'other participants no longer receive the text of a deleted message'
);
select is(
  (select count(*)::int from public.deleted_message_bodies),
  0,
  'staff members cannot read archived message text'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  (select body from public.deleted_message_bodies where message_id = tests.seed_id('50', 'a', 1)),
  'Good morning, may I ask about the assignment?',
  'an organization admin can read the original text of a deleted message'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select is(
  (select count(*)::int from public.deleted_message_bodies),
  0,
  'an admin of another organization cannot read archived message text'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    $$insert into public.deleted_message_bodies (message_id, org_id, body, deleted_by)
      values (%L, %L, 'Forged', %L)$$,
    tests.seed_id('50', 'a', 2), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'clients cannot write archived message text'
);

select * from finish();
rollback;
