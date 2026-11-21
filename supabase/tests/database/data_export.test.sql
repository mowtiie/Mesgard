begin;
select plan(24);

-- Seed: organization a has the administrator a01, staff a02, students a04, a05 and a06. Student a04 belongs to the
-- groups a02 and a04, to the conversations a01, a02 and a03, and sent messages a01 and a04. Staff a02 sent messages
-- a02, a03 (with an attachment) and a06.

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format($$select public.export_person_overview(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)),
  '42501',
  null,
  'a person cannot export their own data through the administration function'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$select * from public.export_person_messages(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)),
  '42501',
  null,
  'staff cannot export messages'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select throws_ok(
  format($$select public.export_person_overview(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)),
  '42501',
  null,
  'an administrator of another organization cannot export'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.export_person_overview(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 4)),
  'P0002',
  null,
  'a person of another organization is not found'
);

-- The overview.
select is(
  public.export_person_overview(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)) -> 'profile' ->> 'display_name',
  'Student One RNHS',
  'the overview has the profile'
);
select is(
  public.export_person_overview(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)) -> 'account' ->> 'email',
  'student1@rnhs.test',
  'the account email'
);
select is(
  public.export_person_overview(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)) -> 'membership' ->> 'role',
  'member',
  'and the role'
);
select is(
  jsonb_array_length(public.export_person_overview(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)) -> 'groups'),
  2,
  'the groups of the person'
);
select is(
  jsonb_array_length(public.export_person_overview(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)) -> 'conversations'),
  3,
  'the conversations of the person'
);
select is(
  (public.export_person_overview(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)) ->> 'messages_sent')::int,
  2,
  'how many messages they sent'
);
select is(
  public.export_person_overview(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)) ? 'password',
  false,
  'no password or secret is in the overview'
);
select is(
  (select count(*)::int from public.audit_logs where action = 'member.data_exported' and target_id = tests.seed_id('20', 'a', 4)),
  7,
  'every export is recorded in the audit log, by the administrator'
);

-- The messages, newest first, in pages.
select is(
  (select count(*)::int from public.export_person_messages(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2))),
  3,
  'a person has the messages they sent, and only those'
);
select is(
  (select count(*)::int from public.export_person_messages(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2), 2)),
  2,
  'a page holds at most the size asked for'
);
select results_eq(
  format(
    $$select message_id from public.export_person_messages(%L, %L, 2, (select page.created_at from public.export_person_messages(%L, %L) as page where page.message_id = %L), %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2), tests.seed_id('50', 'a', 3), tests.seed_id('50', 'a', 3)
  ),
  format($$values (%L::uuid)$$, tests.seed_id('50', 'a', 2)),
  'the next page starts after the last message of the first'
);
select is(
  (select attachments -> 0 ->> 'mime_type' from public.export_person_messages(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2))
   where message_id = tests.seed_id('50', 'a', 3)),
  'application/pdf',
  'the details of an attachment are included, not the file'
);
select is(
  (select count(*)::int from public.export_person_messages(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2)) as page
   where page.message_id in (select id from public.messages where sender_id <> tests.seed_id('20', 'a', 2))),
  0,
  'messages of other people are never included'
);

-- Originals of deleted messages and earlier versions of edited ones.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
update public.messages set deleted_at = now() where id = tests.seed_id('50', 'a', 1);
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
update public.messages set body = 'A corrected reply' where id = tests.seed_id('50', 'a', 2);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select results_eq(
  format(
    $$select body, deleted_original from public.export_person_messages(%L, %L) where message_id = %L$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4), tests.seed_id('50', 'a', 1)
  ),
  $$values (''::text, 'Good morning, may I ask about the assignment?'::text)$$,
  'a deleted message is exported with its original text'
);
select is(
  (select earlier_versions -> 0 ->> 'text' from public.export_person_messages(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 2))
   where message_id = tests.seed_id('50', 'a', 2)),
  'Of course. It is due on Friday.',
  'an edited message is exported with its earlier version'
);

-- A person who left is still exported.
reset role;
delete from public.org_members where user_id = tests.seed_id('20', 'a', 4);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.export_person_overview(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)),
  'a former member can be exported while their messages remain'
);
select is(
  (select count(*)::int from public.export_person_messages(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4))),
  2,
  'with their messages'
);
select throws_ok(
  format($$select public.export_person_overview(%L, %L)$$, tests.seed_id('10', 'a', 0), gen_random_uuid()),
  'P0002',
  null,
  'someone the organization never had is not found'
);

reset role;
update private.mfa_settings set enforced = true;
select tests.authenticate_as(tests.seed_id('20', 'a', 1), 'aal1');
select throws_ok(
  format($$select public.export_person_overview(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)),
  '42501',
  'Two-step verification is required',
  'an administrator who has not entered the code cannot export'
);
select throws_ok(
  format($$select * from public.export_person_messages(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)),
  '42501',
  'Two-step verification is required',
  'nor read the messages'
);

select * from finish();
rollback;
