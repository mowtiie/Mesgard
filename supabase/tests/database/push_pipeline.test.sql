begin;
select plan(18);


-- Message 2 is sent by staff one in a direct conversation with student one, who has a device token.
select tests.authenticate_as_anonymous();
reset role;
set local role service_role;

select is(
  (select count(*)::int from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  1,
  'a direct message is delivered to the other participant only'
);
select is(
  (select recipient_id from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  tests.seed_id('20', 'a', 4),
  'the recipient is the participant with a device token'
);
select is(
  (select preview from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  'sender_only',
  'the preview defaults to sender only'
);
select is(
  (select body from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  null,
  'the text is withheld in sender only mode'
);
select isnt(
  (select sender_name from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  null,
  'the sender name is returned in sender only mode'
);

reset role;
insert into public.user_settings (user_id, notification_preview)
values (tests.seed_id('20', 'a', 4), 'sender_and_text');
set local role service_role;
select is(
  (select body from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  'Of course. It is due on Friday.',
  'the text is returned when the user chose sender and text'
);

reset role;
update public.user_settings set notification_preview = 'hidden' where user_id = tests.seed_id('20', 'a', 4);
set local role service_role;
select is(
  (select coalesce(sender_name, '') || coalesce(body, '') from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  '',
  'neither the sender nor the text is returned in hidden mode'
);

reset role;
delete from public.user_settings where user_id = tests.seed_id('20', 'a', 4);
update public.organizations set notification_preview_default = 'hidden' where id = tests.seed_id('10', 'a', 0);
set local role service_role;
select is(
  (select preview from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  'hidden',
  'the organization default applies when the user has no choice'
);

reset role;
update public.conversation_participants set muted = true
where conversation_id = tests.seed_id('40', 'a', 1) and user_id = tests.seed_id('20', 'a', 4);
set local role service_role;
select is(
  (select count(*)::int from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  0,
  'muted participants receive no delivery'
);

reset role;
update public.conversation_participants set muted = false
where conversation_id = tests.seed_id('40', 'a', 1) and user_id = tests.seed_id('20', 'a', 4);
update public.messages set deleted_at = now(), body = '' where id = tests.seed_id('50', 'a', 2);
set local role service_role;
select is(
  (select count(*)::int from public.list_push_deliveries(tests.seed_id('50', 'a', 2))),
  0,
  'a deleted message has no delivery'
);

-- Message 3 is sent by staff one to the group conversation.
select is(
  (select conversation_title from public.list_push_deliveries(tests.seed_id('50', 'a', 3))),
  'Grade 10 Section A',
  'a group delivery carries the conversation title'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select throws_ok(
  format('select * from public.list_push_deliveries(%L)', tests.seed_id('50', 'a', 3)),
  '42501',
  null,
  'signed in users cannot list push deliveries'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 5));
select lives_ok(
  $$select public.register_device_token('seed-fcm-token-rnhs')$$,
  'a user can register a device token'
);
select is(
  (select count(*)::int from public.device_tokens where fcm_token = 'seed-fcm-token-rnhs'),
  1,
  'registering a token already held by another user moves it'
);
select is(
  (select user_id from public.device_tokens where fcm_token = 'seed-fcm-token-rnhs'),
  tests.seed_id('20', 'a', 5),
  'the token now belongs to the caller'
);

select tests.authenticate_as_anonymous();
select throws_ok(
  $$select public.register_device_token('anonymous-token')$$,
  '42501',
  null,
  'anonymous users cannot register a device token'
);

-- The webhook trigger queues a request only when push settings exist and never blocks a message.
reset role;
delete from private.push_settings;
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format(
    $f$insert into public.messages (id, org_id, conversation_id, sender_id, body)
       values (gen_random_uuid(), %L, %L, %L, 'Hello')$f$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 4)
  ),
  'sending a message works without push settings'
);

reset role;
insert into private.push_settings (function_url, webhook_secret)
values ('http://example.invalid/send-push', 'test-webhook-secret');
create temporary table queued_before as select count(*) as total from net.http_request_queue;
select tests.authenticate_as(tests.seed_id('20', 'a', 4));
insert into public.messages (id, org_id, conversation_id, sender_id, body)
values (gen_random_uuid(), tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 4), 'Hello again');
reset role;
select is(
  (select count(*) from net.http_request_queue) - (select total from queued_before),
  1::bigint,
  'a new message queues one webhook request when push settings exist'
);

select * from finish();
rollback;
