-- Announcement conversations are created automatically when the first member joins an organization, and the app no
-- longer calls this function. Old audit entries named announcements.enabled stay readable.

drop function public.create_announcement_conversation(uuid);
