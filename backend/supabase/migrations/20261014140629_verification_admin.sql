-- Fase 5: verifikasi seller dan admin

-- ===== Fungsi bantu =====
-- Id toko milik penjual aktif, apa pun status verifikasinya (untuk unggah dokumen)
create or replace function public.my_any_shop_id()
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
$$;

revoke all on function public.my_any_shop_id() from public;
grant execute on function public.my_any_shop_id() to authenticated;

-- ===== Penyimpanan dokumen sertifikat =====
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'certificates', 'certificates', false, 5242880,
  array['application/pdf', 'image/jpeg', 'image/png']
)
on conflict (id) do update
  set public = false,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

create policy "certificates_upload_own_shop" on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'certificates'
    and (storage.foldername(name))[1] = public.my_any_shop_id()::text
  );

create policy "certificates_read_own_or_admin" on storage.objects
  for select to authenticated
  using (
    bucket_id = 'certificates'
    and (
      public.is_admin()
      or (storage.foldername(name))[1] = public.my_any_shop_id()::text
    )
  );

-- ===== Kirim sertifikat (penjual) =====
create or replace function public.submit_certificate(
  p_type text,
  p_number text,
  p_issued_by text,
  p_issued_date date,
  p_expired_date date,
  p_document_path text
)
returns public.certificates
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_shop public.shops;
  v_today date := (now() at time zone 'Asia/Jakarta')::date;
  v_cert public.certificates;
begin
  select s.* into v_shop
  from public.shops s
  join public.profiles p on p.id = s.owner_id
  where s.owner_id = auth.uid() and p.role = 'SELLER' and p.is_active
  for update of s;

  if not found then
    raise exception 'Hanya penjual yang bisa mengirim sertifikat';
  end if;
  if v_shop.verification_status = 'APPROVED' then
    raise exception 'Toko sudah terverifikasi';
  end if;
  if v_shop.verification_status = 'PENDING'
     and exists (
       select 1 from public.certificates c
       where c.shop_id = v_shop.id and c.status = 'PENDING'
     ) then
    raise exception 'Pengajuan sedang diperiksa admin';
  end if;

  if p_type not in (
    'Sertifikat Produksi Pangan Industri Rumah Tangga',
    'Sertifikat Izin Edar BPOM',
    'Sertifikat Halal & Produksi Pangan'
  ) then
    raise exception 'Jenis sertifikat tidak valid';
  end if;
  if char_length(trim(coalesce(p_number, ''))) < 8 then
    raise exception 'Nomor sertifikat minimal 8 karakter';
  end if;
  if char_length(trim(coalesce(p_issued_by, ''))) < 3 then
    raise exception 'Nama lembaga penerbit minimal 3 karakter';
  end if;
  if p_issued_date is null or p_issued_date > v_today then
    raise exception 'Tanggal terbit tidak valid';
  end if;
  if p_expired_date is null or p_expired_date <= p_issued_date then
    raise exception 'Masa berlaku harus setelah tanggal terbit';
  end if;
  if p_expired_date <= v_today then
    raise exception 'Sertifikat sudah kedaluwarsa';
  end if;

  if p_document_path is null or position(v_shop.id::text || '/' in p_document_path) <> 1 then
    raise exception 'Dokumen tidak valid';
  end if;
  if not exists (
    select 1 from storage.objects o
    where o.bucket_id = 'certificates' and o.name = p_document_path
  ) then
    raise exception 'Dokumen belum diunggah';
  end if;

  insert into public.certificates (
    shop_id, type, number, issued_by, issued_date, expired_date, document_path, status
  )
  values (
    v_shop.id, p_type, trim(p_number), trim(p_issued_by),
    p_issued_date, p_expired_date, p_document_path, 'PENDING'
  )
  returning * into v_cert;

  update public.shops
  set verification_status = 'PENDING', rejection_reason = null
  where id = v_shop.id;

  return v_cert;
end;
$$;

-- ===== Setujui atau tolak toko (admin) =====
create or replace function public.review_shop(
  p_shop_id uuid,
  p_approve boolean,
  p_reason text default null
)
returns public.shops
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_shop public.shops;
  v_cert public.certificates;
  v_today date := (now() at time zone 'Asia/Jakarta')::date;
