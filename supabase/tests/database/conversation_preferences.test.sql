begin;
select plan(12);

-- Two more conversations for the student, so there are enough to reach the pin limit.
insert into public.conversations (id, org_id, type, title, created_by)
select tests.seed_id('40', 'a', number), conversation.org_id, 'group', 'Extra ' || number, tests.seed_id('20', 'a', 2)
from generate_series(5, 6) as number
cross join (select org_id from public.conversations where id = tests.seed_id('40', 'a', 1)) as conversation;
insert into public.conversation_participants (org_id, conversation_id, user_id)
select conversation.org_id, conversation.id, tests.seed_id('20', 'a', 4)
from public.conversations as conversation
where conversation.id in (tests.seed_id('40', 'a', 5), tests.seed_id('40', 'a', 6));

select tests.authenticate_as(tests.seed_id('20', 'a', 4));

select lives_ok(
  format(
    'update public.conversation_participants set pinned_at = now() where user_id = %L and conversation_id in (%L, %L, %L)',
    tests.seed_id('20', 'a', 4), tests.seed_id('40', 'a', 1), tests.seed_id('40', 'a', 2), tests.seed_id('40', 'a', 3)
  ),
  'three conversations can be pinned at once'
);
select is(
  (select count(*)::int from public.list_conversation_summaries() where pinned_at is not null),
  3,
  'the conversation list says which are pinned'
);
select throws_ok(
  format(
    'update public.conversation_participants set pinned_at = now() where user_id = %L and conversation_id = %L',
    tests.seed_id('20', 'a', 4), tests.seed_id('40', 'a', 5)
  ),
  '23514',
  'At most 3 conversations can be pinned',
  'a fourth pin is refused'
);
select lives_ok(
  format(
    'update public.conversation_participants set pinned_at = null where user_id = %L and conversation_id = %L',
    tests.seed_id('20', 'a', 4), tests.seed_id('40', 'a', 1)
  ),
  'a conversation can be unpinned'
);

select lives_ok(
  format(
    'update public.conversation_participants set marked_unread = true where user_id = %L and conversation_id = %L',
    tests.seed_id('20', 'a', 4), tests.seed_id('40', 'a', 1)
  ),
  'a conversation can be marked as unread'
);
select is(
  (select marked_unread from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 1)),
  true,
  'the conversation list says so'
);

update public.conversation_participants set pinned_at = now()
where user_id = tests.seed_id('20', 'a', 2) and conversation_id = tests.seed_id('40', 'a', 1);
select is(
  (
    select pinned_at from public.conversation_participants
    where user_id = tests.seed_id('20', 'a', 2) and conversation_id = tests.seed_id('40', 'a', 1)
  ),
  null,
  'nobody can change the preferences of another person'
);

-- Archive: a new message brings the conversation back, unless it is muted.
update public.conversation_participants set archived_at = now()
where user_id = tests.seed_id('20', 'a', 4) and conversation_id in (tests.seed_id('40', 'a', 1), tests.seed_id('40', 'a', 2));
update public.conversation_participants set muted = true
where user_id = tests.seed_id('20', 'a', 4) and conversation_id = tests.seed_id('40', 'a', 2);

reset role;
insert into public.messages (id, org_id, conversation_id, sender_id, body)
select tests.seed_id('50', 'a', 20 + chosen.number), conversation.org_id, conversation.id, tests.seed_id('20', 'a', 2), 'New'
from (values (1), (2)) as chosen (number)
join public.conversations as conversation on conversation.id = tests.seed_id('40', 'a', chosen.number);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select archived_at from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 1)),
  null,
  'a new message brings an archived conversation back'
);
select isnt(
  (select archived_at from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 2)),
  null,
  'a muted conversation stays archived'
);

-- Delete for me: hidden until a newer message arrives, and older messages do not count as unread.
update public.conversation_participants set hidden_before = now() + interval '1 second'
where user_id = tests.seed_id('20', 'a', 4) and conversation_id = tests.seed_id('40', 'a', 1);
select is(
  (select count(*)::int from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 1)),
  0,
  'a conversation deleted for me disappears from my list'
);

reset role;
insert into public.messages (id, org_id, conversation_id, sender_id, body, created_at)
select tests.seed_id('50', 'a', 30), org_id, id, tests.seed_id('20', 'a', 2), 'Back again', now() + interval '1 minute'
from public.conversations where id = tests.seed_id('40', 'a', 1);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select unread_count from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 1)),
  1::bigint,
  'a new message brings it back, and only the new message is unread'
);
select is(
  (select count(*)::int from public.conversation_participants where conversation_id = tests.seed_id('40', 'a', 1)),
  2,
  'the other participants stay in the conversation'
);

select * from finish();
rollback;
