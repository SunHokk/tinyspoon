-- Fase 1: autentikasi, profil, dan pengunci akses

-- 1. Cabut hak akses bawaan. Hak dibuka satu per satu di tiap fase.
revoke all on all tables in schema public from anon, authenticated;
revoke all on all sequences in schema public from anon, authenticated;

alter default privileges in schema public revoke all on tables from anon, authenticated;
alter default privileges in schema public revoke all on sequences from anon, authenticated;
alter default privileges in schema public revoke execute on functions from anon, authenticated;

-- 2. Fungsi bantu (security definer supaya tidak terkena RLS dan tidak rekursif)
create or replace function public.app_role()
returns public.user_role
language sql
stable
security definer
set search_path = ''
as $$
  select role from public.profiles where id = auth.uid() and is_active
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(
    (select role = 'ADMIN' from public.profiles where id = auth.uid() and is_active),
    false
  )
$$;

create or replace function public.check_shop_name(p_name text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select char_length(trim(p_name)) >= 3
     and not exists (
       select 1 from public.shops where lower(name) = lower(trim(p_name))
     )
$$;

revoke all on function public.app_role() from public;
revoke all on function public.is_admin() from public;
revoke all on function public.check_shop_name(text) from public;
grant execute on function public.app_role() to authenticated;
grant execute on function public.is_admin() to authenticated;
grant execute on function public.check_shop_name(text) to anon, authenticated;

-- 3. Trigger: buat profil (dan toko untuk penjual) saat ada pendaftaran baru.
-- Peran hanya BUYER atau SELLER. ADMIN tidak bisa diminta lewat pendaftaran.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_role public.user_role;
  v_name text;
  v_shop text;
begin
  v_role := case
    when upper(coalesce(new.raw_user_meta_data ->> 'role', '')) = 'SELLER'
      then 'SELLER'::public.user_role
    else 'BUYER'::public.user_role
  end;

  v_name := nullif(trim(coalesce(new.raw_user_meta_data ->> 'name', '')), '');
  if v_name is null or char_length(v_name) < 3 then
    v_name := coalesce(nullif(split_part(new.email, '@', 1), ''), 'Pengguna');
    if char_length(v_name) < 3 then
      v_name := 'Pengguna';
    end if;
  end if;

  insert into public.profiles (id, name, email, role)
  values (new.id, v_name, new.email, v_role);

  if v_role = 'SELLER' then
    v_shop := trim(coalesce(new.raw_user_meta_data ->> 'shop_name', ''));
    if char_length(v_shop) < 3 then
      raise exception 'Nama toko minimal 3 karakter';
    end if;
    insert into public.shops (owner_id, name) values (new.id, v_shop);
  end if;

  insert into public.notification_prefs (user_id) values (new.id);

  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 4. Policy profiles: baca milik sendiri (admin: semua), ubah hanya kolom name
grant select on public.profiles to authenticated;
grant update (name) on public.profiles to authenticated;

create policy "profiles_select_own_or_admin" on public.profiles
  for select to authenticated
  using (id = auth.uid() or public.is_admin());

create policy "profiles_update_own_name" on public.profiles
  for update to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

-- 5. Policy shops (sementara): pemilik dan admin bisa membaca.
-- Fase 2 menambah akses baca toko terverifikasi untuk semua pembeli.
grant select on public.shops to authenticated;

create policy "shops_select_own_or_admin" on public.shops
  for select to authenticated
  using (owner_id = auth.uid() or public.is_admin());