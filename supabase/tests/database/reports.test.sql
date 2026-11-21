begin;
select plan(58);

-- Seed: organization a has the administrator a01, staff a02 and a03, students a04, a05 and a06. Conversation a01 is
-- direct between a04 and a02, message a02 is the reply of a02, message a01 is a04's own message.

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format(
    $$select public.submit_report(%L, %L, 'bullying', %L, null, 'Unkind reply', true)$$,
    tests.seed_id('70', 'a', 1), tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 2)
  ),
  'a participant can report a message anonymously'
);
select is(
  (select status from public.reports where id = tests.seed_id('70', 'a', 1)),
  'open',
  'the reporter follows the status of their own report'
);
select lives_ok(
  format(
    $$select public.submit_report(%L, %L, 'bullying', %L, null, 'Unkind reply', true)$$,
    tests.seed_id('70', 'a', 1), tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 2)
  ),
  'sending the same report again is idempotent'
);
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'bullying', %L)$$,
    tests.seed_id('70', 'a', 9), tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 2)
  ),
  '23505',
  null,
  'the same person cannot report the same message twice'
);
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', %L)$$,
    tests.seed_id('70', 'a', 9), tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 1)
  ),
  '22023',
  null,
  'nobody reports their own message'
);
select throws_ok(
  format($$select public.submit_report(%L, %L, 'other')$$, tests.seed_id('70', 'a', 9), tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'a report needs a message or a person'
);
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', %L, %L)$$,
    tests.seed_id('70', 'a', 9), tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 2), tests.seed_id('20', 'a', 5)
  ),
  '22023',
  null,
  'a report cannot name a message and a person'
);
select lives_ok(
  format(
    $$select public.submit_report(%L, %L, 'harassment', null, %L, 'Messages after class')$$,
    tests.seed_id('70', 'a', 2), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)
  ),
  'a person can report another person of their organization'
);
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'harassment', null, %L)$$,
    tests.seed_id('70', 'a', 9), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 4)
  ),
  'P0002',
  null,
  'nobody reports a person of another organization'
);
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', null, %L)$$,
    tests.seed_id('70', 'a', 9), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)
  ),
  '22023',
  null,
  'nobody reports themselves'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 6));
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'bullying', %L)$$,
    tests.seed_id('70', 'a', 9), tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 2)
  ),
  'P0002',
  null,
  'someone outside the conversation cannot report its message'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', null, %L)$$,
    tests.seed_id('70', 'a', 1), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)
  ),
  '23505',
  null,
  'a report id cannot be taken over by someone else'
);
select is((select count(*)::int from public.reports), 0, 'nobody reads the reports of another person');

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  'select count(*) from public.report_events',
  '42501',
  null,
  'even an administrator cannot read the history table directly'
);
select throws_ok(
  'select reporter_id from public.reports',
  '42501',
  null,
  'nobody can select the reporter column'
);

-- Reading cases: with no designated reviewer the organization admins are the reviewers.
select is(
  (select count(*)::int from public.list_reports(tests.seed_id('10', 'a', 0))),
  2,
  'an administrator reads the reports when no reviewer is designated'
);
select is(
  (select reporter_name from public.list_reports(tests.seed_id('10', 'a', 0)) where report_id = tests.seed_id('70', 'a', 1)),
  null,
  'an anonymous report hides the reporter'
);
select is(
  (select reporter_name from public.list_reports(tests.seed_id('10', 'a', 0)) where report_id = tests.seed_id('70', 'a', 2)),
  'Student One RNHS',
  'a report that is not anonymous names the reporter'
);
select is(
  (select count(*)::int from public.list_reports(tests.seed_id('10', 'a', 0), 'resolved')),
  0,
  'the list can be filtered by status'
);
select is(
  public.get_report(tests.seed_id('70', 'a', 1)) -> 'evidence' ->> 'body',
  'Of course. It is due on Friday.',
  'the case holds a copy of the message'
);
select is(
  jsonb_array_length(public.get_report(tests.seed_id('70', 'a', 1)) -> 'evidence' -> 'context'),
  1,
  'the case holds the messages before it'
);
select is(
  public.get_report(tests.seed_id('70', 'a', 1)) -> 'reporter_name',
  'null'::jsonb,
  'the case of an anonymous report has no reporter name'
);

