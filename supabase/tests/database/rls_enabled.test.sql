begin;
select plan(1);

select is(
  (
    select count(*)::int
    from pg_class as relation
    join pg_namespace as namespace on namespace.oid = relation.relnamespace
    where namespace.nspname = 'public'
      and relation.relkind = 'r'
      and not relation.relrowsecurity
  ),
  0,
  'every table in the public schema has Row Level Security enabled'
);

select * from finish();
rollback;
