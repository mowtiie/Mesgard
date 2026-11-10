-- Local-only helpers for pgTAP tests. Never apply to a production database.

create schema if not exists tests;
grant usage on schema tests to anon, authenticated;

create function tests.authenticate_as(user_id uuid)
returns void
language plpgsql
as $$
begin
  execute 'reset role';
  perform set_config('request.jwt.claim.sub', user_id::text, true);
  perform set_config(
    'request.jwt.claims',
    json_build_object('sub', user_id, 'role', 'authenticated')::text,
    true
  );
  execute 'set local role authenticated';
end;
$$;

create function tests.authenticate_as_anonymous()
returns void
language plpgsql
as $$
begin
  execute 'reset role';
  perform set_config('request.jwt.claim.sub', '', true);
  perform set_config('request.jwt.claims', '{"role": "anon"}', true);
  execute 'set local role anon';
end;
$$;

create function tests.seed_id(kind_prefix text, organization_letter text, item_number int)
returns uuid
language sql
immutable
as $$
  select (kind_prefix || '000000-0000-4000-8000-000000000' || organization_letter || lpad(item_number::text, 2, '0'))::uuid;
$$;

grant execute on all functions in schema tests to anon, authenticated;
