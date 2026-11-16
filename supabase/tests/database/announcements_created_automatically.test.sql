begin;
select plan(6);

insert into public.organizations (id, name, slug)
values ('00000000-0000-4000-8000-00000000f001', 'New School', 'new-school');

select is(
  (select count(*)::int from public.conversations
   where org_id = '00000000-0000-4000-8000-00000000f001' and type = 'announcement'),
  0,
  'an organization without members has no announcement conversation yet'
);

insert into public.org_members (org_id, user_id, role)
values ('00000000-0000-4000-8000-00000000f001', tests.seed_id('20', 'a', 1), 'org_admin');

select is(
  (select title || ':' || created_by::text from public.conversations
   where org_id = '00000000-0000-4000-8000-00000000f001' and type = 'announcement'),
  'Announcements:' || tests.seed_id('20', 'a', 1)::text,
  'the first member creates the announcement conversation and owns it'
);
select is(
  (select count(*)::int from public.conversation_participants as participant
   join public.conversations as conversation on conversation.id = participant.conversation_id
   where conversation.org_id = '00000000-0000-4000-8000-00000000f001' and conversation.type = 'announcement'),
  1,
  'the first member takes part in it'
);

insert into public.org_members (org_id, user_id, role)
values ('00000000-0000-4000-8000-00000000f001', tests.seed_id('20', 'a', 4), 'student');

select is(
  (select count(*)::int from public.conversations
   where org_id = '00000000-0000-4000-8000-00000000f001' and type = 'announcement'),
  1,
  'a later member does not create a second announcement conversation'
);
select is(
  (select count(*)::int from public.conversation_participants as participant
   join public.conversations as conversation on conversation.id = participant.conversation_id
   where conversation.org_id = '00000000-0000-4000-8000-00000000f001' and conversation.type = 'announcement'),
  2,
  'a later member joins the announcement conversation'
);

select is(
  (select count(*)::int from public.conversations
   where org_id = tests.seed_id('10', 'a', 0) and type = 'announcement'),
  1,
  'an organization that already had announcements keeps a single conversation'
);

select * from finish();
rollback;
