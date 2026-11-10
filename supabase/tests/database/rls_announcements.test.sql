begin;
select plan(5);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, 'Student post')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 3), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a student cannot post in an announcement conversation'
);
select throws_ok(
  format(
    $$insert into public.conversations (id, org_id, type, title)
      values (gen_random_uuid(), %L, 'announcement', 'Student announcement')$$,
    tests.seed_id('10', 'a', 0)
  ),
  '42501',
  null,
  'a student cannot create an announcement conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, 'Staff post')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 3), tests.seed_id('20', 'a', 2)
  ),
  'a staff member can post in an announcement conversation'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, 'Admin post')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 3), tests.seed_id('20', 'a', 1)
  ),
  'an organization admin can post in an announcement conversation'
);
select lives_ok(
  format(
    $$insert into public.conversations (id, org_id, type, title)
      values (gen_random_uuid(), %L, 'announcement', 'Admin announcement')$$,
    tests.seed_id('10', 'a', 0)
  ),
  'an organization admin can create an announcement conversation'
);

select * from finish();
rollback;