reset role;
select is(
  (select count(*)::int from public.report_events where report_id = tests.seed_id('70', 'a', 1) and action = 'viewed'),
  3,
  'every time a reviewer opens a case it is recorded'
);
select is(
  (select actor_id from public.report_events where report_id = tests.seed_id('70', 'a', 1) and action = 'submitted'),
  null,
  'the history of an anonymous report has no reporter'
);

-- The evidence does not change when the sender edits or deletes the message.
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
update public.messages set body = 'Changed afterwards' where id = tests.seed_id('50', 'a', 2);
update public.messages set deleted_at = now() where id = tests.seed_id('50', 'a', 2);
reset role;
select is(
  (select body from public.report_evidence where report_id = tests.seed_id('70', 'a', 1)),
  'Of course. It is due on Friday.',
  'the evidence keeps the text the reporter saw, after an edit and a deletion'
);
select throws_ok(
  $$update public.report_evidence set body = 'x'$$,
  '23001',
  null,
  'evidence cannot be changed'
);
select throws_ok(
  'delete from public.report_events',
  '23001',
  null,
  'the history cannot be deleted'
);
select throws_ok(
  'truncate public.report_evidence_files',
  '23001',
  null,
  'evidence files cannot be removed in bulk'
);

-- Designating reviewers.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format($$select public.set_report_reviewers(%L, array[%L]::uuid[])$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 3)),
  '42501',
  null,
  'a student cannot choose the reviewers'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.set_report_reviewers(%L, array[%L]::uuid[])$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)),
  '22023',
  null,
  'a student cannot be a reviewer'
);
select lives_ok(
  format($$select public.set_report_reviewers(%L, array[%L]::uuid[])$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 3)),
  'an administrator designates a reviewer'
);
select is(
  (select count(*)::int from public.list_report_reviewers(tests.seed_id('10', 'a', 0))),
  1,
  'the administrator reads the reviewers'
);
select throws_ok(
  format($$select count(*) from public.list_reports(%L)$$, tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'once reviewers are designated an administrator who is not one no longer reads the reports'
);
select is(
  (select count(*)::int from public.audit_logs where action = 'report_reviewers.changed'),
  1,
  'choosing the reviewers is recorded in the audit log'
);

-- Handling a case.
select tests.authenticate_as(tests.seed_id('20', 'a', 3));
select is(
  (select count(*)::int from public.list_reports(tests.seed_id('10', 'a', 0))),
  2,
  'the designated reviewer reads the reports'
);
select throws_ok(
  format($$select public.reveal_report_reporter(%L, 'Checking a false report claim')$$, tests.seed_id('70', 'a', 1)),
  '42501',
  null,
  'a reviewer who is not an administrator cannot reveal a reporter'
);
select throws_ok(
  format($$select public.update_report(%L, 'resolved')$$, tests.seed_id('70', 'a', 1)),
  '22023',
  null,
  'closing a report needs a note'
);
select lives_ok(
  format($$select public.update_report(%L, 'in_review')$$, tests.seed_id('70', 'a', 1)),
  'a reviewer starts a review'
);
select throws_ok(
  format($$select public.update_report(%L, 'in_review')$$, tests.seed_id('70', 'a', 1)),
  '22023',
  null,
  'the status has to change'
);
select throws_ok(
  format($$select public.update_report(%L, 'resolved', 'Spoke with both students', true)$$, tests.seed_id('70', 'a', 2)),
  '22023',
  null,
  'a report about a person has no message to remove'
);
select lives_ok(
  format($$select public.update_report(%L, 'resolved', 'Spoke with both students', true)$$, tests.seed_id('70', 'a', 1)),
  'a reviewer resolves a report and removes the message'
);
select throws_ok(
  format($$select public.update_report(%L, 'dismissed', 'Changed my mind')$$, tests.seed_id('70', 'a', 1)),
  '22023',
  null,
  'a closed report cannot be changed'
);
select lives_ok(
  format($$select public.update_report(%L, 'dismissed', 'Not enough information')$$, tests.seed_id('70', 'a', 2)),
  'a reviewer dismisses a report'
);

reset role;
select is(
  (select status || ': ' || resolution_note from public.reports where id = tests.seed_id('70', 'a', 1)),
  'resolved: Spoke with both students',
  'the resolution is kept on the report'
);

-- Revealing a reporter.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.reveal_report_reporter(%L, 'Too short')$$, tests.seed_id('70', 'a', 1)),
  '22023',
  null,
  'revealing a reporter needs a real reason'
);
select is(
  public.reveal_report_reporter(tests.seed_id('70', 'a', 1), 'Checking a false report claim') ->> 'reporter_id',
  tests.seed_id('20', 'a', 4)::text,
  'an administrator can reveal the reporter of an anonymous report'
);
select is(
  (select count(*)::int from public.audit_logs where action = 'report.reporter_revealed'),
  1,
  'revealing a reporter is recorded in the audit log'
);

