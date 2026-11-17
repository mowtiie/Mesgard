-- Renames the default role from student to member, so the product reads right for any organization, and raises the
-- API level because an app that knows only the old name cannot read the new one. Changes made here have no actor, so
-- the audit log is not filled with role changes; older entries keep the word student.

insert into public.org_roles (role) values ('member');

update public.org_members set role = 'member' where role = 'student';

alter table public.org_members alter column role set default 'member';

delete from public.org_roles where role = 'student';

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 2);
$$;
