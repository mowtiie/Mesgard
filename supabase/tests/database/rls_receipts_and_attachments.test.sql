begin;
select plan(9);

select tests.authenticate_as(tests.seed_id('20', 'a', 6));
select is(
  (select count(*)::int from public.attachments),
  0,
  'a non-participant cannot read attachments'
);
select is(
  (select count(*)::int from public.message_receipts),
  0,
  'a non-participant cannot read message receipts'
);
select throws_ok(
  format(
    $$insert into public.message_receipts (org_id, message_id, user_id) values (%L, %L, %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 1), tests.seed_id('20', 'a', 6)
  ),
  '42501',
  null,
  'a non-participant cannot record a receipt for a message'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from public.attachments where message_id = tests.seed_id('50', 'a', 3)),
  1,
  'a participant can read attachments of a message'
);
select is(
  (select count(*)::int from public.message_receipts where message_id = tests.seed_id('50', 'a', 1)),
  1,
  'a sender can read the receipts of their message'
);
select throws_ok(
  format(
    $$insert into public.attachments (org_id, message_id, storage_path, mime_type, size_bytes)
      values (%L, %L, 'rnhs/forged.pdf', 'application/pdf', 10)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 3)
  ),
  '42501',
  null,
  'a participant cannot attach a file to the message of another user'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  format(
    $$insert into public.attachments (org_id, message_id, storage_path, mime_type, size_bytes)
      values (%L, %L, 'rnhs/proposals/rubric.pdf', 'application/pdf', 2048)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 3)
  ),
  'a sender can attach a file to their message'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  (
    with removed as (
      delete from public.attachments where message_id = tests.seed_id('50', 'a', 3) returning 1
    )
    select count(*)::int from removed
  ),
  0,
  'a participant cannot remove an attachment from the message of another user'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select is(
  (select count(*)::int from public.attachments where org_id = tests.seed_id('10', 'a', 0)),
  0,
  'a member of another organization cannot read attachments'
);

select * from finish();
rollback;
