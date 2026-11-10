-- Local development seed data: two organizations with identical structure for isolation testing.
-- Every user signs in with the password "password123". Identifiers encode their type and organization:
-- 10 organization, 20 user, 30 group, 40 conversation, 50 message, 60 attachment; the suffix letter is the organization.

create temporary table seed_organizations as
select * from (values
  ('a', 'Rizal National High School', 'rnhs', 'rnhs.test'),
  ('b', 'Bonifacio State University', 'bsu', 'bsu.test')
) as organization (letter, name, slug, email_domain);

create temporary table seed_roster as
select * from (values
  (1, 'org_admin', 'admin', 'Admin'),
  (2, 'staff', 'staff1', 'Staff One'),
  (3, 'staff', 'staff2', 'Staff Two'),
  (4, 'student', 'student1', 'Student One'),
  (5, 'student', 'student2', 'Student Two'),
  (6, 'student', 'student3', 'Student Three')
) as member (number, role, local_part, display_name);

create temporary table seed_ids as
select
  organization.letter,
  organization.name,
  organization.slug,
  organization.email_domain,
  ('10000000-0000-4000-8000-000000000' || organization.letter || '00')::uuid as org_id
from seed_organizations as organization;

insert into public.organizations (id, name, slug)
select org_id, name, slug from seed_ids;

insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
  raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
  confirmation_token, recovery_token, email_change_token_new, email_change
)
select
  '00000000-0000-0000-0000-000000000000',
  ('20000000-0000-4000-8000-000000000' || organization.letter || lpad(member.number::text, 2, '0'))::uuid,
  'authenticated',
  'authenticated',
  member.local_part || '@' || organization.email_domain,
  extensions.crypt('password123', extensions.gen_salt('bf')),
  now(),
  '{"provider": "email", "providers": ["email"]}',
  jsonb_build_object('display_name', member.display_name || ' ' || upper(organization.slug)),
  now(),
  now(),
  '', '', '', ''
from seed_ids as organization
cross join seed_roster as member;

