begin;
select plan(15);

select tests.authenticate_as(tests.seed_id('20', 'a', 6));
select throws_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('message-attachments', '%s/%s/photo', %L, '{"size": 10, "mimetype": "image/png"}')$$,
    tests.seed_id('40', 'a', 1), tests.seed_id('50', 'a', 98), tests.seed_id('20', 'a', 6)
  ),
  '42501',
  null,
  'a non-participant cannot upload an attachment to a conversation'
);
select throws_ok(
  format(
    $$select public.send_message(%L, %L, 'Hello', '[]')$$,
    tests.seed_id('50', 'a', 98), tests.seed_id('40', 'a', 1)
  ),
  '42501',
  null,
  'a non-participant cannot send a message through send_message'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 4));
select lives_ok(
  format(
    $$insert into storage.objects (bucket_id, name, owner_id, metadata)
      values ('message-attachments', '%s/%s/photo', %L, '{"size": 2048, "mimetype": "image/png"}')$$,
    tests.seed_id('40', 'a', 1), tests.seed_id('50', 'a', 98), tests.seed_id('20', 'a', 4)
  ),
  'a participant can upload an attachment to their conversation'
);
select lives_ok(
  format(
    $$select public.send_message(%L, %L, '', jsonb_build_array(
      jsonb_build_object('id', %L, 'storage_path', '%s/%s/photo', 'file_name', 'photo.png')))$$,
    tests.seed_id('50', 'a', 98), tests.seed_id('40', 'a', 1), tests.seed_id('60', 'a', 99),
    tests.seed_id('40', 'a', 1), tests.seed_id('50', 'a', 98)
  ),
  'a participant can send a message that has only an attachment'
);
select is(
  (select has_attachments from public.messages where id = tests.seed_id('50', 'a', 98)),
  true,
  'the message is marked as having attachments'
);
select results_eq(
  format(
    $$select mime_type, size_bytes, file_name from public.attachments where message_id = %L$$,
    tests.seed_id('50', 'a', 98)
  ),
  $$values ('image/png'::text, 2048::bigint, 'photo.png'::text)$$,
  'the attachment takes its type and size from Storage'
);
select is(
  (select id from public.attachments where message_id = tests.seed_id('50', 'a', 98)),
  tests.seed_id('60', 'a', 99),
  'the attachment keeps the identifier chosen by the client'
);
select lives_ok(
  format(
    $$select public.send_message(%L, %L, '', jsonb_build_array(
      jsonb_build_object('storage_path', '%s/%s/photo', 'file_name', 'photo.png')))$$,
    tests.seed_id('50', 'a', 98), tests.seed_id('40', 'a', 1),
    tests.seed_id('40', 'a', 1), tests.seed_id('50', 'a', 98)
  ),
  'sending the same message again is idempotent'
);
select is(
  (select count(*)::int from public.attachments where message_id = tests.seed_id('50', 'a', 98)),
  1,
  'a retry does not duplicate the attachment'
);
select throws_ok(
  format(
    $$select public.send_message(%L, %L, '', jsonb_build_array(
      jsonb_build_object('storage_path', '%s/%s/photo', 'file_name', 'photo.png')))$$,
    tests.seed_id('50', 'a', 97), tests.seed_id('40', 'a', 1),
    tests.seed_id('40', 'a', 1), tests.seed_id('50', 'a', 98)
  ),
  '22023',
  null,
  'an attachment path of another message is refused'
);
select throws_ok(
  format(
    $$select public.send_message(%L, %L, '', jsonb_build_array(
      jsonb_build_object('storage_path', '%s/%s/missing', 'file_name', 'missing.png')))$$,
    tests.seed_id('50', 'a', 96), tests.seed_id('40', 'a', 1),
    tests.seed_id('40', 'a', 1), tests.seed_id('50', 'a', 96)
  ),
  '22023',
  null,
  'an attachment that was never uploaded is refused'
);
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body)
      values (gen_random_uuid(), %L, %L, %L, '')$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 4)
  ),
  '23514',
  null,
  'an empty message without attachments is refused'
);
select throws_ok(
  format(
    $$insert into public.messages (id, org_id, conversation_id, sender_id, body, has_attachments)
      values (gen_random_uuid(), %L, %L, %L, '', true)$$,
    tests.seed_id('10', 'a', 0), tests.seed_id('40', 'a', 1), tests.seed_id('20', 'a', 4)
  ),
  '42501',
  null,
  'a client cannot set the attachment flag directly'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 6));
select is(
  (select count(*)::int from storage.objects where bucket_id = 'message-attachments')
  + (select count(*)::int from public.attachments),
  0,
  'a non-participant cannot read attachment files or records'
);

select tests.authenticate_as(tests.seed_id('20', 'a', 2));
select is(
  (select count(*)::int from storage.objects
    where bucket_id = 'message-attachments' and name like '%/' || tests.seed_id('50', 'a', 98) || '/%'),
  1,
  'another participant can read the attachment file'
);

select * from finish();
rollback;
