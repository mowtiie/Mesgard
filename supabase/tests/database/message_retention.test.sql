begin;
select plan(41);

-- Seed: organization a has the administrator a01, staff a02, students a04, a05 and a06. Messages a01 to a08 belong to
-- conversations a01 (direct, a04 and a02), a02 (group, with an attachment on message a03), a03 (announcements, a01 and
-- a02 send) and a04 (direct, a05 and a06). Organization b has the same structure and never sets a limit.

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$select public.set_message_retention(%L, 365)$$, tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'staff cannot set the retention'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.set_message_retention(%L, 10)$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'the limit is at least 30 days'
);
select throws_ok(
  format($$select public.set_message_retention(%L, 365)$$, tests.seed_id('10', 'b', 0)),
  '42501',
  null,
  'an administrator cannot set the retention of another organization'
);
select lives_ok(
  format($$select public.set_message_retention(%L, 30)$$, tests.seed_id('10', 'a', 0)),
  'an administrator sets the retention'
);
select is(
  (select count(*)::int from public.audit_logs where action = 'organization.retention_changed'),
  1,
  'it is recorded in the audit log'
);
select lives_ok(
  format($$select public.set_message_retention(%L, 30)$$, tests.seed_id('10', 'a', 0)),
  'saving the same limit again is accepted'
);
select is(
  (select count(*)::int from public.audit_logs where action = 'organization.retention_changed'),
  1,
  'and records nothing more'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  public.get_message_retention(tests.seed_id('10', 'a', 0)),
  30,
  'a member can read the limit of their organization'
);
select throws_ok(
  format($$select public.get_message_retention(%L)$$, tests.seed_id('10', 'b', 0)),
  '42501',
  null,
  'and not the limit of another one'
);
select throws_ok(
  'select public.expire_messages(10)',
  '42501',
  null,
  'nobody who is signed in can run the job'
);

-- Old messages, an archived original and an earlier version.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
update public.messages set deleted_at = now() where id = tests.seed_id('50', 'a', 1);
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
update public.messages set body = 'An edited reply' where id = tests.seed_id('50', 'a', 2);

reset role;
update public.messages set created_at = now() - interval '60 days'
where id in (select tests.seed_id('50', l, n) from (values ('a'), ('b')) as org (l), generate_series(1, 4) as n)
   or id in (tests.seed_id('50', 'a', 7), tests.seed_id('50', 'a', 8));
select is(
  (select count(*)::int from public.deleted_message_bodies where message_id = tests.seed_id('50', 'a', 1)),
  1,
  'the original of a deleted message is archived'
);
select is(
  (select count(*)::int from public.message_edits where message_id = tests.seed_id('50', 'a', 2)),
  1,
  'and so is the earlier version of an edited one'
);

-- Protected messages stay: a hold on a05, a hold on the group a02 and an open report about a06 and by a04.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.place_legal_hold(%L, 'A court order was received', %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)),
  'a hold is placed on a person'
);
select lives_ok(
  format($$select public.place_legal_hold(%L, 'The class chat is part of an inquiry', null, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 2)),
  'and one on a group'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format($$select public.submit_report(%L, %L, 'bullying', null, %L, 'Messages after class')$$, tests.seed_id('70', 'a', 1), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 6)),
  'a student reports another student'
);

