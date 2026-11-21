-- Privacy notice: an organization links its privacy notice and a version written for children (NPC Advisory
-- 2024-03). Members see the links and acknowledge the notice once, and again when an organization admin changes it.
-- Only the links are stored here, never the notice itself. api_level is 9.

alter table public.organizations
  add column privacy_notice_url text
    check (privacy_notice_url ~ '^https?://[^[:space:]]+$' and char_length(privacy_notice_url) <= 500),
  add column privacy_notice_child_url text
    check (privacy_notice_child_url ~ '^https?://[^[:space:]]+$' and char_length(privacy_notice_child_url) <= 500),
  add column privacy_notice_version integer not null default 0;

create table public.privacy_notice_acknowledgments (
  org_id uuid not null,
  user_id uuid not null,
  version integer not null,
  acknowledged_at timestamptz not null default now(),
  primary key (org_id, user_id),
  foreign key (org_id, user_id) references public.org_members (org_id, user_id) on delete cascade
);

alter table public.privacy_notice_acknowledgments enable row level security;
revoke all on public.privacy_notice_acknowledgments from anon, authenticated;
grant select on public.privacy_notice_acknowledgments to authenticated;

create policy "People can read their own acknowledgments"
  on public.privacy_notice_acknowledgments for select to authenticated
  using (user_id = (select auth.uid()));

create policy "Require two-step verification" on public.privacy_notice_acknowledgments
  as restrictive for all to authenticated
  using ((select private.mfa_satisfied()));

create function public.list_privacy_notices()
returns table (
  org_id uuid,
  org_name text,
  notice_url text,
  child_notice_url text,
  version integer,
  is_acknowledged boolean
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  return query
  select
    organization.id,
    organization.name,
    organization.privacy_notice_url,
    organization.privacy_notice_child_url,
    organization.privacy_notice_version,
    (
      (organization.privacy_notice_url is null and organization.privacy_notice_child_url is null)
      or coalesce(acknowledgment.version, 0) >= organization.privacy_notice_version
    )
  from public.org_members as member
  join public.organizations as organization on organization.id = member.org_id
  left join public.privacy_notice_acknowledgments as acknowledgment
    on acknowledgment.org_id = member.org_id and acknowledgment.user_id = member.user_id
  where member.user_id = (select auth.uid())
  order by organization.name, organization.id;
end;
$$;

create function public.acknowledge_privacy_notice(target_org_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  person uuid := (select auth.uid());
begin
  perform private.require_mfa();
  if person is null or not exists (
    select 1 from public.org_members where org_id = target_org_id and user_id = person
  ) then
    raise exception 'Not a member of this organization' using errcode = '42501';
  end if;

  insert into public.privacy_notice_acknowledgments (org_id, user_id, version)
  select target_org_id, person, organization.privacy_notice_version
  from public.organizations as organization
  where organization.id = target_org_id
  on conflict (org_id, user_id) do update
    set version = excluded.version, acknowledged_at = now();
end;
$$;

create function public.set_privacy_notice(target_org_id uuid, new_notice_url text, new_child_notice_url text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  notice text := nullif(trim(new_notice_url), '');
  child_notice text := nullif(trim(new_child_notice_url), '');
  organization_row public.organizations;
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can set the privacy notice' using errcode = '42501';
  end if;

  select * into organization_row from public.organizations where id = target_org_id for update;
  if organization_row.privacy_notice_url is not distinct from notice
    and organization_row.privacy_notice_child_url is not distinct from child_notice then
    return;
  end if;

  update public.organizations
  set privacy_notice_url = notice,
      privacy_notice_child_url = child_notice,
      privacy_notice_version = privacy_notice_version + 1
  where id = target_org_id;

  perform private.record_audit_event(target_org_id, 'organization.privacy_notice_changed', 'organization', target_org_id);
end;
$$;

revoke execute on function
  public.list_privacy_notices(),
  public.acknowledge_privacy_notice(uuid),
  public.set_privacy_notice(uuid, text, text)
from public, anon;
grant execute on function
  public.list_privacy_notices(),
  public.acknowledge_privacy_notice(uuid),
  public.set_privacy_notice(uuid, text, text)
to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 9);
$$;
