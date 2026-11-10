begin;
select plan(11);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  public.create_direct_conversation(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2)),
  tests.seed_id('40', 'a', 1),
  'an existing direct conversation is returned instead of creating a duplicate'
);

create temporary table created_conversation as
select public.create_direct_conversation(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)) as id;
grant all on created_conversation to authenticated;

select is(
  (select count(*)::int from public.conversation_participants where conversation_id = (select id from created_conversation)),
  2,
  'a new direct conversation has the caller and the other member as participants'
);
select is(
  (select type from public.conversations where id = (select id from created_conversation)),
  'direct',
  'the new conversation is a direct conversation'
);
select is(
  public.create_direct_conversation(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)),
  (select id from created_conversation),
  'creating the same direct conversation again is idempotent'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  public.create_direct_conversation(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)),
  (select id from created_conversation),
  'the other member reaches the same conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    'select public.create_direct_conversation(%L, %L)',
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)
  ),
  '22023',
  'A direct conversation needs another member',
  'a user cannot start a direct conversation with themselves'
);
select throws_ok(
  format(
    'select public.create_direct_conversation(%L, %L)',
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 4)
  ),
  '42501',
  'The other user is not a member of this organization',
  'a user cannot start a conversation with a member of another organization'
);
select throws_ok(
  format(
    'select public.create_direct_conversation(%L, %L)',
    tests.seed_id('10', 'b', 0), tests.seed_id('20', 'b', 4)
  ),
  '42501',
  'Only organization members can start a conversation',
  'a user cannot start a conversation in an organization they do not belong to'
);
select throws_ok(
  format(
    $$insert into public.conversations (id, org_id, type) values (gen_random_uuid(), %L, 'direct')$$,
    tests.seed_id('10', 'a', 0)
  ),
  '42501',
  null,
  'a direct conversation cannot be inserted directly'
);
select throws_ok(
  format(
    $$insert into public.conversation_participants (org_id, conversation_id, user_id) values (%L, %L, %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 6)
  ),
  '23514',
  'A direct conversation has at most two participants',
  'a third participant cannot be added to a direct conversation'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  format(
    'select public.create_direct_conversation(%L, %L)',
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2)
  ),
  '42501',
  null,
  'anonymous users cannot create conversations'
);

select * from finish();
rollback;
