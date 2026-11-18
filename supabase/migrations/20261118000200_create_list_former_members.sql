-- Lets an administrator see the people their organization removed, so an erase that failed half way can be finished.
-- A removed person leaves org_members, so the list comes from the audit log. The app needs it, so api_level is 3.

create index audit_logs_removed_and_erased_idx
  on public.audit_logs (org_id, target_id, created_at desc)
  where action in ('member.removed', 'member.erased');

create function public.list_former_members(
  target_org_id uuid,
  page_size integer default 50,
  removed_before timestamptz default null,
  before_user_id uuid default null
)
returns table (user_id uuid, display_name text, removed_at timestamptz, is_erased boolean, can_erase boolean)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an administrator can list former members' using errcode = '42501';
  end if;

  return query
  with latest_removal as (
    select distinct on (log.target_id) log.target_id as person_id, log.created_at as removal_time
    from public.audit_logs as log
    where log.org_id = target_org_id and log.action = 'member.removed' and log.target_id is not null
    order by log.target_id, log.created_at desc
  ),
  former as (
    select
      removal.person_id,
      removal.removal_time,
      exists (
        select 1 from public.audit_logs as erased
        where erased.org_id = target_org_id and erased.action = 'member.erased' and erased.target_id = removal.person_id
      ) as erased
    from latest_removal as removal
    where not exists (
      select 1 from public.org_members as member
      where member.org_id = target_org_id and member.user_id = removal.person_id
    )
  )
  select
    former.person_id,
    coalesce(profile.display_name, '')::text,
    former.removal_time,
    former.erased,
    (not former.erased and not exists (
      select 1 from public.org_members as elsewhere where elsewhere.user_id = former.person_id
    ))
  from former
  left join public.profiles as profile on profile.id = former.person_id
  where removed_before is null or (former.removal_time, former.person_id) < (removed_before, before_user_id)
  order by former.removal_time desc, former.person_id desc
  limit least(greatest(page_size, 1), 100);
end;
$$;

revoke execute on function public.list_former_members(uuid, integer, timestamptz, uuid) from public, anon;
grant execute on function public.list_former_members(uuid, integer, timestamptz, uuid) to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 3);
$$;
