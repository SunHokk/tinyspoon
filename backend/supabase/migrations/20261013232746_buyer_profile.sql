-- Fase 4: profil pembeli

-- ===== Ulasan =====
-- Nama penulis disimpan di ulasan. user_id boleh kosong untuk ulasan contoh dari seed.
alter table public.reviews
  add column author_name text not null default '',
  alter column user_id drop not null;

drop policy "reviews_select_visible" on public.reviews;

create policy "reviews_select" on public.reviews
  for select to authenticated
  using (public.is_admin() or user_id = auth.uid() or public.product_is_visible(product_id));

create or replace function public.submit_review(
  p_product_id uuid,
  p_rating integer,
  p_comment text,
  p_baby_age text default null
)
returns public.reviews
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_profile public.profiles;
  v_review public.reviews;
begin
  select * into v_profile from public.profiles where id = auth.uid() and is_active;
  if not found or v_profile.role <> 'BUYER' then
    raise exception 'Hanya pembeli yang bisa menulis ulasan';
  end if;
  if p_rating is null or p_rating not between 1 and 5 then
    raise exception 'Rating harus 1 sampai 5';
  end if;
  if p_comment is null or char_length(trim(p_comment)) = 0 then
    raise exception 'Ulasan tidak boleh kosong';
  end if;
  if char_length(p_comment) > 500 then
    raise exception 'Ulasan maksimal 500 karakter';
  end if;
  if not public.product_is_visible(p_product_id) then
    raise exception 'Produk tidak tersedia';
  end if;

  insert into public.reviews (product_id, user_id, author_name, rating, comment, baby_age)
  values (
    p_product_id, v_profile.id, v_profile.name, p_rating, trim(p_comment),
    nullif(trim(coalesce(p_baby_age, '')), '')
  )
  on conflict (product_id, user_id) do update
    set rating = excluded.rating,
        comment = excluded.comment,
        baby_age = excluded.baby_age,
        author_name = excluded.author_name,
        created_at = now()
  returning * into v_review;

  return v_review;
end;
$$;

-- ===== Favorit =====
-- user_id diisi database (default), tidak ada hak menulis kolom itu, jadi tidak bisa dipalsukan
alter table public.favorites alter column user_id set default auth.uid();

grant select, delete on public.favorites to authenticated;
grant insert (product_id) on public.favorites to authenticated;

create policy "favorites_select_own" on public.favorites
  for select to authenticated
  using (user_id = auth.uid());

create policy "favorites_insert_own" on public.favorites
  for insert to authenticated
  with check (
    user_id = auth.uid()
    and public.app_role() = 'BUYER'
    and public.product_is_visible(product_id)
  );

create policy "favorites_delete_own" on public.favorites
  for delete to authenticated
  using (user_id = auth.uid());

-- ===== Alamat =====
alter table public.addresses
  add column created_at timestamptz not null default now();

grant select on public.addresses to authenticated;

create policy "addresses_select_own" on public.addresses
  for select to authenticated
  using (user_id = auth.uid());

create or replace function public.save_address(
  p_id uuid,
  p_label text,
  p_recipient text,
  p_full_address text,
  p_make_default boolean default false
)
returns public.addresses
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
  v_count integer;
  v_default boolean;
  v_row public.addresses;
