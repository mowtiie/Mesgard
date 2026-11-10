-- Groups (classes, sections, departments) and group membership, with Row Level Security.

create table public.group_roles (
  role text primary key
);

insert into public.group_roles (role) values ('moderator'), ('member');

create table public.groups (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations (id) on delete cascade,
  parent_id uuid,
  name text not null check (char_length(name) between 1 and 200),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (org_id, id),
  check (parent_id <> id),
  foreign key (org_id, parent_id) references public.groups (org_id, id) on delete cascade
);

create index groups_org_id_parent_id_idx on public.groups (org_id, parent_id);

create trigger groups_set_updated_at
  before update on public.groups
  for each row execute function private.set_updated_at();

create table public.group_members (
  org_id uuid not null,
  group_id uuid not null,
  user_id uuid not null,
  role_in_group text not null default 'member' references public.group_roles (role),
  created_at timestamptz not null default now(),
  primary key (group_id, user_id),
  foreign key (org_id, group_id) references public.groups (org_id, id) on delete cascade,
  foreign key (org_id, user_id) references public.org_members (org_id, user_id) on delete cascade
);

create index group_members_org_id_user_id_idx on public.group_members (org_id, user_id);

alter table public.group_roles enable row level security;
alter table public.groups enable row level security;
alter table public.group_members enable row level security;

revoke all on public.group_roles, public.groups, public.group_members from anon, authenticated;

grant select on public.group_roles to authenticated;
grant select, delete on public.groups to authenticated;
grant insert (id, org_id, parent_id, name) on public.groups to authenticated;
grant update (name, parent_id) on public.groups to authenticated;
grant select, delete on public.group_members to authenticated;
grant insert (org_id, group_id, user_id, role_in_group) on public.group_members to authenticated;
grant update (role_in_group) on public.group_members to authenticated;

create policy "Authenticated users can read group roles"
  on public.group_roles for select to authenticated
  using (true);

create policy "Members can read groups of their organizations"
  on public.groups for select to authenticated
  using (private.is_org_member(org_id));

create policy "Organization admins can create groups"
  on public.groups for insert to authenticated
  with check (private.has_org_role(org_id, array['org_admin']));

create policy "Organization admins can update groups"
  on public.groups for update to authenticated
  using (private.has_org_role(org_id, array['org_admin']))
  with check (private.has_org_role(org_id, array['org_admin']));

create policy "Organization admins can delete groups"
  on public.groups for delete to authenticated
  using (private.has_org_role(org_id, array['org_admin']));

create policy "Members can read group rosters of their organizations"
  on public.group_members for select to authenticated
  using (private.is_org_member(org_id));

create policy "Organization admins can add group members"
  on public.group_members for insert to authenticated
  with check (private.has_org_role(org_id, array['org_admin']));

create policy "Organization admins can change group member roles"
  on public.group_members for update to authenticated
  using (private.has_org_role(org_id, array['org_admin']))
  with check (private.has_org_role(org_id, array['org_admin']));

create policy "Organization admins can remove group members"
  on public.group_members for delete to authenticated
  using (private.has_org_role(org_id, array['org_admin']));
