begin;
select plan(24);

-- Seed: conversation 40-a-01 is direct between staff one (a02) and member one (a04); message 50-a-01 is from a04 and
-- 50-a-02 from a02. Conversation 40-a-02 is a group with a02 and a04; 40-a-04 is direct between a05 and a06.

-- Edit: the earlier text is archived, and only organization admins can read it.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
update public.messages set body = 'Good morning, may I ask about the assignment today?' where id = tests.seed_id('50', 'a', 1);
select is(
  (select count(*)::int from public.message_edits),
  0,
  'the sender cannot read the archive of their own edit'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  (select body from public.message_edits where message_id = tests.seed_id('50', 'a', 1)),
  'Good morning, may I ask about the assignment?',
  'an administrator reads the text from before the edit'
);
select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select is((select count(*)::int from public.message_edits), 0, 'an administrator of another organization reads nothing');

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
update public.messages set body = 'Good morning, may I ask about the assignment today?' where id = tests.seed_id('50', 'a', 1);
reset role;
select is(
  (select count(*)::int from public.message_edits where message_id = tests.seed_id('50', 'a', 1)),
  1,
  'saving the same text again archives nothing'
);

-- Reply and mentions.
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
insert into public.messages (id, org_id, conversation_id, sender_id, body, reply_to_message_id)
values (
  '51000000-0000-4000-8000-000000000a01', tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1),
  tests.seed_id('20', 'a', 2), 'Replying to you', tests.seed_id('50', 'a', 1)
);
select is(
  (select reply_to_message_id from public.messages where id = '51000000-0000-4000-8000-000000000a01'),
  tests.seed_id('50', 'a', 1),
  'a message can reply to a message of the same conversation'
);
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body, reply_to_message_id)
      values (gen_random_uuid(), %L, %L, %L, 'Wrong place', %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 2), tests.seed_id('50', 'a', 8)
  ),
  '23503',
  null,
  'a reply cannot point to a message of another conversation'
);

insert into public.messages (id, org_id, conversation_id, sender_id, body, mentioned_user_ids)
values (
  '51000000-0000-4000-8000-000000000a02', tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1),
  tests.seed_id('20', 'a', 2), '@Student One please check', array[tests.seed_id('20', 'a', 4)]
);
select is(
  (select mentioned_user_ids from public.messages where id = '51000000-0000-4000-8000-000000000a02'),
  array[tests.seed_id('20', 'a', 4)],
  'a participant can be mentioned'
);
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body, mentioned_user_ids)
      values (gen_random_uuid(), %L, %L, %L, 'Stranger', array[%L::uuid])$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 2), tests.seed_id('20', 'a', 5)
  ),
  '22023',
  'Only people in the conversation can be mentioned',
  'someone outside the conversation cannot be mentioned'
);
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body, mentioned_user_ids)
      values (gen_random_uuid(), %L, %L, %L, 'Crowd', array_fill(%L::uuid, array[21]))$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 2), tests.seed_id('20', 'a', 4)
  ),
  '23514',
  null,
  'at most 20 people can be mentioned'
);

select is(
  (select public.send_message(
    '51000000-0000-4000-8000-000000000a03', tests.seed_id('40', 'a', 1), 'Through the function',
    '[]'::jsonb, tests.seed_id('50', 'a', 1), array[tests.seed_id('20', 'a', 4)]
  ) is not null),
  true,
  'send_message accepts a reply and mentions'
);
select is(
  (select reply_to_message_id from public.messages where id = '51000000-0000-4000-8000-000000000a03'),
  tests.seed_id('50', 'a', 1),
  'the reply from send_message is stored'
);

reset role;
delete from public.messages where id = tests.seed_id('50', 'a', 1);
select is(
  (select reply_to_message_id from public.messages where id = '51000000-0000-4000-8000-000000000a01'),
  null,
  'a reply keeps its text and loses the link when the original is removed'
);

-- A mention breaks through mute.
insert into public.push_subscriptions (user_id, endpoint, p256dh, auth_secret)
values (tests.seed_id('20', 'a', 4), 'https://push.example/endpoint-one', repeat('a', 20), repeat('b', 20));
update public.conversation_participants set muted = true
where conversation_id = tests.seed_id('40', 'a', 1) and user_id = tests.seed_id('20', 'a', 4);
set local role service_role;
select is(
  (select count(distinct recipient_id)::int from public.list_push_deliveries('51000000-0000-4000-8000-000000000a02')),
  1,
  'a muted person is still notified when mentioned'
);
reset role;
insert into public.messages (id, org_id, conversation_id, sender_id, body)
values ('51000000-0000-4000-8000-000000000a04', tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1),
  tests.seed_id('20', 'a', 2), 'No mention here');
set local role service_role;
select is(
  (select count(*)::int from public.list_push_deliveries('51000000-0000-4000-8000-000000000a04')),
  0,
  'a muted person is not notified without a mention'
);
reset role;

-- Pins: administrators and staff only, up to five, and removed with the message.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    $$insert into public.pinned_messages (message_id, org_id, conversation_id) values (%L, %L, %L)$$,
    '51000000-0000-4000-8000-000000000a02', tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1)
  ),
  '42501',
  null,
  'a member cannot pin a message'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
insert into public.pinned_messages (message_id, org_id, conversation_id)
values ('51000000-0000-4000-8000-000000000a02', tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1));
select is((select count(*)::int from public.pinned_messages), 1, 'staff can pin a message');
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is((select count(*)::int from public.pinned_messages), 1, 'every participant sees the pinned message');
select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is((select count(*)::int from public.pinned_messages), 0, 'someone outside the conversation sees no pins');

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
update public.messages set deleted_at = now() where id = '51000000-0000-4000-8000-000000000a02';
select is((select count(*)::int from public.pinned_messages), 0, 'deleting a message unpins it');

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  (select count(*)::int from public.audit_logs where action in ('message.pinned', 'message.unpinned')),
  2,
  'pinning and the unpin that follows a delete are recorded in the audit log'
);

-- Search.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.search_messages(tests.seed_id('40', 'a', 1), 'DUE ON')),
  1,
  'search finds part of a word, ignoring case'
);
select is(
  (select count(*)::int from public.search_messages(tests.seed_id('40', 'a', 1), '%%')),
  0,
  'a percent sign is searched as text'
);
select throws_ok(
  format('select * from public.search_messages(%L, %L)', tests.seed_id('40', 'a', 1), 'a'),
  '22023',
  'Search for at least 2 characters',
  'a search needs at least two characters'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  (select count(*)::int from public.search_messages(tests.seed_id('40', 'a', 1), 'due on')),
  0,
  'someone outside the conversation finds nothing'
);

select * from finish();
rollback;