-- Files kept as evidence, and the files of a removed message.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('message-attachments', '%s/%s/photo', %L, '{"size": 2048, "mimetype": "image/png"}')$$,
    tests.seed_id('40', 'a', 1), tests.seed_id('50', 'a', 97), tests.seed_id('20', 'a', 4)
  ),
  'a participant uploads a file'
);
select lives_ok(
  format(
    $$select public.send_message(%L, %L, '', jsonb_build_array(
      jsonb_build_object('id', %L, 'storage_path', '%s/%s/photo', 'file_name', 'photo.png')))$$,
    tests.seed_id('50', 'a', 97), tests.seed_id('40', 'a', 1), tests.seed_id('60', 'a', 97),
    tests.seed_id('40', 'a', 1), tests.seed_id('50', 'a', 97)
  ),
  'and sends it in a message'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  format(
    $$select public.submit_report(%L, %L, 'child_safety', %L)$$,
    tests.seed_id('70', 'a', 3), tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 97)
  ),
  'staff can report a message that carries a file'
);

reset role;
select is(
  (select count(*)::int from public.report_evidence_files where report_id = tests.seed_id('70', 'a', 3)),
  1,
  'the evidence keeps the file details'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.set_report_reviewers(%L, '{}'::uuid[])$$, tests.seed_id('10', 'a', 0)),
  'with no designated reviewer the administrators review again'
);
select is(
  (select count(*)::int from storage.objects where bucket_id = 'message-attachments'
    and name = tests.seed_id('40', 'a', 1)::text || '/' || tests.seed_id('50', 'a', 97)::text || '/photo'),
  1,
  'a reviewer who is not in the conversation reads the file kept as evidence'
);
select lives_ok(
  format($$select public.update_report(%L, 'resolved', 'The file was removed', true)$$, tests.seed_id('70', 'a', 3)),
  'a reviewer resolves the report and removes the message'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from storage.objects where bucket_id = 'message-attachments'
    and name = tests.seed_id('40', 'a', 1)::text || '/' || tests.seed_id('50', 'a', 97)::text || '/photo'),
  0,
  'participants no longer read the file of a removed message'
);
select is(
  (select count(*)::int from public.attachments where message_id = tests.seed_id('50', 'a', 97)),
  0,
  'and no longer see its attachment'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  (select count(*)::int from storage.objects where bucket_id = 'message-attachments'
    and name = tests.seed_id('40', 'a', 1)::text || '/' || tests.seed_id('50', 'a', 97)::text || '/photo'),
  1,
  'the reviewer still reads the file kept as evidence'
);

-- Two-step verification.
reset role;
update private.mfa_settings set enforced = true;
select tests.authenticate_as(tests.seed_id('20', 'a', 1), 'aal1');
select throws_ok(
  format($$select public.list_reports(%L)$$, tests.seed_id('10', 'a', 0)),
  '42501',
  'Two-step verification is required',
  'an administrator who has not entered the code cannot read reports'
);

select * from finish();
rollback;
