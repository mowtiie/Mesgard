begin;
select plan(11);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is(
  public.create_group_conversation(tests.seed_id('30', 'a', 2)),
  tests.seed_id('40', 'a', 2),
  'an existing group conversation is returned instead of creating a duplicate'
);

create temporary table created_conversation as
select public.create_group_conversation(tests.seed_id('30', 'a', 3)) as id;
grant all on created_conversation to authenticated;

select is(
  (select type || ':' || title from public.conversations where id = (select id from created_conversation)),
  'group:Science Department',
  'a new group conversation is titled with the group name'
);
select is(
  (select count(*)::int from public.conversation_participants where conversation_id = (select id from created_conversation)),
  2,
  'the group members become participants'
);
select is(
  public.create_group_conversation(tests.seed_id('30', 'a', 3)),
  (select id from created_conversation),
  'creating the same group conversation again is idempotent'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  public.create_group_conversation(tests.seed_id('30', 'a', 3)),
  (select id from created_conversation),
  'an organization admin who is not a member joins the existing conversation'
);
select ok(
  exists (
    select 1 from public.conversation_participants
    where conversation_id = (select id from created_conversation) and user_id = tests.seed_id('20', 'a', 1)
  ),
  'the caller becomes a participant'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format('select public.create_group_conversation(%L)', tests.seed_id('30', 'a', 3)),
  '42501',
  null,
  'a student cannot start a group conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 2));
select throws_ok(
  format('select public.create_group_conversation(%L)', tests.seed_id('30', 'a', 3)),
  '42501',
  null,
  'staff of another organization cannot start a group conversation'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  format('select public.create_group_conversation(%L)', tests.seed_id('30', 'a', 3)),
  '42501',
  null,
  'anonymous users cannot start a group conversation'
);

reset role;
insert into public.group_members (org_id, group_id, user_id)
values (tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 3), tests.seed_id('20', 'a', 4));
select ok(
  exists (
    select 1 from public.conversation_participants
    where conversation_id = (select id from created_conversation) and user_id = tests.seed_id('20', 'a', 4)
  ),
  'a new group member is added to the group conversation'
);

delete from public.group_members
where group_id = tests.seed_id('30', 'a', 3) and user_id = tests.seed_id('20', 'a', 4);
select ok(
  not exists (
    select 1 from public.conversation_participants
    where conversation_id = (select id from created_conversation) and user_id = tests.seed_id('20', 'a', 4)
  ),
  'a removed group member leaves the group conversation'
);

select * from finish();
rollback;
