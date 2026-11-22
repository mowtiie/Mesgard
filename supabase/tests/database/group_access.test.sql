begin;
select plan(11);

-- a04 (a member) creates a group in an organization where everyone may. a01 is the admin and a02 is staff.
insert into public.organizations (id, name, slug)
values ('00000000-0000-4000-8000-00000000e001', 'Break Glass School', 'break-glass');
insert into public.org_members (org_id, user_id, role) values
  ('00000000-0000-4000-8000-00000000e001', tests.seed_id('20', 'a', 1), 'org_admin'),
  ('00000000-0000-4000-8000-00000000e001', tests.seed_id('20', 'a', 2), 'staff'),
  ('00000000-0000-4000-8000-00000000e001', tests.seed_id('20', 'a', 4), 'member');

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
insert into public.groups (id, org_id, name, kind)
values ('30000000-0000-4000-8000-0000000e0001', '00000000-0000-4000-8000-00000000e001', 'Study Circle', 'other');
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
insert into public.groups (id, org_id, name)
values ('30000000-0000-4000-8000-0000000e0002', '00000000-0000-4000-8000-00000000e001', 'Faculty Room');

reset role;
insert into public.messages (id, org_id, conversation_id, sender_id, body)
select gen_random_uuid(), '00000000-0000-4000-8000-00000000e001', conversation.id, tests.seed_id('20', 'a', 4), body
from public.conversations as conversation, (values ('Chem quiz tomorrow'), ('Bring notes')) as texts (body)
where conversation.group_id = '30000000-0000-4000-8000-0000000e0001';
update public.messages set deleted_at = now() where body = 'Bring notes';

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  $$select public.open_group_messages('30000000-0000-4000-8000-0000000e0001', 'I would like to read this please')$$,
  '42501',
  null,
  'a member cannot open the messages of a group'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  $$select public.open_group_messages('30000000-0000-4000-8000-0000000e0002', 'There is no need for this group')$$,
  '22023',
  null,
  'a group an administrator made does not need it'
);
select throws_ok(
  $$select public.open_group_messages('30000000-0000-4000-8000-0000000e0001', 'too short')$$,
  '22023',
  null,
  'a reason of at least ten characters is needed'
);
select ok(
  public.open_group_messages('30000000-0000-4000-8000-0000000e0001', 'Report 2026-114 names a student here') > now() + interval '6 days',
  'with a reason an administrator opens the group for a week'
);
select is(
  (select count(*)::int from public.read_group_messages('30000000-0000-4000-8000-0000000e0001')),
  2,
  'and reads its messages'
);
select is(
  (select body from public.read_group_messages('30000000-0000-4000-8000-0000000e0001') where deleted_at is not null),
  null,
  'a deleted message shows no text'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  $$select * from public.read_group_messages('30000000-0000-4000-8000-0000000e0001')$$,
  '42501',
  null,
  'someone who did not open it cannot read it'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.list_group_access_notices('30000000-0000-4000-8000-0000000e0001')),
  1,
  'a member of the group can see that it was opened'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  $$select * from public.list_group_access_notices('30000000-0000-4000-8000-0000000e0001')$$,
  '42501',
  null,
  'a person outside the group cannot'
);

reset role;
update public.group_access_grants set expires_at = now() - interval '1 minute';
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  $$select * from public.read_group_messages('30000000-0000-4000-8000-0000000e0001')$$,
  '42501',
  null,
  'the access ends after a week'
);

reset role;
select is(
  (select metadata ->> 'reason' from public.audit_logs where action = 'group.messages_opened'),
  'Report 2026-114 names a student here',
  'the opening and its reason are in the audit log'
);

select * from finish();
rollback;
