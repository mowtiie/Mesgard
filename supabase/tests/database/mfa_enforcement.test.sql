begin;
select plan(17);

create function pg_temp.visible_members() returns int
language sql
as $$ select count(*)::int from public.org_members $$;

grant execute on function pg_temp.visible_members() to authenticated;

create function pg_temp.rename_own_profile() returns int
language plpgsql
as $$
declare
  changed_rows int;
begin
  update public.profiles set display_name = 'Changed' where id = auth.uid();
  get diagnostics changed_rows = row_count;
  return changed_rows;
end;
$$;

grant execute on function pg_temp.rename_own_profile() to authenticated;

select tests.authenticate_as(tests.seed_id('20', 'a', 2), 'aal1');
select is(pg_temp.visible_members(), 6, 'staff see the roster at aal1 while enforcement is off');
select is(public.is_mfa_required(), false, 'staff are not required to verify while enforcement is off');

reset role;
update private.mfa_settings set enforced = true;

select tests.authenticate_as(tests.seed_id('20', 'a', 2), 'aal1');
select is(public.is_mfa_required(), true, 'staff are required to verify once enforcement is on');
select is(pg_temp.visible_members(), 0, 'staff see nothing at aal1 once enforcement is on');
select is(pg_temp.rename_own_profile(), 0, 'staff cannot write at aal1 once enforcement is on');

select tests.authenticate_as(tests.seed_id('20', 'a', 2), 'aal2');
select is(pg_temp.visible_members(), 6, 'staff see the roster at aal2');

select tests.authenticate_as(tests.seed_id('20', 'a', 1), 'aal1');
select is(pg_temp.visible_members(), 0, 'administrators see nothing at aal1 once enforcement is on');

select tests.authenticate_as(tests.seed_id('20', 'a', 4), 'aal1');
select is(public.is_mfa_required(), false, 'students are not required to verify');
select is(pg_temp.visible_members(), 6, 'students see the roster at aal1 without a factor');

reset role;
insert into auth.mfa_factors (id, user_id, factor_type, status, created_at, updated_at)
values (gen_random_uuid(), tests.seed_id('20', 'a', 4), 'totp', 'unverified', now(), now());

select tests.authenticate_as(tests.seed_id('20', 'a', 4), 'aal1');
select is(pg_temp.visible_members(), 6, 'an unverified factor does not hold a student to aal2');

reset role;
update auth.mfa_factors set status = 'verified' where user_id = tests.seed_id('20', 'a', 4);

select tests.authenticate_as(tests.seed_id('20', 'a', 4), 'aal1');
select is(public.is_mfa_required(), true, 'a student with a verified factor is required to verify');
select is(pg_temp.visible_members(), 0, 'a student with a verified factor sees nothing at aal1');

select tests.authenticate_as(tests.seed_id('20', 'a', 4), 'aal2');
select is(pg_temp.visible_members(), 6, 'a student with a verified factor sees the roster at aal2');

reset role;
update private.mfa_settings set enforced = false;

select tests.authenticate_as(tests.seed_id('20', 'a', 4), 'aal1');
select is(pg_temp.visible_members(), 0, 'a verified factor holds a student even while enforcement is off');

select tests.authenticate_as(tests.seed_id('20', 'a', 2), 'aal1');
select is(public.is_mfa_required(), false, 'turning enforcement off releases staff without a factor');

reset role;
select is(
  (
    select count(*)::int
    from pg_class as relation
    join pg_namespace as namespace on namespace.oid = relation.relnamespace
    where namespace.nspname = 'public'
      and relation.relkind in ('r', 'p')
      and not exists (
        select 1
        from pg_policy as policy
        where policy.polrelid = relation.oid
          and policy.polname = 'Require two-step verification'
          and not policy.polpermissive
      )
  ),
  0,
  'every table in the public schema has the restrictive two-step verification policy'
);

select is(
  (
    select count(*)::int
    from pg_policy as policy
    where policy.polname = 'Require two-step verification'
      and not policy.polpermissive
      and policy.polrelid in ('storage.objects'::regclass, 'realtime.messages'::regclass)
  ),
  2,
  'attachments and Realtime channels have the restrictive two-step verification policy'
);

select * from finish();
rollback;
