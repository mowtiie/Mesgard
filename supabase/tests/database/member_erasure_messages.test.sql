begin;
select plan(9);

-- Rosa left every organization. She wrote a message with an attachment, a message that was deleted earlier (so its
-- text sits in the archive for administrators), and uploaded a file that never got attached to a message.
reset role;
insert into auth.users (instance_id, id, aud, role, email, raw_user_meta_data, created_at, updated_at)
values (
  '00000000-0000-0000-0000-000000000000', '88888888-8888-4888-8888-888888888888', 'authenticated', 'authenticated',
  'rosa.cruz@school.example', '{"display_name": "Rosa Cruz"}', now(), now()
);
update public.profiles set display_name = 'Rosa Cruz' where id = '88888888-8888-4888-8888-888888888888';

insert into public.messages (id, org_id, conversation_id, sender_id, body, has_attachments) values
  (tests.seed_id('50', 'a', 91), tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1),
   '88888888-8888-4888-8888-888888888888', 'Rosa Cruz says hello', true),
  (tests.seed_id('50', 'a', 92), tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1),
   '88888888-8888-4888-8888-888888888888', 'old secret', false),
  (tests.seed_id('50', 'a', 93), tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1),
   tests.seed_id('20', 'a', 2), 'Hello from staff', false);
update public.messages set deleted_at = now() where id = tests.seed_id('50', 'a', 92);

insert into storage.objects (bucket_id, name, owner_id, metadata) values
  ('message-attachments', tests.seed_id('40', 'a', 1) || '/' || tests.seed_id('50', 'a', 91) || '/report',
   '88888888-8888-4888-8888-888888888888', '{"size": 10, "mimetype": "application/pdf"}'),
  ('message-attachments', tests.seed_id('40', 'a', 1) || '/' || tests.seed_id('50', 'a', 91) || '/never-attached',
   '88888888-8888-4888-8888-888888888888', '{"size": 10, "mimetype": "application/pdf"}');
insert into public.attachments (id, org_id, message_id, storage_path, mime_type, size_bytes, file_name)
values (
  tests.seed_id('60', 'a', 91), tests.seed_id('10', 'a', 0), tests.seed_id('50', 'a', 91),
  tests.seed_id('40', 'a', 1) || '/' || tests.seed_id('50', 'a', 91) || '/report', 'application/pdf', 10, 'report.pdf'
);

select is(
  (select count(*)::int from public.list_user_attachment_paths('88888888-8888-4888-8888-888888888888')),
  2,
  'the files of a person are found, also one that was never attached to a message'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(
  has_function_privilege('authenticated', 'public.list_user_attachment_paths(uuid)', 'execute'),
  false,
  'a signed in user cannot list the files of someone else'
);
reset role;

-- admin-users replaces the email through the Auth API before it calls the function.
update auth.users set email = 'erased-88888888-8888-4888-8888-888888888888@erased.invalid'
where id = '88888888-8888-4888-8888-888888888888';

-- keep_messages is the default: the person is scrubbed and the messages stay as they are.
select public.erase_user_data('88888888-8888-4888-8888-888888888888', 'rosa.cruz@school.example');
select is(
  (select body from public.messages where id = tests.seed_id('50', 'a', 91)),
  'Rosa Cruz says hello',
  'keep_messages leaves the messages of an erased person as they are'
);

update private.erasure_settings set mode = 'blank_messages';
select public.erase_user_data('88888888-8888-4888-8888-888888888888', 'rosa.cruz@school.example');

select is(
  (select body || '/' || (deleted_at is not null)::text from public.messages where id = tests.seed_id('50', 'a', 91)),
  '/true',
  'blank_messages blanks the message and marks it deleted'
);
select is(
  (select count(*)::int from public.deleted_message_bodies
   where message_id in (tests.seed_id('50', 'a', 91), tests.seed_id('50', 'a', 92))),
  0,
  'the original text kept for administrators is purged, also for a message deleted earlier'
);
select is(
  (select count(*)::int from public.attachments where message_id = tests.seed_id('50', 'a', 91)),
  0,
  'the attachment rows are gone'
);
select is(
  (select count(*)::int from public.messages where sender_id = '88888888-8888-4888-8888-888888888888'),
  2,
  'the messages stay as deleted messages, so conversations keep their shape'
);
select is(
  (select body from public.messages where id = tests.seed_id('50', 'a', 93)),
  'Hello from staff',
  'the messages of other people are not touched'
);

create temporary table leftovers (location text, matches integer);
do $$
declare
  scanned record;
  found_rows integer;
begin
  for scanned in
    select table_schema as schema_name, table_name
    from information_schema.tables
    where table_schema in ('auth', 'public') and table_type = 'BASE TABLE'
  loop
    execute format(
      'select count(*) from %I.%I as row where row::text ilike %L or row::text ilike %L or row::text ilike %L',
      scanned.schema_name, scanned.table_name, '%rosa.cruz@%', '%Rosa Cruz%', '%old secret%'
    ) into found_rows;
    if found_rows > 0 then
      insert into leftovers values (scanned.schema_name || '.' || scanned.table_name, found_rows);
    end if;
  end loop;
end;
$$;
select is(
  (select coalesce(string_agg(location, ', '), '') from leftovers),
  '',
  'neither the name, the email nor the message text is left in the auth or public tables'
);

update private.erasure_settings set mode = 'keep_messages';
select * from finish();
rollback;
