-- Security definer functions bypass Row Level Security, so each one callable by a signed in user checks the session itself.

create function private.require_mfa()
returns void
language plpgsql
stable
set search_path = ''
as $$
begin
  if not private.mfa_satisfied() then
    raise exception 'Two-step verification is required' using errcode = 'insufficient_privilege';
  end if;
end;
$$;

revoke execute on function private.require_mfa() from public, anon;
grant execute on function private.require_mfa() to authenticated;

create or replace function public.complete_password_change()
returns void
language sql
security definer
set search_path = ''
as $$
  select private.require_mfa();
  update auth.users
  set raw_app_meta_data = raw_app_meta_data - 'must_change_password'
  where id = (select auth.uid());
$$;

do $$
declare
  guarded_function record;
  definition text;
  guarded_definition text;
begin
  for guarded_function in
    select procedure.oid, procedure.proname
    from pg_proc as procedure
    where procedure.pronamespace = 'public'::regnamespace
      and procedure.prosecdef
      and procedure.prolang = (select oid from pg_language where lanname = 'plpgsql')
      and has_function_privilege('authenticated', procedure.oid, 'execute')
      and procedure.proname <> 'is_mfa_required'
  loop
    definition := pg_get_functiondef(guarded_function.oid);
    guarded_definition := regexp_replace(definition, E'\nbegin\n', E'\nbegin\n  perform private.require_mfa();\n');
    if guarded_definition = definition then
      raise exception 'Could not add the two-step verification check to %', guarded_function.proname;
    end if;
    execute guarded_definition;
  end loop;
end;
$$;
