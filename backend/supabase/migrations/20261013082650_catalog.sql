-- Fase 2: toko dan produk

-- Fungsi bantu (security definer supaya tidak rekursif terhadap RLS)
create or replace function public.shop_is_visible(p_shop_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.shops s
    join public.profiles p on p.id = s.owner_id
    where s.id = p_shop_id
      and s.verification_status = 'APPROVED'
      and p.is_active
  )
$$;

create or replace function public.my_shop_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
  select s.id
  from public.shops s
  join public.profiles p on p.id = s.owner_id
  where s.owner_id = auth.uid()
    and p.role = 'SELLER'
    and p.is_active
    and s.verification_status = 'APPROVED'
$$;

create or replace function public.product_is_visible(p_product_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.products pr
    where pr.id = p_product_id
      and pr.deleted_at is null
      and public.shop_is_visible(pr.shop_id)
  )
$$;

revoke all on function public.shop_is_visible(uuid) from public;
revoke all on function public.my_shop_id() from public;
revoke all on function public.product_is_visible(uuid) from public;
grant execute on function public.shop_is_visible(uuid) to authenticated;
grant execute on function public.my_shop_id() to authenticated;
grant execute on function public.product_is_visible(uuid) to authenticated;

-- Toko: semua pengguna login melihat toko terverifikasi (policy pemilik dan admin dari Fase 1 tetap berlaku)
create policy "shops_select_visible" on public.shops
  for select to authenticated
  using (public.shop_is_visible(id));

-- Pemilik hanya boleh mengubah kolom profil toko (nama dan status verifikasi terkunci)
grant update (description, address, phone, open_hours) on public.shops to authenticated;

create policy "shops_update_own" on public.shops
  for update to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

-- Sertifikat: pemilik, admin, dan semua pengguna untuk sertifikat aktif toko yang terlihat
grant select on public.certificates to authenticated;

create policy "certificates_select" on public.certificates
  for select to authenticated
  using (
    public.is_admin()
    or shop_id in (select id from public.shops where owner_id = auth.uid())
    or (status = 'ACTIVE' and public.shop_is_visible(shop_id))
  );

-- Produk
grant select on public.products to authenticated;
grant insert (shop_id, name, price, emoji, age_group, texture, allergens, protein, carbs, fat, fiber)
  on public.products to authenticated;
grant update (name, price, emoji, age_group, texture, allergens, protein, carbs, fat, fiber, deleted_at)
  on public.products to authenticated;
-- Tidak ada hak DELETE: produk hanya bisa di-soft delete lewat deleted_at

create policy "products_select" on public.products
  for select to authenticated
  using (
    public.is_admin()
    or shop_id = public.my_shop_id()
    or (deleted_at is null and public.shop_is_visible(shop_id))
  );

create policy "products_insert_own_shop" on public.products
  for insert to authenticated
  with check (shop_id = public.my_shop_id());

create policy "products_update_own_shop" on public.products
  for update to authenticated
  using (shop_id = public.my_shop_id())
  with check (shop_id = public.my_shop_id());

-- Ulasan: baca saja dulu (agar rating terhitung). Menulis ulasan dibuka di Fase 4.
grant select on public.reviews to authenticated;

create policy "reviews_select_visible" on public.reviews
  for select to authenticated
  using (public.is_admin() or public.product_is_visible(product_id));

-- View produk + nama toko + rating. security_invoker: RLS pemanggil tetap berlaku.
create view public.products_with_stats
  with (security_invoker = true) as
select
  p.id,
  p.shop_id,
  s.name as shop_name,
  p.name,
  p.price,
  p.emoji,
  p.age_group,
  p.texture,
  p.allergens,
  p.protein,
  p.carbs,
  p.fat,
  p.fiber,
  p.deleted_at,
  p.created_at,
  coalesce(r.rating, 0)::numeric(2, 1) as rating,
  coalesce(r.review_count, 0)::integer as review_count
from public.products p
join public.shops s on s.id = p.shop_id
left join (
  select product_id, round(avg(rating)::numeric, 1) as rating, count(*) as review_count
  from public.reviews
  group by product_id
) r on r.product_id = p.id;

grant select on public.products_with_stats to authenticated;