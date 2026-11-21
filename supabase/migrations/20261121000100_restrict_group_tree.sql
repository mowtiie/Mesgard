-- Group tree: a group cannot be deleted while it has groups inside it (the parent link used to cascade, so deleting a
-- section silently deleted every group below it and their messages), a group cannot be its own ancestor, and the tree is
-- at most five levels deep. The same checks apply when a group is moved. api_level is 15.

alter table public.groups drop constraint groups_org_id_parent_id_fkey;
alter table public.groups
  add constraint groups_org_id_parent_id_fkey
  foreign key (org_id, parent_id) references public.groups (org_id, id) on delete restrict;

create function private.max_group_depth()
returns integer
language sql
immutable
set search_path = ''
as $$
  select 5;
$$;

create function private.check_group_tree()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  parent_level integer;
  subtree_height integer;
begin
  if new.parent_id is null then
    return new;
  end if;
  if tg_op = 'UPDATE' and new.parent_id is not distinct from old.parent_id then
    return new;
  end if;

  with recursive ancestors as (
    select id, parent_id from public.groups where id = new.parent_id
    union all
    select g.id, g.parent_id from public.groups g join ancestors a on g.id = a.parent_id
  )
  select count(*) into parent_level from ancestors;
  if exists (
    with recursive ancestors as (
      select id, parent_id from public.groups where id = new.parent_id
      union all
      select g.id, g.parent_id from public.groups g join ancestors a on g.id = a.parent_id
    )
    select 1 from ancestors where id = new.id
  ) then
    raise exception 'A group cannot be placed inside itself or inside a group it contains'
      using errcode = 'check_violation';
  end if;

  with recursive descendants as (
    select id, 1 as level from public.groups where parent_id = new.id
    union all
    select g.id, d.level + 1 from public.groups g join descendants d on g.parent_id = d.id
  )
  select coalesce(max(level), 0) into subtree_height from descendants;

  if parent_level + 1 + subtree_height > private.max_group_depth() then
    raise exception 'Groups can be at most % levels deep', private.max_group_depth()
      using errcode = 'check_violation';
  end if;
  return new;
end;
$$;

create trigger groups_check_tree
  before insert or update of parent_id on public.groups
  for each row execute function private.check_group_tree();

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 15);
$$;
