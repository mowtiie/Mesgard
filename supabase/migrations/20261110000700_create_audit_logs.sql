-- Append-only audit log of administrative and moderation actions.

create table public.audit_logs (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations (id),
  actor_id uuid not null default auth.uid() references public.profiles (id),
  action text not null check (char_length(action) between 1 and 100),
  target_type text,
  target_id uuid,
  metadata jsonb not null default '{}',
  created_at timestamptz not null default now()
);

create index audit_logs_org_id_created_at_idx
  on public.audit_logs (org_id, created_at desc, id desc);

create function private.reject_audit_log_changes()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  raise exception 'audit_logs is append-only' using errcode = 'restrict_violation';
end;
$$;

create trigger audit_logs_reject_row_changes
  before update or delete on public.audit_logs
  for each row execute function private.reject_audit_log_changes();

create trigger audit_logs_reject_truncate
  before truncate on public.audit_logs
  for each statement execute function private.reject_audit_log_changes();

alter table public.audit_logs enable row level security;

revoke all on public.audit_logs from anon, authenticated;

grant select on public.audit_logs to authenticated;
grant insert (org_id, actor_id, action, target_type, target_id, metadata)
  on public.audit_logs to authenticated;

create policy "Organization admins can read the audit log of their organization"
  on public.audit_logs for select to authenticated
  using (private.has_org_role(org_id, array['org_admin']));

create policy "Admins and staff can record audit entries as themselves"
  on public.audit_logs for insert to authenticated
  with check (
    actor_id = (select auth.uid())
    and private.has_org_role(org_id, array['org_admin', 'staff'])
  );
