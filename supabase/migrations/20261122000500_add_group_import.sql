-- Import sections and assignments in bulk: each row names a group, optionally the group it is inside and its kind, and
-- optionally a person (by email) and their role in it. Groups are found by name inside their parent or created, so a file
-- can be run again. One bad row does not stop the others, and each row gets its own answer. Only organization admins can
-- import, at most 2000 rows. api_level is 20.

create function public.import_groups(target_org_id uuid, import_rows jsonb)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  row_item jsonb;
  results jsonb := '[]'::jsonb;
  created_groups integer := 0;
  added_members integer := 0;
  group_name text;
  parent_name text;
  group_kind text;
  member_email text;
  member_role text;
  parent_matches integer;
  parent_group_id uuid;
  target_group_id uuid;
  group_status text;
  member_status text;
  member_user_id uuid;
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can import groups' using errcode = 'insufficient_privilege';
  end if;
  if jsonb_typeof(import_rows) <> 'array' or jsonb_array_length(import_rows) > 2000 then
    raise exception 'Import at most 2000 rows' using errcode = 'invalid_parameter_value';
  end if;

  for row_item in select value from jsonb_array_elements(import_rows) loop
    group_status := null;
    member_status := null;
    begin
      group_name := btrim(coalesce(row_item ->> 'group', ''));
      parent_name := nullif(btrim(coalesce(row_item ->> 'parent', '')), '');
      group_kind := lower(coalesce(nullif(btrim(coalesce(row_item ->> 'kind', '')), ''), 'other'));
      member_email := nullif(lower(btrim(coalesce(row_item ->> 'email', ''))), '');
      member_role := lower(coalesce(nullif(btrim(coalesce(row_item ->> 'role', '')), ''), 'member'));
      if group_name = '' then
        raise exception 'The group needs a name';
      end if;
      if member_role not in ('owner', 'moderator', 'member') then
        raise exception 'The role must be owner, moderator or member';
      end if;

      parent_group_id := null;
      if parent_name is not null then
        select count(*), min(id::text)::uuid into parent_matches, parent_group_id
        from public.groups where org_id = target_org_id and lower(name) = lower(parent_name);
        if parent_matches = 0 then
          raise exception 'The group % does not exist, put its row first', parent_name;
        elsif parent_matches > 1 then
          raise exception 'More than one group is called %', parent_name;
        end if;
      end if;

      select id into target_group_id
      from public.groups
      where org_id = target_org_id
        and lower(name) = lower(group_name)
        and parent_id is not distinct from parent_group_id;
      if target_group_id is null then
        insert into public.groups (org_id, parent_id, name, kind)
        values (target_org_id, parent_group_id, group_name, group_kind)
        returning id into target_group_id;
        group_status := 'created';
      else
        group_status := 'exists';
      end if;

      if member_email is not null then
        select id into member_user_id from auth.users where lower(email) = member_email;
        if member_user_id is null or not exists (
          select 1 from public.org_members where org_id = target_org_id and user_id = member_user_id
        ) then
          member_status := 'not_found';
        else
          insert into public.group_members (org_id, group_id, user_id, role_in_group)
          values (target_org_id, target_group_id, member_user_id, member_role)
          on conflict (group_id, user_id) do nothing;
          if found then
            member_status := 'added';
          else
            member_status := 'already';
          end if;
        end if;
      end if;
      results := results || jsonb_build_array(jsonb_build_object(
        'line', row_item -> 'line', 'group', group_status, 'member', member_status, 'error', null
      ));
    exception when others then
      results := results || jsonb_build_array(jsonb_build_object(
        'line', row_item -> 'line', 'group', null, 'member', null, 'error', sqlerrm
      ));
    end;
  end loop;

  -- A row that failed is rolled back, so only the rows without an error count.
  select count(*) filter (where result ->> 'group' = 'created' and result ->> 'error' is null),
         count(*) filter (where result ->> 'member' = 'added' and result ->> 'error' is null)
  into created_groups, added_members
  from jsonb_array_elements(results) as result;
  perform private.record_audit_event(
    target_org_id, 'group.imported', 'organization', target_org_id,
    jsonb_build_object('groups', created_groups, 'members', added_members)
  );
  return jsonb_build_object('created_groups', created_groups, 'added_members', added_members, 'rows', results);
end;
$$;

revoke execute on function public.import_groups(uuid, jsonb) from public, anon;
grant execute on function public.import_groups(uuid, jsonb) to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 20);
$$;
