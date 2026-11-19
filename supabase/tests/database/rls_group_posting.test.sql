begin;
select plan(9);

-- Seed: conversation 40-a-03 is the group "School Announcements" (group 30-a-04), limited to moderators. Staff a02 and a03
-- are its moderators, a01 is an organization admin, a04 is a member. Conversation 40-a-02 is an ordinary group.

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, 'Member post')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 3), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a member cannot post in a group that limits posting to moderators'
);
select is(
  (select can_post from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 3)),
  false,
  'the conversation list says so'
);
select lives_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, 'Member post')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 2), tests.seed_id('20', 'a', 4)
  ),
  'a member can post in an ordinary group'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, 'Moderator post')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 3), tests.seed_id('20', 'a', 2)
  ),
  'a moderator of the group can post'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, 'Admin post')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 3), tests.seed_id('20', 'a', 1)
  ),
  'an organization admin can always post'
);

select throws_ok(
  format(
    $$insert into public.conversations (id, org_id, type, title, created_by)
      values (gen_random_uuid(), %L, 'announcement', 'Old kind', %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 1)
  ),
  '42501',
  null,
  'there is no announcement conversation type any more'
);

-- Making a moderator a plain member takes the right to post away.
reset role;
update public.group_members set role_in_group = 'member'
where group_id = tests.seed_id('30', 'a', 4) and user_id = tests.seed_id('20', 'a', 3);
select tests.authenticate_as(tests.seed_id('20', 'a', 3));
select is(
  (select can_post from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 3)),
  false,
  'a person who stops being a moderator stops being able to post'
);

-- Choosing "everyone can post" opens the group.
reset role;
update public.groups set posting_policy = 'everyone' where id = tests.seed_id('30', 'a', 4);
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select can_post from public.list_conversation_summaries() where conversation_id = tests.seed_id('40', 'a', 3)),
  true,
  'when the group lets everyone post, a member can'
);

-- Only organization admins choose the posting policy.
update public.groups set posting_policy = 'moderators' where id = tests.seed_id('30', 'a', 4);
select is(
  (select posting_policy from public.groups where id = tests.seed_id('30', 'a', 4)),
  'everyone',
  'a member cannot change who can post'
);

select * from finish();
rollback;
