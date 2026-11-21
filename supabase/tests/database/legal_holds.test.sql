begin;
select plan(38);

-- Seed: organization a has the administrator a01, staff a02, students a04, a05 and a06. Group a02 has a group
-- conversation, group a03 has none. Organization b has the administrator b01 and the student b04.

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format($$select public.place_legal_hold(%L, 'A court order was received', %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)),
  '42501',
  null,
  'a student cannot place a legal hold'
);
select throws_ok(
  format($$select count(*) from public.legal_holds$$),
  '42501',
  null,
  'nobody reads the holds table directly'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$select public.place_legal_hold(%L, 'A court order was received', %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)),
  '42501',
  null,
  'staff cannot place a legal hold'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.place_legal_hold(%L, 'Too short', %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)),
  '22023',
  null,
  'a hold needs a real reason'
);
select throws_ok(
  format(
    $$select public.place_legal_hold(%L, 'A court order was received', %L, %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5), tests.seed_id('30', 'a', 2)
  ),
  '22023',
  null,
  'a hold covers a person or a group, not both'
);
select throws_ok(
  format($$select public.place_legal_hold(%L, 'A court order was received', %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'b', 4)),
  '22023',
  null,
  'a hold cannot name a person of another organization'
);
select throws_ok(
  format(
    $$select public.place_legal_hold(%L, 'A court order was received', null, %L)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('30', 'b', 2)
  ),
  '22023',
  null,
  'a hold cannot name a group of another organization'
);
select throws_ok(
  format($$select public.place_legal_hold(%L, 'A court order was received', %L)$$, tests.seed_id('10', 'b', 0), tests.seed_id('20', 'b', 4)),
  '42501',
  null,
  'an administrator cannot place a hold in another organization'
);

-- A hold on a person.
select lives_ok(
  format($$select public.place_legal_hold(%L, 'A court order was received', %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)),
  'an administrator places a hold on a person'
);
select results_eq(
  format($$select scope, user_name, reason from public.list_legal_holds(%L)$$, tests.seed_id('10', 'a', 0)),
  $$values ('person'::text, 'Student Two RNHS'::text, 'A court order was received'::text)$$,
  'the hold is listed with the person and the reason'
);
select is(
  (select count(*)::int from public.audit_logs where action = 'legal_hold.placed'),
  1,
  'placing a hold is recorded in the audit log'
);

reset role;
select throws_ok(
  format($$select public.erase_user_data(%L, 'student2@rnhs.test')$$, tests.seed_id('20', 'a', 5)),
  '23503',
  null,
  'the person under a hold cannot be erased'
);
select throws_ok(
  format($$select public.list_user_attachment_paths(%L)$$, tests.seed_id('20', 'a', 5)),
  '23503',
  null,
  'and their files are not listed for removal'
);
select is(public.legal_hold_blocks_erasure(tests.seed_id('20', 'a', 5)), true, 'the erase flow can ask before it touches anything');
select is(public.legal_hold_blocks_erasure(tests.seed_id('20', 'a', 6)), false, 'a person with no hold is not blocked');
select throws_ok(
  format($$select public.erase_user_data(%L, 'student3@rnhs.test')$$, tests.seed_id('20', 'a', 6)),
  '42501',
  null,
  'someone else still goes through the other checks'
);

-- A hold on a group.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.place_legal_hold(%L, 'The class chat is part of an inquiry', null, %L)$$, tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 2)),
  'an administrator places a hold on a group'
);
select throws_ok(
  format($$delete from public.groups where id = %L$$, tests.seed_id('30', 'a', 2)),
  '23503',
  null,
  'a group under a hold cannot be deleted'
);
select lives_ok(
  format($$delete from public.groups where id = %L$$, tests.seed_id('30', 'a', 3)),
  'another group can be deleted'
);

-- Releasing.
select is(
  (select count(*)::int from public.list_legal_holds(tests.seed_id('10', 'a', 0))),
  2,
  'the active holds are listed'
);
reset role;
create temporary table hold_ids on commit drop as select id, group_id from public.legal_holds;
grant select on hold_ids to public;
select tests.authenticate_as(tests.seed_id('20', 'b', 1));
select throws_ok(
  format(
    $$select public.release_legal_hold(%L, 'Not mine to release')$$,
    (select id from hold_ids where group_id is not null)
  ),
  '42501',
  null,
  'an administrator of another organization cannot release the hold'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format(
    $$select public.release_legal_hold(hold_id, 'The order was withdrawn') from public.list_legal_holds(%L) where scope = 'group'$$,
    tests.seed_id('10', 'a', 0)
  ),
  'an administrator releases the hold on the group'
);
select lives_ok(
  format($$delete from public.groups where id = %L$$, tests.seed_id('30', 'a', 2)),
  'and the group can be deleted'
);
select throws_ok(
  format(
    $$select public.release_legal_hold(hold_id, 'Releasing again') from public.list_legal_holds(%L, true) where scope = 'group'$$,
    tests.seed_id('10', 'a', 0)
  ),
  '22023',
  null,
  'a hold cannot be released twice'
);
select throws_ok(
  format(
    $$select public.release_legal_hold(hold_id, 'ok') from public.list_legal_holds(%L) where scope = 'person'$$,
    tests.seed_id('10', 'a', 0)
  ),
  '22023',
  null,
  'releasing needs a note'
);
select lives_ok(
  format(
    $$select public.release_legal_hold(hold_id, 'The order was withdrawn') from public.list_legal_holds(%L) where scope = 'person'$$,
    tests.seed_id('10', 'a', 0)
  ),
  'an administrator releases the hold on the person'
);
select is(
  (select count(*)::int from public.list_legal_holds(tests.seed_id('10', 'a', 0))),
  0,
  'no hold is active any more'
);
select is(
  (select count(*)::int from public.list_legal_holds(tests.seed_id('10', 'a', 0), true)),
  2,
  'released holds stay in the history'
);
select is(
  (select count(*)::int from public.audit_logs where action = 'legal_hold.released'),
  2,
  'releasing a hold is recorded in the audit log'
);

reset role;
select throws_ok(
  format($$select public.erase_user_data(%L, 'student2@rnhs.test')$$, tests.seed_id('20', 'a', 5)),
  '42501',
  null,
  'after the release the person is no longer protected by the hold'
);

-- An open report protects the people involved, without anyone placing a hold.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format($$select public.submit_report(%L, %L, 'harassment', null, %L, 'Messages after class')$$, tests.seed_id('70', 'a', 1), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5)),
  'a student reports another student'
);
reset role;
select is(public.legal_hold_blocks_erasure(tests.seed_id('20', 'a', 5)), true, 'an open report protects the person reported');
select is(public.legal_hold_blocks_erasure(tests.seed_id('20', 'a', 4)), true, 'and the reporter');

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.update_report(%L, 'dismissed', 'Not enough information')$$, tests.seed_id('70', 'a', 1)),
  'a reviewer closes the report'
);
reset role;
select is(public.legal_hold_blocks_erasure(tests.seed_id('20', 'a', 5)), false, 'a closed report no longer protects anyone');

-- A hold on the whole organization.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.place_legal_hold(%L, 'An inquiry covers the whole school')$$, tests.seed_id('10', 'a', 0)),
  'an administrator places a hold on the whole organization'
);
reset role;
select is(public.legal_hold_blocks_erasure(tests.seed_id('20', 'a', 6)), true, 'it protects every person of the organization');
select is(public.legal_hold_blocks_erasure(tests.seed_id('20', 'b', 4)), false, 'and nobody of another organization');

select * from finish();
rollback;
