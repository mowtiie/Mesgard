begin;
select plan(13);

-- A person who left their last organization, with data in every place the erasure has to reach.
reset role;
insert into auth.users (instance_id, id, aud, role, email, raw_user_meta_data, phone, created_at, updated_at)
values (
  '00000000-0000-0000-0000-000000000000', '77777777-7777-4777-8777-777777777777', 'authenticated', 'authenticated',
  'maria.santos@school.example', '{"display_name": "Maria Santos", "note": "Maria Santos private"}', '+639170000000', now(), now()
);
update public.profiles set display_name = 'Maria Santos', avatar_path = 'avatars/maria.png', status = 'busy'
where id = '77777777-7777-4777-8777-777777777777';
insert into public.push_subscriptions (user_id, endpoint, p256dh, auth_secret)
values ('77777777-7777-4777-8777-777777777777', 'https://push.example/maria', 'p256dh-key-0123456789', 'auth-secret-0123456789');
insert into public.user_settings (user_id, notification_preview)
values ('77777777-7777-4777-8777-777777777777', 'hidden');
insert into auth.sessions (id, user_id, created_at, updated_at, ip)
values (gen_random_uuid(), '77777777-7777-4777-8777-777777777777', now(), now(), '203.0.113.7');
insert into auth.mfa_factors (id, user_id, factor_type, status, created_at, updated_at)
values (gen_random_uuid(), '77777777-7777-4777-8777-777777777777', 'totp', 'verified', now(), now());
insert into auth.audit_log_entries (id, instance_id, payload, ip_address, created_at)
values (
  gen_random_uuid(), '00000000-0000-0000-0000-000000000000',
  '{"actor_id": "77777777-7777-4777-8777-777777777777", "actor_username": "maria.santos@school.example", "action": "login"}',
  '203.0.113.7', now()
);
insert into auth.audit_log_entries (id, instance_id, payload, ip_address, created_at)
values (
  gen_random_uuid(), '00000000-0000-0000-0000-000000000000',
  '{"actor_username": "someone.else@school.example", "action": "login", "note": "Maria is a common name"}', '198.51.100.9', now()
);

select is(
  (select mode from private.erasure_settings),
  'keep_messages',
  'a server keeps the messages of an erased person unless it says otherwise'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 1));
select is(public.erasure_policy(), 'keep_messages', 'an administrator can read the policy');
select is(
  has_function_privilege('authenticated', 'public.erase_user_data(uuid, text)', 'execute'),
  false,
  'a signed in user cannot erase anyone'
);
select is(
  has_function_privilege('authenticated', 'public.find_login_by_email(text)', 'execute'),
  false,
  'a signed in user cannot look up logins'
);

reset role;
-- admin-users does this through the Auth API before it calls the function.
update auth.users set email = 'erased-77777777-7777-4777-8777-777777777777@erased.invalid'
where id = '77777777-7777-4777-8777-777777777777';

select lives_ok(
  $$ select public.erase_user_data('77777777-7777-4777-8777-777777777777', 'maria.santos@school.example') $$,
  'the service role can erase a person who is in no organization'
);

select is(
  (select display_name || '/' || coalesce(avatar_path, '-') || '/' || coalesce(status, '-') from public.profiles
   where id = '77777777-7777-4777-8777-777777777777'),
  'Former member/-/-',
  'the profile keeps only the placeholder name'
);
select is(
  (select count(*)::int from public.push_subscriptions where user_id = '77777777-7777-4777-8777-777777777777')
  + (select count(*)::int from public.user_settings where user_id = '77777777-7777-4777-8777-777777777777')
  + (select count(*)::int from auth.sessions where user_id = '77777777-7777-4777-8777-777777777777')
  + (select count(*)::int from auth.mfa_factors where user_id = '77777777-7777-4777-8777-777777777777'),
  0,
  'devices, settings, sessions and authenticator factors are gone'
);
select is(
  (select ip_address from auth.audit_log_entries where payload::text like '%erased-77777777%'),
  '',
  'the address in the Auth log of that person is blank'
);
select is(
  (select ip_address from auth.audit_log_entries where payload::text like '%someone.else%'),
  '198.51.100.9',
  'the Auth log rows of other people are not touched'
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
    where table_schema in ('auth', 'public', 'storage') and table_type = 'BASE TABLE'
  loop
    execute format(
      'select count(*) from %I.%I as row where row::text ilike %L or row::text ilike %L or row::text like %L',
      scanned.schema_name, scanned.table_name, '%maria.santos%', '%Maria Santos%', '%+639170000000%'
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
  'the old email, the name and the phone number are nowhere in the auth, public or storage tables'
);

select throws_ok(
  $$ select public.erase_user_data(tests.seed_id('20', 'a', 4), 'student1@rnhs.test') $$,
  '42501',
  'Remove the person from every organization first',
  'someone who is still a member cannot be erased'
);

update private.erasure_settings set mode = 'off';
select throws_ok(
  $$ select public.erase_user_data('77777777-7777-4777-8777-777777777777', 'maria.santos@school.example') $$,
  '42501',
  'Erasure is turned off on this server',
  'nobody can be erased when the server turned it off'
);
update private.erasure_settings set mode = 'keep_messages';

select is(
  (select is_disabled::text || '/' || membership_count::text from public.find_login_by_email('Student1@RNHS.test')),
  'false/1',
  'a login is found by its email in any letter case, with its membership count'
);

select * from finish();
rollback;
