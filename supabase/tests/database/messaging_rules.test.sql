begin;
select plan(28);

-- Seed: organization a has the administrator a01, staff a02 and a03, students a04, a05 and a06. Conversation a01 is
-- direct between staff a02 and student a04, a02 is a group (a02, a04, a05), a03 is the announcements group and a04 is
-- direct between students a05 and a06.

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format($$select public.set_messaging_rules(%L, true, null, null)$$, tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'a student cannot set the messaging rules'
);
select is(
  (select block_staff_student_direct_messages from public.get_messaging_rules(tests.seed_id('10', 'a', 0))),
  false,
  'a member reads the rules of their organization, which start with nothing blocked'
);
select throws_ok(
  format($$select * from public.get_messaging_rules(%L)$$, tests.seed_id('10', 'b', 0)),
  '42501',
  null,
  'and not the rules of another one'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.set_messaging_rules(%L, false, '21:00', null)$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'quiet hours need a start and an end'
);
select throws_ok(
  format($$select public.set_messaging_rules(%L, false, '21:00', '21:00')$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'and a different end'
);
select throws_ok(
  format($$select public.set_messaging_rules(%L, false, '21:00', '06:00', 'Not/AZone')$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'and a time zone that exists'
);

-- Staff and students cannot message each other.
select lives_ok(
  format($$select public.set_messaging_rules(%L, true, null, null)$$, tests.seed_id('10', 'a', 0)),
  'an administrator turns off direct messages between staff and students'
);
select is(
  (select count(*)::int from public.audit_logs where action = 'organization.messaging_rules_changed'),
  1,
  'it is recorded in the audit log'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$select public.send_message(gen_random_uuid(), %L, 'Hello')$$, tests.seed_id('40', 'a', 1)),
  '42501',
  null,
  'staff cannot post in an existing direct conversation with a student'
);
select lives_ok(
  format($$select public.send_message(gen_random_uuid(), %L, 'Hello class')$$, tests.seed_id('40', 'a', 2)),
  'staff still post in a group with students'
);
select is(
  public.create_direct_conversation(tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)),
  tests.seed_id('40', 'a', 1),
  'asking for the existing conversation returns it, read only'
);
select throws_ok(
  format($$select public.create_direct_conversation(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)),
  '42501',
  null,
  'staff cannot start a direct conversation with a student'
);
select lives_ok(
  format($$select public.create_direct_conversation(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 3)),
  'staff can start one with other staff'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format($$select public.send_message(gen_random_uuid(), %L, 'Hello')$$, tests.seed_id('40', 'a', 1)),
  '42501',
  null,
  'a student cannot post to the staff member either'
);
select throws_ok(
  format($$select public.create_direct_conversation(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 3)),
  '42501',
  null,
  'a student cannot start a direct conversation with staff'
);
select is(
  (select count(*)::int from public.messages where conversation_id = tests.seed_id('40', 'a', 1)),
  2,
  'and what was said before can still be read'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select lives_ok(
  format($$select public.send_message(gen_random_uuid(), %L, 'Did you finish?')$$, tests.seed_id('40', 'a', 4)),
  'students still message each other'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.create_direct_conversation(%L, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 6)),
  'an administrator is not held to the rule'
);

-- Quiet hours: now is inside a window that started an hour ago, then inside one that wraps midnight, then outside.
select lives_ok(
  format(
    $$select public.set_messaging_rules(%L, false, ((now() at time zone 'Asia/Manila') - interval '1 hour')::time, ((now() at time zone 'Asia/Manila') + interval '1 hour')::time)$$,
    tests.seed_id('10', 'a', 0)
  ),
  'an administrator sets quiet hours that cover now'
);
select is(
  (select quiet_hours_active from public.get_messaging_rules(tests.seed_id('10', 'a', 0))),
  true,
  'the rules say quiet hours are on'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select throws_ok(
  format($$select public.send_message(gen_random_uuid(), %L, 'Late message')$$, tests.seed_id('40', 'a', 4)),
  '42501',
  null,
  'a student cannot send during quiet hours'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$select public.send_message(gen_random_uuid(), %L, 'Late message')$$, tests.seed_id('40', 'a', 2)),
  '42501',
  null,
  'nor can staff'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.send_message(gen_random_uuid(), %L, 'An urgent notice')$$, tests.seed_id('40', 'a', 3)),
  'an administrator still can'
);
select lives_ok(
  format(
    $$select public.set_messaging_rules(%L, false, ((now() at time zone 'Asia/Manila') - interval '1 hour')::time, ((now() at time zone 'Asia/Manila') - interval '2 hours')::time)$$,
    tests.seed_id('10', 'a', 0)
  ),
  'quiet hours can wrap midnight'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select throws_ok(
  format($$select public.send_message(gen_random_uuid(), %L, 'Late message')$$, tests.seed_id('40', 'a', 4)),
  '42501',
  null,
  'a window that wraps midnight covers now too'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format(
    $$select public.set_messaging_rules(%L, false, ((now() at time zone 'Asia/Manila') + interval '2 hours')::time, ((now() at time zone 'Asia/Manila') + interval '3 hours')::time)$$,
    tests.seed_id('10', 'a', 0)
  ),
  'quiet hours that do not cover now'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select lives_ok(
  format($$select public.send_message(gen_random_uuid(), %L, 'Daytime message')$$, tests.seed_id('40', 'a', 4)),
  'let everyone send'
);

reset role;
update private.mfa_settings set enforced = true;
select tests.authenticate_as(tests.seed_id('20', 'a', 1), 'aal1');
select throws_ok(
  format($$select * from public.get_messaging_rules(%L)$$, tests.seed_id('10', 'a', 0)),
  '42501',
  'Two-step verification is required',
  'an administrator who has not entered the code cannot read the rules'
);

select * from finish();
rollback;