begin
  if not public.is_admin() then
    raise exception 'Hanya admin yang bisa memverifikasi toko';
  end if;

  select * into v_shop from public.shops where id = p_shop_id for update;
  if not found then
    raise exception 'Toko tidak ditemukan';
  end if;
  if v_shop.verification_status <> 'PENDING' then
    raise exception 'Toko ini tidak sedang menunggu verifikasi';
  end if;

  select * into v_cert
  from public.certificates
  where shop_id = p_shop_id and status = 'PENDING'
  order by submitted_at desc
  limit 1
  for update;
  if not found then
    raise exception 'Toko belum mengirim sertifikat';
  end if;

  if p_approve then
    if v_cert.expired_date <= v_today then
      raise exception 'Sertifikat sudah kedaluwarsa';
    end if;
    update public.certificates
    set status = 'ACTIVE', reviewed_at = now()
    where id = v_cert.id;
    update public.shops
    set verification_status = 'APPROVED', rejection_reason = null
    where id = p_shop_id
    returning * into v_shop;
  else
    if char_length(trim(coalesce(p_reason, ''))) < 5 then
      raise exception 'Alasan penolakan minimal 5 karakter';
    end if;
    update public.certificates
    set status = 'REJECTED', reviewed_at = now()
    where id = v_cert.id;
    update public.shops
    set verification_status = 'REJECTED', rejection_reason = trim(p_reason)
    where id = p_shop_id
    returning * into v_shop;
  end if;

  return v_shop;
end;
$$;

-- ===== Aktifkan atau nonaktifkan akun (admin) =====
-- Selain is_active, login diblokir di Supabase Auth (banned_until)
create or replace function public.set_user_active(p_user_id uuid, p_active boolean)
returns public.profiles
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_profile public.profiles;
begin
  if not public.is_admin() then
    raise exception 'Hanya admin yang bisa mengubah status akun';
  end if;

  select * into v_profile from public.profiles where id = p_user_id for update;
  if not found then
    raise exception 'Akun tidak ditemukan';
  end if;
  if v_profile.role = 'ADMIN' then
    raise exception 'Akun admin tidak dapat diubah';
  end if;

  update public.profiles
  set is_active = p_active
  where id = p_user_id
  returning * into v_profile;

  update auth.users
  set banned_until = case when p_active then null else now() + interval '100 years' end
  where id = p_user_id;

  return v_profile;
end;
$$;

-- ===== Ringkasan admin =====
create or replace function public.admin_overview()
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_result jsonb;
begin
  if not public.is_admin() then
    raise exception 'Hanya admin';
  end if;

  select jsonb_build_object(
    'buyers', (select count(*) from public.profiles where role = 'BUYER'),
    'sellers', (select count(*) from public.profiles where role = 'SELLER'),
    'pending_verifications', (select count(*) from public.shops where verification_status = 'PENDING'),
    'inactive_accounts', (select count(*) from public.profiles where not is_active),
    'products', (select count(*) from public.products where deleted_at is null),
    'orders', (select count(*) from public.orders),
    'done_orders', (select count(*) from public.orders where status = 'DONE'),
    'revenue', (select coalesce(sum(subtotal), 0) from public.orders where status = 'DONE'),
    'reviews', (select count(*) from public.reviews),
    'average_rating', (select coalesce(round(avg(rating)::numeric, 1), 0) from public.reviews),
    'orders_by_status', (
      select jsonb_object_agg(x.s, coalesce(x.c, 0))
      from (
        select e.s::text as s,
               (select count(*) from public.orders o where o.status = e.s) as c
        from unnest(enum_range(null::public.order_status)) as e(s)
      ) x
    ),
    'top_sellers', (
      select coalesce(
        jsonb_agg(jsonb_build_object('shop', t.shop_name, 'revenue', t.revenue) order by t.revenue desc),
        '[]'::jsonb
      )
      from (
        select shop_name, sum(subtotal)::integer as revenue
        from public.orders
        where status = 'DONE'
        group by shop_name
        order by sum(subtotal) desc
        limit 3
      ) t
    )
  ) into v_result;

  return v_result;
end;
$$;

-- ===== Izin eksekusi =====
revoke all on function public.submit_certificate(text, text, text, date, date, text) from public;
revoke all on function public.review_shop(uuid, boolean, text) from public;
revoke all on function public.set_user_active(uuid, boolean) from public;
revoke all on function public.admin_overview() from public;

grant execute on function public.submit_certificate(text, text, text, date, date, text) to authenticated;
grant execute on function public.review_shop(uuid, boolean, text) to authenticated;
grant execute on function public.set_user_active(uuid, boolean) to authenticated;
grant execute on function public.admin_overview() to authenticated;