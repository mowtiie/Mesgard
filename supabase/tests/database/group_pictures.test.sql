begin;
select plan(35);

-- Seed: organization a has the administrator a01 (the reviewer, as nobody is designated), staff a02 and a03, and
-- students a04, a05 and a06. In group 2 the staff member a02 is a moderator and the students a04 and a05 are members.
-- Organization b has the student b04. The default policy is staff and administrators.

create function pg_temp.group_picture_path(version int, organization_letter text default 'a')
returns text
language sql
as $$
  select tests.seed_id('10', organization_letter, 0)::text || '/groups/' || tests.seed_id('30', 'a', 2)::text
    || '/' || version::text;
$$;

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('avatars', %L, %L, '{"size": 2048, "mimetype": "image/jpeg"}')$$,
    pg_temp.group_picture_path(1), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a member who is not a moderator cannot upload a group picture'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('avatars', %L, %L, '{"size": 2048, "mimetype": "image/jpeg"}')$$,
    pg_temp.group_picture_path(1), tests.seed_id('20', 'a', 2)
  ),
  'a moderator uploads a group picture'
);
select throws_ok(
  format($$select public.set_group_picture(%L, 2)$$, tests.seed_id('30', 'a', 2)),
  '22023',
  null,
  'a picture that was not uploaded cannot be set'
);
select lives_ok(
  format($$select public.set_group_picture(%L, 1)$$, tests.seed_id('30', 'a', 2)),
  'a moderator sets the uploaded picture'
);
select throws_ok(
  format($$select public.set_group_picture(%L, 1)$$, tests.seed_id('30', 'a', 2)),
  '22023',
  null,
  'a version that is not newer is refused'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format($$select public.set_group_picture(%L, 2)$$, tests.seed_id('30', 'a', 2)),
  '42501',
  null,
  'a member who is not a moderator cannot set the picture'
);
select throws_ok(
  format($$select public.remove_group_picture(%L)$$, tests.seed_id('30', 'a', 2)),
  '42501',
  null,
  'a member who is not a moderator cannot remove the picture'
);
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = pg_temp.group_picture_path(1)),
  1,
  'a member of the organization reads the current group picture'
);
select is(
  (public.get_group_picture_rules(tests.seed_id('30', 'a', 2)) ->> 'can_set')::boolean,
  false,
  'the rules tell a member that they cannot set the picture'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  format($$select public.create_group_conversation(%L)$$, tests.seed_id('30', 'a', 2)),
  'a moderator opens the conversation of the group'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select group_avatar_version from public.list_conversation_summaries()
   where group_id = tests.seed_id('30', 'a', 2)),
  1,
  'the conversation list tells which picture the group has'
);

select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = pg_temp.group_picture_path(1)),
  0,
  'a member of another organization cannot read it'
);

-- Policy.
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.set_profile_picture_policy(%L, 'off')$$, tests.seed_id('10', 'a', 0)),
  'an administrator turns pictures off'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$select public.set_group_picture(%L, 2)$$, tests.seed_id('30', 'a', 2)),
  '42501',
  null,
  'no group can have a picture while the policy is off'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = pg_temp.group_picture_path(1)),
  0,
  'a group picture is hidden while the policy is off'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select lives_ok(
  format($$select public.set_profile_picture_policy(%L, 'staff_and_admins')$$, tests.seed_id('10', 'a', 0)),
  'an administrator turns pictures back on'
);

-- Reports.
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', null, %L, null, false, 'group_picture', %L)$$,
    tests.seed_id('70', 'a', 51), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 5), tests.seed_id('30', 'a', 2)
  ),
  '22023',
  null,
  'a report about a group names no person'
);
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', null, null, null, false, 'group_picture', %L)$$,
    tests.seed_id('70', 'a', 51), tests.seed_id('10', 'b', 0), tests.seed_id('30', 'a', 2)
  ),
  '42501',
  null,
  'a person cannot report in an organization they do not belong to'
);
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', null, null, null, false, 'group_picture', %L)$$,
    tests.seed_id('70', 'a', 51), tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 3)
  ),
  '22023',
  null,
  'a group without a picture cannot be reported for it'
);
select lives_ok(
  format(
    $$select public.submit_report(%L, %L, 'offensive_content', null, null, null, false, 'group_picture', %L)$$,
    tests.seed_id('70', 'a', 52), tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 2)
  ),
  'a member reports the picture of a group'
);
select lives_ok(
  format(
    $$select public.submit_report(%L, %L, 'offensive_content', null, null, null, false, 'group_name', %L)$$,
    tests.seed_id('70', 'a', 53), tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 2)
  ),
  'a member reports the name of a group'
);

reset role;
select is(
  (select count(*)::int from public.report_evidence_files
   where report_id = tests.seed_id('70', 'a', 52) and storage_path = pg_temp.group_picture_path(1)),
  1,
  'the report keeps the group picture as evidence'
);
select is(
  (select subject_text from public.report_evidence where report_id = tests.seed_id('70', 'a', 53)),
  (select name from public.groups where id = tests.seed_id('30', 'a', 2)),
  'the report keeps a copy of the group name'
);

update public.group_members set role_in_group = 'owner'
where group_id = tests.seed_id('30', 'a', 2) and user_id = tests.seed_id('20', 'a', 5);
select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', null, null, null, false, 'group_name', %L)$$,
    tests.seed_id('70', 'a', 54), tests.seed_id('10', 'a', 0), tests.seed_id('30', 'a', 2)
  ),
  '22023',
  null,
  'the owner of a group cannot report it'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
set local storage.allow_delete_query = 'true';
delete from storage.objects where bucket_id = 'avatars' and name = pg_temp.group_picture_path(1);
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = pg_temp.group_picture_path(1)),
  1,
  'a group picture kept as evidence cannot be deleted'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.update_report(%L, 'resolved', 'Not appropriate', false, false, false, null, 'New name')$$,
    tests.seed_id('70', 'a', 52)),
  '22023',
  null,
  'a group is renamed only through a report about its name'
);
select lives_ok(
  format($$select public.update_report(%L, 'resolved', 'Not appropriate', false, false, true, null)$$,
    tests.seed_id('70', 'a', 52)),
  'a reviewer removes the group picture while closing the report'
);
select is(
  (select avatar_version from public.groups where id = tests.seed_id('30', 'a', 2)),
  null,
  'the group has no picture any more'
);
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = pg_temp.group_picture_path(1)),
  1,
  'the reviewer still reads the picture kept as evidence'
);
select throws_ok(
  format($$select public.update_report(%L, 'resolved', 'Not appropriate', false, false, false, null, '   ')$$,
    tests.seed_id('70', 'a', 53)),
  '22023',
  null,
  'a group cannot be renamed to nothing'
);
select lives_ok(
  format($$select public.update_report(%L, 'resolved', 'Not appropriate', false, false, false, null, 'Mathematics')$$,
    tests.seed_id('70', 'a', 53)),
  'a reviewer renames the group while closing the report'
);
select is(
  (select name from public.groups where id = tests.seed_id('30', 'a', 2)),
  'Mathematics',
  'the group has the new name'
);
select is(
  (select subject_name from public.list_reports(tests.seed_id('10', 'a', 0)) where subject = 'group_name'),
  'Mathematics',
  'the report list names the group'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  (select array_agg(kind order by kind) from public.list_profile_notices()),
  array['group_picture_removed', 'group_renamed'],
  'the owner of the group gets a neutral notice for each change'
);

reset role;
select is(
  (select count(*)::int from public.audit_logs where action in ('group.picture_removed', 'group.renamed')),
  2,
  'both changes are in the audit log'
);

select * from finish();
rollback;