insert into auth.identities (id, user_id, provider_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
select
  gen_random_uuid(),
  users.id,
  users.id::text,
  jsonb_build_object('sub', users.id::text, 'email', users.email),
  'email',
  now(),
  now(),
  now()
from auth.users as users;

insert into public.org_members (org_id, user_id, role)
select
  organization.org_id,
  ('20000000-0000-4000-8000-000000000' || organization.letter || lpad(member.number::text, 2, '0'))::uuid,
  member.role
from seed_ids as organization
cross join seed_roster as member;

create temporary table seed_groups as
select * from (values
  (1, null::int, 'Grade 10'),
  (2, 1, 'Grade 10 Section A'),
  (3, null::int, 'Science Department')
) as grp (number, parent_number, name);

insert into public.groups (id, org_id, parent_id, name)
select
  ('30000000-0000-4000-8000-000000000' || organization.letter || lpad(grp.number::text, 2, '0'))::uuid,
  organization.org_id,
  case when grp.parent_number is null then null
       else ('30000000-0000-4000-8000-000000000' || organization.letter || lpad(grp.parent_number::text, 2, '0'))::uuid end,
  grp.name
from seed_ids as organization
cross join seed_groups as grp
order by grp.parent_number nulls first;

insert into public.group_members (org_id, group_id, user_id, role_in_group)
select
  organization.org_id,
  ('30000000-0000-4000-8000-000000000' || organization.letter || lpad(membership.group_number::text, 2, '0'))::uuid,
  ('20000000-0000-4000-8000-000000000' || organization.letter || lpad(membership.user_number::text, 2, '0'))::uuid,
  membership.role_in_group
from seed_ids as organization
cross join (values
  (2, 2, 'moderator'),
  (2, 4, 'member'),
  (2, 5, 'member'),
  (3, 2, 'moderator'),
  (3, 3, 'member')
) as membership (group_number, user_number, role_in_group);

create temporary table seed_conversations as
select * from (values
  (1, 'direct', null::int, 'Student One and Staff One', 4),
  (2, 'group', 2, 'Grade 10 Section A', 2),
  (3, 'announcement', null::int, 'School Announcements', 1),
  (4, 'direct', null::int, 'Student Two and Student Three', 5)
) as conversation (number, type, group_number, title, creator_number);

insert into public.conversations (id, org_id, type, group_id, title, created_by)
select
  ('40000000-0000-4000-8000-000000000' || organization.letter || lpad(conversation.number::text, 2, '0'))::uuid,
  organization.org_id,
  conversation.type,
  case when conversation.group_number is null then null
       else ('30000000-0000-4000-8000-000000000' || organization.letter || lpad(conversation.group_number::text, 2, '0'))::uuid end,
  conversation.title,
  ('20000000-0000-4000-8000-000000000' || organization.letter || lpad(conversation.creator_number::text, 2, '0'))::uuid
from seed_ids as organization
cross join seed_conversations as conversation;

insert into public.conversation_participants (org_id, conversation_id, user_id)
select
  organization.org_id,
  ('40000000-0000-4000-8000-000000000' || organization.letter || lpad(participant.conversation_number::text, 2, '0'))::uuid,
  ('20000000-0000-4000-8000-000000000' || organization.letter || lpad(participant.user_number::text, 2, '0'))::uuid
from seed_ids as organization
cross join (values
  (1, 4), (1, 2),
  (2, 2), (2, 4), (2, 5),
  (3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 6),
  (4, 5), (4, 6)
) as participant (conversation_number, user_number);

insert into public.messages (id, org_id, conversation_id, sender_id, body, created_at)
select
  ('50000000-0000-4000-8000-000000000' || organization.letter || lpad(message.number::text, 2, '0'))::uuid,
  organization.org_id,
  ('40000000-0000-4000-8000-000000000' || organization.letter || lpad(message.conversation_number::text, 2, '0'))::uuid,
  ('20000000-0000-4000-8000-000000000' || organization.letter || lpad(message.sender_number::text, 2, '0'))::uuid,
  message.body,
  now() - make_interval(mins => message.minutes_ago)
from seed_ids as organization
cross join (values
  (1, 1, 4, 'Good morning, may I ask about the assignment?', 50),
  (2, 1, 2, 'Of course. It is due on Friday.', 45),
  (3, 2, 2, 'Reminder: bring your project proposals tomorrow.', 40),
  (4, 2, 4, 'Noted, thank you.', 35),
  (5, 3, 1, 'Classes are suspended on Monday due to a holiday.', 30),
  (6, 3, 2, 'Please confirm your attendance for the faculty meeting.', 25),
  (7, 4, 5, 'Did you finish the reading?', 20),
  (8, 4, 6, 'Almost done.', 15)
) as message (number, conversation_number, sender_number, body, minutes_ago);

insert into public.attachments (id, org_id, message_id, storage_path, mime_type, size_bytes)
select
  ('60000000-0000-4000-8000-000000000' || organization.letter || '01')::uuid,
  organization.org_id,
  ('50000000-0000-4000-8000-000000000' || organization.letter || '03')::uuid,
  organization.slug || '/proposals/template.pdf',
  'application/pdf',
  102400
from seed_ids as organization;

insert into public.message_receipts (org_id, message_id, user_id, read_at)
select
  organization.org_id,
  ('50000000-0000-4000-8000-000000000' || organization.letter || '01')::uuid,
  ('20000000-0000-4000-8000-000000000' || organization.letter || '02')::uuid,
  now()
from seed_ids as organization;

insert into public.device_tokens (user_id, fcm_token)
select
  ('20000000-0000-4000-8000-000000000' || organization.letter || '04')::uuid,
  'seed-fcm-token-' || organization.slug
from seed_ids as organization;

insert into public.audit_logs (org_id, actor_id, action, target_type, target_id)
select
  organization.org_id,
  ('20000000-0000-4000-8000-000000000' || organization.letter || '01')::uuid,
  'group.create',
  'group',
  ('30000000-0000-4000-8000-000000000' || organization.letter || '01')::uuid
from seed_ids as organization;

drop table seed_conversations, seed_groups, seed_ids, seed_roster, seed_organizations;
