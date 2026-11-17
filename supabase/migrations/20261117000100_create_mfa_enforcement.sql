-- Two-step verification: accounts that need it are held to an aal2 session by restrictive policies on every table, the
-- attachment bucket and the Realtime channels. Enforcement for administrators and staff is off until it is switched on
-- with: update private.mfa_settings set enforced = true; Anyone with a verified factor is always held to it.

create table private.mfa_settings (
  singleton boolean primary key default true check (singleton),
  enforced boolean not null default false
);

insert into private.mfa_settings default values;

create function private.mfa_required_for(target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select
    exists (
      select 1
      from auth.mfa_factors
      where user_id = target_user_id
        and status = 'verified'
    )
    or (
      (select enforced from private.mfa_settings)
      and exists (
        select 1
        from public.org_members
        where user_id = target_user_id
          and role in ('org_admin', 'staff')
      )
    );
$$;

create function private.mfa_satisfied()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select
    coalesce((select auth.jwt() ->> 'aal') = 'aal2', false)
    or not private.mfa_required_for((select auth.uid()));
$$;

revoke execute on function private.mfa_required_for(uuid) from public, anon, authenticated;
revoke execute on function private.mfa_satisfied() from public, anon;
grant execute on function private.mfa_satisfied() to authenticated;

-- Callable at aal1, so the app can tell whether the account must verify or enroll before any data is readable.
create function public.is_mfa_required()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select private.mfa_required_for((select auth.uid()));
$$;

revoke execute on function public.is_mfa_required() from public, anon;
grant execute on function public.is_mfa_required() to authenticated;

do $$
declare
  protected_table record;
begin
  for protected_table in
    select schemaname, tablename
    from pg_tables
    where (schemaname = 'public')
       or (schemaname = 'storage' and tablename = 'objects')
       or (schemaname = 'realtime' and tablename = 'messages')
  loop
    execute format(
      'create policy "Require two-step verification" on %I.%I as restrictive for all to authenticated using ((select private.mfa_satisfied()))',
      protected_table.schemaname,
      protected_table.tablename
    );
  end loop;
end;
$$;
