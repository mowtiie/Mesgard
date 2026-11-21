begin;
select plan(9);

-- The group tree: five levels at most, no cycles, and a group with groups inside it cannot be deleted.
insert into public.organizations (id, name, slug) values
  ('00000000-0000-4000-8000-00000000a001', 'Tree School', 'tree-school'),
  ('00000000-0000-4000-8000-00000000a002', 'Other School', 'other-school');

insert into public.groups (id, org_id, parent_id, name) values
  ('30000000-0000-4000-8000-0000000a0001', '00000000-0000-4000-8000-00000000a001', null, 'Level 1'),
  ('30000000-0000-4000-8000-0000000a0002', '00000000-0000-4000-8000-00000000a001', '30000000-0000-4000-8000-0000000a0001', 'Level 2'),
  ('30000000-0000-4000-8000-0000000a0003', '00000000-0000-4000-8000-00000000a001', '30000000-0000-4000-8000-0000000a0002', 'Level 3'),
  ('30000000-0000-4000-8000-0000000a0004', '00000000-0000-4000-8000-00000000a001', '30000000-0000-4000-8000-0000000a0003', 'Level 4');

select lives_ok(
  $$insert into public.groups (id, org_id, parent_id, name) values
    ('30000000-0000-4000-8000-0000000a0005', '00000000-0000-4000-8000-00000000a001', '30000000-0000-4000-8000-0000000a0004', 'Level 5')$$,
  'a group can sit five levels deep'
);
select throws_ok(
  $$insert into public.groups (id, org_id, parent_id, name) values
    ('30000000-0000-4000-8000-0000000a0006', '00000000-0000-4000-8000-00000000a001', '30000000-0000-4000-8000-0000000a0005', 'Level 6')$$,
  '23514',
  null,
  'a sixth level is refused'
);
select throws_ok(
  $$update public.groups set parent_id = '30000000-0000-4000-8000-0000000a0003'
    where id = '30000000-0000-4000-8000-0000000a0001'$$,
  '23514',
  null,
  'a group cannot be moved inside a group it contains'
);

insert into public.groups (id, org_id, parent_id, name) values
  ('30000000-0000-4000-8000-0000000a0010', '00000000-0000-4000-8000-00000000a001', null, 'Another top'),
  ('30000000-0000-4000-8000-0000000a0011', '00000000-0000-4000-8000-00000000a001', '30000000-0000-4000-8000-0000000a0010', 'Inside another top'),
  ('30000000-0000-4000-8000-0000000a0012', '00000000-0000-4000-8000-00000000a001', null, 'Alone');

select throws_ok(
  $$update public.groups set parent_id = '30000000-0000-4000-8000-0000000a0004'
    where id = '30000000-0000-4000-8000-0000000a0010'$$,
  '23514',
  null,
  'moving a group counts the groups inside it, so the tree stays within five levels'
);
select lives_ok(
  $$update public.groups set parent_id = '30000000-0000-4000-8000-0000000a0004'
    where id = '30000000-0000-4000-8000-0000000a0012'$$,
  'a group with nothing inside it can be moved to the last level'
);
select throws_ok(
  $$delete from public.groups where id = '30000000-0000-4000-8000-0000000a0001'$$,
  '23503',
  null,
  'a group with groups inside it cannot be deleted'
);
select lives_ok(
  $$delete from public.groups where id = '30000000-0000-4000-8000-0000000a0012'$$,
  'a group with nothing inside it can be deleted'
);
select throws_ok(
  $$insert into public.groups (id, org_id, parent_id, name) values
    ('30000000-0000-4000-8000-0000000a0020', '00000000-0000-4000-8000-00000000a002', '30000000-0000-4000-8000-0000000a0001', 'Wrong organization')$$,
  '23503',
  null,
  'the parent must belong to the same organization'
);
select is(
  (select count(*)::int from public.groups where org_id = '00000000-0000-4000-8000-00000000a001'),
  7,
  'nothing was deleted along the way'
);

select * from finish();
rollback;
