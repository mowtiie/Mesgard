begin;
select plan(14);

-- A new organization starts with everyone allowed to create groups. a01 is its admin, a02 is staff and a04 a member.
insert into public.organizations (id, name, slug)
values ('00000000-0000-4000-8000-00000000d001', 'Open School', 'open-school');
insert into public.org_members (org_id, user_id, role) values
  ('00000000-0000-4000-8000-00000000d001', tests.seed_id('20', 'a', 1), 'org_admin'),
  ('00000000-0000-4000-8000-00000000d001', tests.seed_id('20', 'a', 2), 'staff'),
  ('00000000-0000-4000-8000-00000000d001', tests.seed_id('20', 'a', 4), 'member');

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  $$insert into public.groups (id, org_id, name, kind)
    values ('30000000-0000-4000-8000-0000000d0001', '00000000-0000-4000-8000-00000000d001', 'Chess', 'club')$$,
  'a member can create a club where everyone may'
);
select is(
  (select role_in_group from public.group_members
   where group_id = '30000000-0000-4000-8000-0000000d0001' and user_id = tests.seed_id('20', 'a', 4)),
  'owner',
  'the creator owns it'
);
select is(
  (select count(*)::int from public.conversation_participants as participant
   join public.conversations as conversation on conversation.id = participant.conversation_id
   where conversation.group_id = '30000000-0000-4000-8000-0000000d0001' and participant.user_id = tests.seed_id('20', 'a', 4)),
  1,
  'and it already has its conversation'
);
select throws_ok(
  $$insert into public.groups (id, org_id, name, parent_id)
    values (gen_random_uuid(), '00000000-0000-4000-8000-00000000d001', 'Inside', '30000000-0000-4000-8000-0000000d0001')$$,
  '23514',
  null,
  'a group a person creates cannot go inside another group'
);
select throws_ok(
  $$insert into public.groups (id, org_id, name, kind)
    values (gen_random_uuid(), '00000000-0000-4000-8000-00000000d001', 'Fake class', 'class')$$,
  '23514',
  null,
  'nor be a class or a section'
);
select throws_ok(
  $$insert into public.groups (id, org_id, name, includes_everyone)
    values (gen_random_uuid(), '00000000-0000-4000-8000-00000000d001', 'Everybody', true)$$,
  '23514',
  null,
  'nor add the whole organization'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select public.set_group_creation_policy('00000000-0000-4000-8000-00000000d001', 'staff');
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  $$insert into public.groups (id, org_id, name)
    values (gen_random_uuid(), '00000000-0000-4000-8000-00000000d001', 'Not now')$$,
  '42501',
  null,
  'when only staff may create groups a member cannot'
);
select throws_ok(
  $$select public.set_group_creation_policy('00000000-0000-4000-8000-00000000d001', 'everyone')$$,
  '42501',
  null,
  'and a member cannot change the policy'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select public.set_group_creation_policy('00000000-0000-4000-8000-00000000d001', 'everyone');
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  $$insert into public.groups (org_id, name) select '00000000-0000-4000-8000-00000000d001', 'Group ' || n from generate_series(2, 5) as n$$,
  'a person can create five groups'
);
select throws_ok(
  $$insert into public.groups (id, org_id, name)
    values (gen_random_uuid(), '00000000-0000-4000-8000-00000000d001', 'One too many')$$,
  '23514',
  null,
  'and no more'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is(
  (public.get_group_creation('00000000-0000-4000-8000-00000000d001') ->> 'can_create')::boolean,
  true,
  'staff can, and the answer says so'
);

-- Messages in a group a person created are not for administrators to read.
reset role;
insert into public.messages (id, org_id, conversation_id, sender_id, body)
select '50000000-0000-4000-8000-0000000d0001', '00000000-0000-4000-8000-00000000d001', conversation.id,
  tests.seed_id('20', 'a', 4), 'Private chess plan'
from public.conversations as conversation where conversation.group_id = '30000000-0000-4000-8000-0000000d0001';
update public.messages set deleted_at = now() where id = '50000000-0000-4000-8000-0000000d0001';

select is(
  (select count(*)::int from public.deleted_message_bodies where message_id = '50000000-0000-4000-8000-0000000d0001'),
  1,
  'the original of a deleted message is archived'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  (select count(*)::int from public.deleted_message_bodies where message_id = '50000000-0000-4000-8000-0000000d0001'),
  0,
  'but an administrator cannot read it'
);
select is(
  (select count(*)::int from public.export_person_messages(
    '00000000-0000-4000-8000-00000000d001', tests.seed_id('20', 'a', 4))),
  0,
  'and the export of a person does not hold their messages from such a group'
);

select * from finish();
rollback;
