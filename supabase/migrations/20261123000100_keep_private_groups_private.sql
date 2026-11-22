-- A group a person created cannot be made to include everyone: that would put the whole organization in a group its
-- creator chose the members of. Administrators still rename or delete such a group and see its members. api_level is 22.

create function private.keep_private_groups_private()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.user_created and new.includes_everyone and not old.includes_everyone then
    raise exception 'A group a person created cannot include everyone' using errcode = 'check_violation';
  end if;
  return new;
end;
$$;

create trigger groups_keep_private_groups_private
  before update of includes_everyone on public.groups
  for each row execute function private.keep_private_groups_private();

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 22);
$$;
