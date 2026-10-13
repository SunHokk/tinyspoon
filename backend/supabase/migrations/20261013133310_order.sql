-- Fase 3: pesanan

-- Snapshot nama toko dan nama pembeli
alter table public.orders
  add column shop_name text not null default '',
  add column buyer_name text not null default '';

-- Buat pesanan (satu pesanan per toko)
create or replace function public.place_order(
  p_items jsonb,
  p_address text,
  p_notes text default '',
  p_payment_method text default 'Transfer Bank'
)
returns setof public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
  v_profile public.profiles;
  v_day text := to_char(now() at time zone 'Asia/Jakarta', 'YYYYMMDD');
  v_delivery_fee constant integer := 10000;
  v_requested integer;
  v_valid integer;
  v_merged jsonb;
  v_shop record;
  v_seq integer;
  v_subtotal integer;
  v_order public.orders;
begin
  select * into v_profile from public.profiles where id = v_user and is_active;
  if not found or v_profile.role <> 'BUYER' then
    raise exception 'Hanya pembeli yang bisa membuat pesanan';
  end if;

  if p_items is null or jsonb_typeof(p_items) <> 'array' or jsonb_array_length(p_items) = 0 then
    raise exception 'Keranjang kosong';
  end if;

  if exists (
    select 1 from jsonb_array_elements(p_items) e
    where (e ->> 'product_id') is null
       or coalesce((e ->> 'quantity')::integer, 0) not between 1 and 50
  ) then
    raise exception 'Item keranjang tidak valid (jumlah 1 sampai 50 per item)';
  end if;

  if p_address is null or char_length(trim(p_address)) < 10 then
    raise exception 'Alamat pengiriman minimal 10 karakter';
  end if;

  if char_length(coalesce(p_notes, '')) > 200 then
    raise exception 'Catatan maksimal 200 karakter';
  end if;

  if p_payment_method not in ('Transfer Bank', 'QRIS', 'COD (Bayar di Tempat)') then
    raise exception 'Metode pembayaran tidak valid';
  end if;

  -- Semua produk harus ada, belum dihapus, dan tokonya terlihat
  select count(*) into v_requested
  from (
    select distinct (e ->> 'product_id')::uuid as product_id
    from jsonb_array_elements(p_items) e
  ) d;

  select count(*) into v_valid
  from (
    select distinct pr.id
    from jsonb_array_elements(p_items) e
    join public.products pr on pr.id = (e ->> 'product_id')::uuid
    where pr.deleted_at is null
      and public.shop_is_visible(pr.shop_id)
  ) d;

  if v_requested <> v_valid then
    raise exception 'Ada produk yang sudah tidak tersedia';
  end if;

  -- Gabungkan produk kembar
  select jsonb_agg(jsonb_build_object('product_id', t.product_id, 'quantity', t.quantity))
  into v_merged
  from (
    select (e ->> 'product_id')::uuid as product_id,
           sum((e ->> 'quantity')::integer)::integer as quantity
    from jsonb_array_elements(p_items) e
    group by 1
  ) t;

  for v_shop in
    select pr.shop_id, s.name as shop_name
    from jsonb_to_recordset(v_merged) as m(product_id uuid, quantity integer)
    join public.products pr on pr.id = m.product_id
    join public.shops s on s.id = pr.shop_id
    group by pr.shop_id, s.name
    order by s.name
  loop
    insert into public.order_counters (day, last)
    values (v_day, 1)
    on conflict (day) do update set last = public.order_counters.last + 1
    returning last into v_seq;

    select sum(pr.price * m.quantity)::integer into v_subtotal
    from jsonb_to_recordset(v_merged) as m(product_id uuid, quantity integer)
    join public.products pr on pr.id = m.product_id
    where pr.shop_id = v_shop.shop_id;

    insert into public.orders (
      code, buyer_id, shop_id, shop_name, buyer_name, status,
      subtotal, delivery_fee, address, notes, payment_method
    )
    values (
      'TS-' || v_day || '-' || lpad(v_seq::text, 3, '0'),
      v_user, v_shop.shop_id, v_shop.shop_name, v_profile.name, 'NEW',
      v_subtotal, v_delivery_fee, trim(p_address), coalesce(trim(p_notes), ''), p_payment_method
    )
    returning * into v_order;

    insert into public.order_items (order_id, product_id, product_name, unit_price, quantity)
    select v_order.id, pr.id, pr.name, pr.price, m.quantity
    from jsonb_to_recordset(v_merged) as m(product_id uuid, quantity integer)
    join public.products pr on pr.id = m.product_id
    where pr.shop_id = v_shop.shop_id;

    return next v_order;
  end loop;

  return;
end;
$$;

-- Ubah status pesanan (penjual pemilik toko)
create or replace function public.set_order_status(
  p_order_id uuid,
  p_status public.order_status
)
returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_shop uuid := public.my_shop_id();
  v_order public.orders;
begin
  if v_shop is null then
    raise exception 'Hanya penjual terverifikasi yang bisa mengubah status pesanan';
  end if;

  select * into v_order
  from public.orders
  where id = p_order_id and shop_id = v_shop
  for update;

  if not found then
    raise exception 'Pesanan tidak ditemukan';
  end if;

  if not (
    (v_order.status = 'NEW' and p_status in ('COOKING', 'REJECTED'))
    or (v_order.status = 'COOKING' and p_status = 'SHIPPING')
    or (v_order.status = 'SHIPPING' and p_status = 'DONE')
  ) then
    raise exception 'Perubahan status dari % ke % tidak diizinkan', v_order.status, p_status;
  end if;

  update public.orders
  set status = p_status, updated_at = now()
  where id = p_order_id
  returning * into v_order;

  return v_order;
end;
$$;

revoke all on function public.place_order(jsonb, text, text, text) from public;
revoke all on function public.set_order_status(uuid, public.order_status) from public;
grant execute on function public.place_order(jsonb, text, text, text) to authenticated;
grant execute on function public.set_order_status(uuid, public.order_status) to authenticated;

-- Akses baca pesanan (tanpa hak tulis langsung)
grant select on public.orders to authenticated;
grant select on public.order_items to authenticated;

create policy "orders_select" on public.orders
  for select to authenticated
  using (
    (buyer_id = auth.uid() and public.app_role() = 'BUYER')
    or shop_id = public.my_shop_id()
    or public.is_admin()
  );

-- Item pesanan mengikuti aturan pesanan induknya
create policy "order_items_select" on public.order_items
  for select to authenticated
  using (exists (select 1 from public.orders o where o.id = order_items.order_id));