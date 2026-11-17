begin;
select plan(8);

-- Seed: the direct conversation 40-a-01 is between staff one (a02) and student one (a04), who has written in it.
reset role;
delete from public.org_members
where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'a', 4);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));

select is(
  (select count(*)::int from public.messages
   where conversation_id = tests.seed_id('40', 'a', 1) and sender_id = tests.seed_id('20', 'a', 4)),
  1,
  'the messages of the person who left stay readable'
);
select is(
  (select can_post from public.list_conversation_summaries(50, null) where conversation_id = tests.seed_id('40', 'a', 1)),
  false,
  'a direct conversation with one person left cannot be posted to'
);
select is(
  (select title from public.list_conversation_summaries(50, null) where conversation_id = tests.seed_id('40', 'a', 1)),
  null,
  'its title is empty, so the app can name it'
);
select throws_ok(
  format(
    'insert into public.messages (id, org_id, conversation_id, sender_id, body) values (gen_random_uuid(), %L, %L, %L, %L)',
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 2), 'Hello?'
  ),
  '42501',
  null,
  'a message cannot be sent into it'
);
select is(
  (select display_name is not null from public.profiles where id = tests.seed_id('20', 'a', 4)),
  true,
  'the name of the person who left stays readable for the people who can read their messages'
);
select is(
  private.can_post_to_conversation(tests.seed_id('40', 'a', 2)),
  true,
  'other conversations are not affected'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select is(
  (select count(*)::int from public.profiles where id = tests.seed_id('20', 'a', 4)),
  0,
  'people outside those conversations still cannot read the profile'
);

reset role;
select is(
  (select count(*)::int from public.conversation_participants where conversation_id = tests.seed_id('40', 'a', 1)),
  1,
  'the person who left is no longer a participant'
);

select * from finish();
rollback;
