-- The pictures of a person, so admin-users can delete the files from Storage when the person is erased. A picture kept
-- as evidence of a report is left out, and the report protects the person from erasure while it is open anyway.

create function public.list_user_avatar_paths(target_user_id uuid)
returns setof text
language sql
stable
security definer
set search_path = ''
as $$
  select object.name
  from storage.objects as object
  where object.bucket_id = 'avatars'
    and private.avatar_path_user(object.name) = target_user_id
    and not private.avatar_is_evidence(object.name);
$$;

revoke execute on function public.list_user_avatar_paths(uuid) from public, anon, authenticated;
grant execute on function public.list_user_avatar_paths(uuid) to service_role;