begin
  if public.app_role() is distinct from 'BUYER'::public.user_role then
    raise exception 'Hanya pembeli yang bisa menyimpan alamat';
  end if;
  if p_label not in ('Rumah', 'Kantor', 'Lainnya') then
    raise exception 'Label alamat tidak valid';
  end if;
  if char_length(trim(coalesce(p_recipient, ''))) < 3 then
    raise exception 'Nama penerima minimal 3 karakter';
  end if;
  if char_length(trim(coalesce(p_full_address, ''))) < 10 then
    raise exception 'Alamat minimal 10 karakter';
  end if;

  select count(*) into v_count from public.addresses where user_id = v_user;

  if p_id is null then
    if v_count >= 10 then
      raise exception 'Maksimal 10 alamat';
    end if;
    v_default := coalesce(p_make_default, false) or v_count = 0;
    if v_default then
      update public.addresses set is_default = false where user_id = v_user and is_default;
    end if;
    insert into public.addresses (user_id, label, recipient, full_address, is_default)
    values (v_user, p_label, trim(p_recipient), trim(p_full_address), v_default)
    returning * into v_row;
  else
    select * into v_row from public.addresses where id = p_id and user_id = v_user for update;
    if not found then
      raise exception 'Alamat tidak ditemukan';
    end if;
    if coalesce(p_make_default, false) and not v_row.is_default then
      update public.addresses set is_default = false where user_id = v_user and is_default;
    end if;
    update public.addresses
    set label = p_label,
        recipient = trim(p_recipient),
        full_address = trim(p_full_address),
        is_default = v_row.is_default or coalesce(p_make_default, false)
    where id = p_id
    returning * into v_row;
  end if;

  return v_row;
end;
$$;

create or replace function public.set_default_address(p_id uuid)
returns public.addresses
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
  v_row public.addresses;
begin
  perform 1 from public.addresses where id = p_id and user_id = v_user for update;
  if not found then
    raise exception 'Alamat tidak ditemukan';
  end if;

  update public.addresses set is_default = false where user_id = v_user and is_default;
  update public.addresses set is_default = true where id = p_id returning * into v_row;
  return v_row;
end;
$$;

create or replace function public.delete_address(p_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
  v_row public.addresses;
begin
  delete from public.addresses where id = p_id and user_id = v_user returning * into v_row;
  if not found then
    raise exception 'Alamat tidak ditemukan';
  end if;

  if v_row.is_default then
    update public.addresses
    set is_default = true
    where id = (
      select id from public.addresses
      where user_id = v_user
      order by created_at, id
      limit 1
    );
  end if;
end;
$$;

-- ===== Preferensi notifikasi =====
grant select on public.notification_prefs to authenticated;
grant update (order_status, promo, review_reminder, new_products)
  on public.notification_prefs to authenticated;

create policy "prefs_select_own" on public.notification_prefs
  for select to authenticated
  using (user_id = auth.uid());

create policy "prefs_update_own" on public.notification_prefs
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- ===== Rating aplikasi =====
grant select on public.app_ratings to authenticated;

create policy "app_ratings_select_own" on public.app_ratings
  for select to authenticated
  using (user_id = auth.uid());

create or replace function public.submit_app_rating(p_rating integer, p_feedback text default '')
returns public.app_ratings
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_row public.app_ratings;
begin
  if public.app_role() is distinct from 'BUYER'::public.user_role then
    raise exception 'Hanya pembeli yang bisa memberi rating aplikasi';
  end if;
  if p_rating is null or p_rating not between 1 and 5 then
    raise exception 'Rating harus 1 sampai 5';
  end if;
  if char_length(coalesce(p_feedback, '')) > 500 then
    raise exception 'Masukan maksimal 500 karakter';
  end if;

  insert into public.app_ratings (user_id, rating, feedback, updated_at)
  values (auth.uid(), p_rating, trim(coalesce(p_feedback, '')), now())
  on conflict (user_id) do update
    set rating = excluded.rating,
        feedback = excluded.feedback,
        updated_at = now()
  returning * into v_row;

  return v_row;
end;
$$;

-- ===== Izin eksekusi fungsi =====
revoke all on function public.submit_review(uuid, integer, text, text) from public;
revoke all on function public.save_address(uuid, text, text, text, boolean) from public;
revoke all on function public.set_default_address(uuid) from public;
revoke all on function public.delete_address(uuid) from public;
revoke all on function public.submit_app_rating(integer, text) from public;

grant execute on function public.submit_review(uuid, integer, text, text) to authenticated;
grant execute on function public.save_address(uuid, text, text, text, boolean) to authenticated;
grant execute on function public.set_default_address(uuid) to authenticated;
grant execute on function public.delete_address(uuid) to authenticated;
grant execute on function public.submit_app_rating(integer, text) to authenticated;