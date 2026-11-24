begin;
select plan(32);

-- Seed: organization a has the administrator a01 (the reviewer, as nobody is designated), staff a02 and a03, and
-- students a04, a05 and a06. Organization b has the student b04. The default policy is staff and administrators.

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('avatars', %L, %L, '{"size": 2048, "mimetype": "image/jpeg"}')$$,
    tests.avatar_path('a', 4, 1), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a student cannot upload a picture while the policy keeps it to staff'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('avatars', %L, %L, '{"size": 2048, "mimetype": "image/jpeg"}')$$,
    tests.avatar_path('a', 3, 1), tests.seed_id('20', 'a', 2)
  ),
  '42501',
  null,
  'nobody uploads into the folder of someone else'
);
select lives_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('avatars', %L, %L, '{"size": 2048, "mimetype": "image/jpeg"}')$$,
    tests.avatar_path('a', 2, 1), tests.seed_id('20', 'a', 2)
  ),
  'staff upload their own picture'
);
select throws_ok(
  format($$select public.set_profile_picture(%L, 2)$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'a picture that was not uploaded cannot be set'
);
select lives_ok(
  format($$select public.set_profile_picture(%L, 1)$$, tests.seed_id('10', 'a', 0)),
  'staff set the uploaded picture'
);
select throws_ok(
  format($$select public.set_profile_picture(%L, 1)$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'a version that is not newer is refused'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = tests.avatar_path('a', 2, 1)),
  1,
  'a member of the organization reads the current picture'
);
select tests.authenticate_as(tests.seed_id('20', 'b', 4));
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = tests.avatar_path('a', 2, 1)),
  0,
  'a member of another organization cannot read it'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select lives_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('avatars', %L, %L, '{"size": 2048, "mimetype": "image/jpeg"}')$$,
    tests.avatar_path('a', 2, 2), tests.seed_id('20', 'a', 2)
  ),
  'staff upload a newer picture'
);
select lives_ok(
  format($$select public.set_profile_picture(%L, 2)$$, tests.seed_id('10', 'a', 0)),
  'staff replace the picture'
);
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = tests.avatar_path('a', 2, 1)),
  1,
  'the owner still reads an older version of their own picture'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = tests.avatar_path('a', 2, 1)),
  0,
  'others no longer read a picture that is not the current one'
);

-- Policy.
select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select throws_ok(
  format($$select public.set_profile_picture_policy(%L, 'everyone')$$, tests.seed_id('10', 'a', 0)),
  '42501',
  null,
  'staff cannot change the picture policy'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format($$select public.set_profile_picture_policy(%L, 'sometimes')$$, tests.seed_id('10', 'a', 0)),
  '22023',
  null,
  'an unknown policy is refused'
);
select lives_ok(
  format($$select public.set_profile_picture_policy(%L, 'everyone')$$, tests.seed_id('10', 'a', 0)),
  'an administrator lets everyone have a picture'
);
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('avatars', %L, %L, '{"size": 2048, "mimetype": "image/jpeg"}')$$,
    tests.avatar_path('a', 4, 1), tests.seed_id('20', 'a', 4)
  ),
  'a student uploads when the policy allows everyone'
);
select lives_ok(
  format($$select public.set_profile_picture(%L, 1)$$, tests.seed_id('10', 'a', 0)),
  'a student sets a picture when the policy allows everyone'
);

-- Reports.
select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select throws_ok(
  format(
    $$select public.submit_report(%L, %L, 'other', null, %L, null, false, 'profile_picture')$$,
    tests.seed_id('70', 'a', 41), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 3)
  ),
  '22023',
  null,
  'a person without a picture cannot be reported for it'
);
select lives_ok(
  format(
    $$select public.submit_report(%L, %L, 'offensive_content', null, %L, null, false, 'profile_picture')$$,
    tests.seed_id('70', 'a', 42), tests.seed_id('10', 'a', 0), tests.seed_id('20', 'a', 4)
  ),
  'a person reports the profile picture of another person'
);

reset role;
select is(
  (select count(*)::int from public.report_evidence_files
   where report_id = tests.seed_id('70', 'a', 42) and storage_path = tests.avatar_path('a', 4, 1)),
  1,
  'the report keeps the picture as evidence'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
set local storage.allow_delete_query = 'true';
delete from storage.objects where bucket_id = 'avatars' and name = tests.avatar_path('a', 4, 1);
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = tests.avatar_path('a', 4, 1)),
  1,
  'a picture kept as evidence cannot be deleted, even by its owner'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select throws_ok(
  format(
    $$select public.update_report(%L, 'resolved', 'Not appropriate', false, false, true, 10)$$,
    tests.seed_id('70', 'a', 42)
  ),
  '22023',
  null,
  'uploads are blocked for 7 or 30 days only'
);
select throws_ok(
  format($$select public.update_report(%L, 'in_review', null, false, false, true, null)$$, tests.seed_id('70', 'a', 42)),
  '22023',
  null,
  'a picture is removed only when the report is closed'
);
select lives_ok(
  format(
    $$select public.update_report(%L, 'resolved', 'Not appropriate', false, false, true, 7)$$,
    tests.seed_id('70', 'a', 42)
  ),
  'a reviewer removes the picture and blocks new uploads while closing the report'
);
select is(
  (select avatar_version from public.org_members
   where org_id = tests.seed_id('10', 'a', 0) and user_id = tests.seed_id('20', 'a', 4)),
  null,
  'the person has no picture any more'
);
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = tests.avatar_path('a', 4, 1)),
  1,
  'the reviewer still reads the picture kept as evidence'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select is(
  (select count(*)::int from storage.objects where bucket_id = 'avatars' and name = tests.avatar_path('a', 4, 1)),
  0,
  'other members cannot read a removed picture'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('avatars', %L, %L, '{"size": 2048, "mimetype": "image/jpeg"}')$$,
    tests.avatar_path('a', 4, 2), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a person whose uploads are blocked cannot upload'
);
select is(
  (select kind from public.list_profile_notices()),
  'picture_removed',
  'the person gets a neutral notice'
);

reset role;
select is(
  (select count(*)::int from public.audit_logs where action in ('member.picture_removed', 'organization.profile_picture_policy_changed')),
  2,
  'the policy change and the removal are in the audit log'
);

select is(
  (select count(*)::int from public.list_user_avatar_paths(tests.seed_id('20', 'a', 2))),
  2,
  'the pictures of a person are listed so the files can be deleted when they are erased'
);
select is(
  (select count(*)::int from public.list_user_avatar_paths(tests.seed_id('20', 'a', 4))),
  0,
  'a picture kept as evidence is not listed'
);

select * from finish();
rollback;
