-- Who may create groups: an organization chooses admins only, staff, or everyone (new organizations start with everyone,
-- existing ones keep admins only). A person who is not an admin creates flat groups of the kind club or other, at most
-- five, becomes their owner and gets the group conversation at once. Organization admins can see the members and rename or
-- delete such a group, but not read its messages: the archives of deleted and edited messages and the data export skip
-- them. Reports still reach reviewers, that is their purpose. api_level is 19.

alter table public.organizations
  add column group_creation_policy text not null default 'admins_only'
  check (group_creation_policy in ('admins_only', 'staff', 'everyone'));
alter table public.organizations alter column group_creation_policy set default 'everyone';

alter table public.groups
  add column created_by uuid references public.profiles (id) on delete set null,
  add column user_created boolean not null default false;

create function private.max_groups_per_person()
returns integer
language sql
immutable
set search_path = ''
as $$
  select 5;
$$;

create function private.can_create_groups(target_org_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.org_members as member
    join public.organizations as organization on organization.id = member.org_id
    where member.org_id = target_org_id
      and member.user_id = (select auth.uid())
      and (
        member.role = 'org_admin'
        or (organization.group_creation_policy = 'staff' and member.role = 'staff')
        or organization.group_creation_policy = 'everyone'
      )
  );
$$;

create function private.message_in_user_created_group(target_message_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.messages as message
    join public.conversations as conversation on conversation.id = message.conversation_id
    join public.groups as grp on grp.id = conversation.group_id
    where message.id = target_message_id and grp.user_created
  );
$$;

create policy "Members can create groups where the organization allows it"
  on public.groups for insert to authenticated
  with check (private.can_create_groups(org_id));

create function private.prepare_new_group()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
begin
  if caller_id is null then
    return new;
  end if;
  new.created_by := caller_id;
  if private.has_org_role(new.org_id, array['org_admin']) then
    new.user_created := false;
    return new;
  end if;
  new.user_created := true;
  if new.parent_id is not null or new.includes_everyone or new.kind not in ('club', 'other') then
    raise exception 'A group you create is a club or another flat group that does not include everyone'
      using errcode = 'check_violation';
  end if;
  if (
    select count(*) from public.groups
    where org_id = new.org_id and created_by = caller_id and user_created
  ) >= private.max_groups_per_person() then
    raise exception 'You can create at most % groups', private.max_groups_per_person()
      using errcode = 'check_violation';
  end if;
  return new;
end;
$$;

create trigger groups_prepare_new
  before insert on public.groups
  for each row execute function private.prepare_new_group();

create function private.start_user_group()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  new_conversation_id uuid;
begin
  if not new.user_created then
    return new;
  end if;
  insert into public.conversations (org_id, type, group_id, title, created_by)
  values (new.org_id, 'group', new.id, new.name, new.created_by)
  returning id into new_conversation_id;
  insert into public.group_members (org_id, group_id, user_id, role_in_group)
  values (new.org_id, new.id, new.created_by, 'owner');
  insert into public.conversation_participants (org_id, conversation_id, user_id)
  values (new.org_id, new_conversation_id, new.created_by)
  on conflict do nothing;
  return new;
end;
$$;

create trigger groups_start_user_group
  after insert on public.groups
  for each row execute function private.start_user_group();

drop policy "Organization admins can read the original text of deleted messages" on public.deleted_message_bodies;
create policy "Organization admins can read the original text of deleted messages"
  on public.deleted_message_bodies for select to authenticated
  using (private.has_org_role(org_id, array['org_admin']) and not private.message_in_user_created_group(message_id));

drop policy "Organization admins can read the earlier versions of messages" on public.message_edits;
create policy "Organization admins can read the earlier versions of messages"
  on public.message_edits for select to authenticated
  using (private.has_org_role(org_id, array['org_admin']) and not private.message_in_user_created_group(message_id));

create or replace function public.export_person_messages(
  target_org_id uuid,
  target_user_id uuid,
  page_size integer default 200,
  before_created_at timestamptz default null,
  before_message_id uuid default null
)
returns table (
  message_id uuid,
  conversation_type text,
  conversation_title text,
  created_at timestamptz,
  edited_at timestamptz,
  deleted_at timestamptz,
  body text,
  deleted_original text,
  earlier_versions jsonb,
  attachments jsonb
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can export data' using errcode = '42501';
  end if;
  if not private.person_is_known_in_org(target_org_id, target_user_id) then
    raise exception 'Person not found in this organization' using errcode = 'P0002';
  end if;
  return query
  select
    message.id,
    conversation.type,
    conversation.title,
    message.created_at,
    message.edited_at,
    message.deleted_at,
    message.body,
    archived.body,
    coalesce((
      select jsonb_agg(jsonb_build_object('text', edit.body, 'edited_at', edit.edited_at) order by edit.edited_at)
      from public.message_edits as edit where edit.message_id = message.id
    ), '[]'::jsonb),
    coalesce((
      select jsonb_agg(jsonb_build_object(
        'file_name', attachment.file_name, 'mime_type', attachment.mime_type, 'size_bytes', attachment.size_bytes
      ) order by attachment.file_name)
      from public.attachments as attachment where attachment.message_id = message.id
    ), '[]'::jsonb)
  from public.messages as message
  join public.conversations as conversation on conversation.id = message.conversation_id
  left join public.deleted_message_bodies as archived on archived.message_id = message.id
  where message.org_id = target_org_id
    and message.sender_id = target_user_id
    and not private.message_in_user_created_group(message.id)
    and (before_created_at is null or (message.created_at, message.id) < (before_created_at, before_message_id))
  order by message.created_at desc, message.id desc
  limit least(greatest(page_size, 1), 500);
end;
$$;

create function public.get_group_creation(target_org_id uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  chosen_policy text;
begin
  perform private.require_mfa();
  if caller_id is null or not exists (
    select 1 from public.org_members where org_id = target_org_id and user_id = caller_id
  ) then
    raise exception 'Only members of the organization can ask' using errcode = 'insufficient_privilege';
  end if;
  select group_creation_policy into chosen_policy from public.organizations where id = target_org_id;
  return jsonb_build_object(
    'policy', chosen_policy,
    'can_create', private.can_create_groups(target_org_id),
    'created', (
      select count(*) from public.groups
      where org_id = target_org_id and created_by = caller_id and user_created
    ),
    'limit', private.max_groups_per_person()
  );
end;
$$;

create function public.set_group_creation_policy(target_org_id uuid, new_policy text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.require_mfa();
  if not private.has_org_role(target_org_id, array['org_admin']) then
    raise exception 'Only an organization admin can change who creates groups' using errcode = 'insufficient_privilege';
  end if;
  if new_policy not in ('admins_only', 'staff', 'everyone') then
    raise exception 'Unknown policy' using errcode = 'invalid_parameter_value';
  end if;
  update public.organizations set group_creation_policy = new_policy where id = target_org_id;
  perform private.record_audit_event(
    target_org_id, 'organization.group_creation_changed', 'organization', target_org_id,
    jsonb_build_object('policy', new_policy)
  );
end;
$$;

revoke execute on function public.get_group_creation(uuid), public.set_group_creation_policy(uuid, text)
  from public, anon;
grant execute on function public.get_group_creation(uuid), public.set_group_creation_policy(uuid, text)
  to authenticated;

create or replace function public.server_info()
returns jsonb
language sql
immutable
set search_path = ''
as $$
  select jsonb_build_object('api_level', 19);
$$;
