-- Enable profile bootstrap on new Supabase Auth sign-ups.
-- Requires: db/rls.sql (handle_new_user function) already applied.

drop trigger if exists on_auth_user_created on auth.users;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();