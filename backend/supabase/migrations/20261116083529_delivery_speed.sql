-- Fase 8: pilihan kecepatan pengantaran (ongkir dihitung di server)

alter table public.orders
  add column delivery_speed text not null default 'REGULAR'
  check (delivery_speed in ('SAVER', 'REGULAR', 'FAST'));

-- Ganti tanda tangan fungsi: hapus yang lama supaya tidak ada dua versi
drop function public.place_order(jsonb, text, text, text);

create or replace function public.place_order(
  p_items jsonb,
  p_address text,
  p_notes text default '',
  p_payment_method text default 'Transfer Bank',
  p_delivery_speed text default 'REGULAR'
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
  v_delivery_fee integer;
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

  v_delivery_fee := case p_delivery_speed
    when 'SAVER' then 8000
    when 'REGULAR' then 10000
    when 'FAST' then 15000
  end;
  if v_delivery_fee is null then
    raise exception 'Kecepatan pengantaran tidak valid';
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
      subtotal, delivery_fee, delivery_speed, address, notes, payment_method
    )
    values (
      'TS-' || v_day || '-' || lpad(v_seq::text, 3, '0'),
      v_user, v_shop.shop_id, v_shop.shop_name, v_profile.name, 'NEW',
      v_subtotal, v_delivery_fee, p_delivery_speed, trim(p_address),
      coalesce(trim(p_notes), ''), p_payment_method
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

revoke all on function public.place_order(jsonb, text, text, text, text) from public;
grant execute on function public.place_order(jsonb, text, text, text, text) to authenticated;