reset role;
select is(
  (select count(*)::int from public.list_expired_attachment_paths(100)),
  0,
  'the file of a protected message is not listed for removal'
);
select is(public.expire_messages(100), 1, 'only the unprotected old message is erased');
select is(
  (select body || '|' || (deleted_at is not null)::text from public.messages where id = tests.seed_id('50', 'a', 2)),
  '|true',
  'its text is blanked'
);
select is(
  (select count(*)::int from public.message_edits where message_id = tests.seed_id('50', 'a', 2)),
  0,
  'and its earlier version is purged'
);
select is(
  (select count(*)::int from public.messages where deleted_at is null and id in (
    tests.seed_id('50', 'a', 3), tests.seed_id('50', 'a', 4), tests.seed_id('50', 'a', 7), tests.seed_id('50', 'a', 8)
  )),
  4,
  'the messages under a hold or an open report keep their text'
);
select is(
  (select count(*)::int from public.deleted_message_bodies where message_id = tests.seed_id('50', 'a', 1)),
  1,
  'so does the archive of the reporter'
);
select is(
  (select count(*)::int from public.attachments where message_id = tests.seed_id('50', 'a', 3)),
  1,
  'and the attachment of a message in a group under a hold'
);
select is(
  (select count(*)::int from public.messages where deleted_at is null and org_id = tests.seed_id('10', 'b', 0)),
  8,
  'an organization without a limit loses nothing'
);
select is(
  (select count(*)::int from public.messages where deleted_at is null and id in (tests.seed_id('50', 'a', 5), tests.seed_id('50', 'a', 6))),
  2,
  'recent messages stay'
);

-- Releasing the hold on the person lets their message go.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format(
    $$select public.release_legal_hold(hold_id, 'The order was withdrawn') from public.list_legal_holds(%L) where scope = 'person'$$,
    tests.seed_id('10', 'a', 0)
  ),
  'the hold on the person is released'
);
reset role;
select is(public.expire_messages(100), 1, 'their old message is erased on the next run');

-- Closing the report and releasing the group lets the rest go, files first.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.update_report(%L, 'dismissed', 'Not enough information')$$, tests.seed_id('70', 'a', 1)),
  'the report is closed'
);
select lives_ok(
  format(
    $$select public.release_legal_hold(hold_id, 'The inquiry is over') from public.list_legal_holds(%L) where scope = 'group'$$,
    tests.seed_id('10', 'a', 0)
  ),
  'the hold on the group is released'
);
reset role;
select results_eq(
  $$select path from public.list_expired_attachment_paths(100) as path$$,
  $$values ('rnhs/proposals/template.pdf'::text)$$,
  'the file is listed so the job can delete it before the rows go'
);
select is(public.expire_messages(100), 4, 'the remaining old messages are erased');
select is(
  (select count(*)::int from public.deleted_message_bodies where message_id = tests.seed_id('50', 'a', 1)),
  0,
  'the archive of the deleted message is purged'
);
select is(
  (select count(*)::int from public.attachments where message_id = tests.seed_id('50', 'a', 3)),
  0,
  'the attachment rows are gone'
);
select is(public.expire_messages(100), 0, 'running the job again changes nothing');
select is(
  (select count(*)::int from public.messages where deleted_at is null and org_id = tests.seed_id('10', 'a', 0)),
  2,
  'only the recent messages of the organization keep their text'
);
select is(
  (select count(*)::int from public.report_evidence),
  0,
  'a report about a person has no frozen message to keep'
);

-- Turning the limit off stops the job.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.set_message_retention(%L, null)$$, tests.seed_id('10', 'a', 0)),
  'an administrator keeps messages without a limit again'
);
reset role;
update public.messages set created_at = now() - interval '90 days' where id = tests.seed_id('50', 'a', 5);
select is(public.expire_messages(100), 0, 'without a limit nothing expires, however old');

-- The batch is bounded.
select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select lives_ok(
  format($$select public.set_message_retention(%L, 30)$$, tests.seed_id('10', 'b', 0)),
  'the other organization sets a limit'
);
reset role;
select is(public.expire_messages(2), 2, 'a run handles at most the batch it is given');
select is(public.expire_messages(100), 2, 'and the next run takes the rest');

update private.mfa_settings set enforced = true;
select tests.authenticate_as(tests.seed_id('20', 'a', 1), 'aal1');
select throws_ok(
  format($$select public.get_message_retention(%L)$$, tests.seed_id('10', 'a', 0)),
  '42501',
  'Two-step verification is required',
  'a person who has not entered the code cannot read the limit'
);

select * from finish();
rollback;
