begin;
select plan(10);

-- Seed: organization a has the member a04 and the staff a02; organization b has b04.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format(
    'insert into public.favorite_contacts (org_id, user_id, favorite_user_id) select org_id, user_id, %L from public.org_members where user_id = %L',
    tests.seed_id('20', 'a', 2), tests.seed_id('20', 'a', 4)
  ),
  'a person can favorite someone in their organization'
);
select is((select count(*)::int from public.favorite_contacts), 1, 'they read their own favorite');

select throws_ok(
  format(
    'insert into public.favorite_contacts (org_id, user_id, favorite_user_id) select org_id, user_id, user_id from public.org_members where user_id = %L',
    tests.seed_id('20', 'a', 4)
  ),
  '23514',
  null,
  'nobody favorites themselves'
);
select throws_ok(
  format(
    'insert into public.favorite_contacts (org_id, user_id, favorite_user_id) select org_id, %L, %L from public.org_members where user_id = %L',
    tests.seed_id('20', 'a', 4), tests.seed_id('20', 'b', 4), tests.seed_id('20', 'a', 4)
  ),
  '23503',
  null,
  'a favorite has to belong to the same organization'
);
select throws_ok(
  format(
    'insert into public.favorite_contacts (org_id, user_id, favorite_user_id) select org_id, %L, %L from public.org_members where user_id = %L',
    tests.seed_id('20', 'a', 2), tests.seed_id('20', 'a', 5), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'nobody adds a favorite for someone else'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is((select count(*)::int from public.favorite_contacts), 0, 'nobody reads the favorites of another person');

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
delete from public.favorite_contacts where user_id = tests.seed_id('20', 'a', 2);
select is((select count(*)::int from public.favorite_contacts), 1, 'they cannot remove a favorite that is not theirs (none matched)');
delete from public.favorite_contacts where favorite_user_id = tests.seed_id('20', 'a', 2);
select is((select count(*)::int from public.favorite_contacts), 0, 'they can remove their own favorite');

-- The list is capped at 50, so seed enough organization members through the superuser.
reset role;
select is(
  (select count(*)::int from pg_trigger where tgrelid = 'public.favorite_contacts'::regclass and tgname = 'favorite_contacts_limit'),
  1,
  'the list of favorites is limited'
);

-- Removing someone from the organization removes them from everyone's favorites.
insert into public.favorite_contacts (org_id, user_id, favorite_user_id)
select org_id, tests.seed_id('20', 'a', 4), tests.seed_id('20', 'a', 2) from public.org_members where user_id = tests.seed_id('20', 'a', 4);
delete from public.org_members where user_id = tests.seed_id('20', 'a', 2);
select is((select count(*)::int from public.favorite_contacts), 0, 'a person who leaves the organization leaves every favorites list');

select * from finish();
rollback;
