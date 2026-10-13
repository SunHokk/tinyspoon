-- TinySpoon: skema awal (Fase 0)
-- Semua tabel dikunci dengan Row Level Security. Tanpa policy, tidak ada akses lewat API.
-- Policy dan fungsi ditambahkan di migrasi berikutnya.

create type public.user_role as enum ('BUYER', 'SELLER', 'ADMIN');
create type public.verification_status as enum ('PENDING', 'APPROVED', 'REJECTED');
create type public.cert_status as enum ('PENDING', 'ACTIVE', 'EXPIRED');
create type public.order_status as enum ('NEW', 'COOKING', 'SHIPPING', 'DONE', 'REJECTED');
create type public.age_group as enum ('M6_8', 'M9_11', 'M12_PLUS');
create type public.texture as enum ('HALUS', 'LEMBUT', 'KASAR');
create type public.allergen as enum ('SUSU', 'TELUR', 'KACANG', 'GLUTEN', 'SEAFOOD');

-- Profil pengguna (id sama dengan id di Supabase Auth)
create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  name text not null check (char_length(trim(name)) >= 3),
  email text not null,
  role public.user_role not null default 'BUYER',
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);
create unique index profiles_email_lower_idx on public.profiles (lower(email));

-- Toko (satu penjual satu toko)
create table public.shops (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null unique references public.profiles (id) on delete cascade,
  name text not null check (char_length(trim(name)) >= 3),
  description text not null default '',
  address text not null default '',
  phone text not null default '',
  open_hours text not null default '',
  verification_status public.verification_status not null default 'PENDING',
  rejection_reason text,
  created_at timestamptz not null default now()
);
create unique index shops_name_lower_idx on public.shops (lower(name));

-- Sertifikat yang dikirim penjual (riwayat disimpan, yang terbaru dipakai)
create table public.certificates (
  id uuid primary key default gen_random_uuid(),
  shop_id uuid not null references public.shops (id) on delete cascade,
  type text not null,
  number text not null check (char_length(trim(number)) >= 8),
  issued_by text not null check (char_length(trim(issued_by)) >= 3),
  issued_date date not null,
  expired_date date not null,
  document_path text not null,
  status public.cert_status not null default 'PENDING',
  submitted_at timestamptz not null default now(),
  reviewed_at timestamptz,
  check (expired_date > issued_date)
);
create index certificates_shop_idx on public.certificates (shop_id);

-- Produk (soft delete supaya riwayat pesanan dan ulasan tetap utuh)
create table public.products (
  id uuid primary key default gen_random_uuid(),
  shop_id uuid not null references public.shops (id) on delete cascade,
  name text not null check (char_length(trim(name)) >= 3),
  price integer not null check (price > 0),
  emoji text not null,
  age_group public.age_group not null,
  texture public.texture not null,
  allergens public.allergen[] not null default '{}',
  protein integer not null check (protein between 0 and 40),
  carbs integer not null check (carbs between 0 and 80),
  fat integer not null check (fat between 0 and 40),
  fiber integer not null check (fiber between 0 and 20),
  deleted_at timestamptz,
  created_at timestamptz not null default now(),
  check (protein + carbs + fat + fiber > 0)
);
create index products_shop_idx on public.products (shop_id);

-- Ulasan (satu ulasan per pembeli per produk)
create table public.reviews (
  id uuid primary key default gen_random_uuid(),
  product_id uuid not null references public.products (id) on delete cascade,
  user_id uuid not null references public.profiles (id) on delete cascade,
  rating integer not null check (rating between 1 and 5),
  comment text not null check (char_length(trim(comment)) > 0),
  baby_age text,
  created_at timestamptz not null default now(),
  unique (product_id, user_id)
);

create table public.favorites (
  user_id uuid not null references public.profiles (id) on delete cascade,
  product_id uuid not null references public.products (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, product_id)
);

create table public.addresses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  label text not null,
  recipient text not null check (char_length(trim(recipient)) >= 3),
  full_address text not null check (char_length(trim(full_address)) >= 10),
  is_default boolean not null default false
);
create index addresses_user_idx on public.addresses (user_id);
create unique index addresses_one_default_idx on public.addresses (user_id) where is_default;

create table public.notification_prefs (
  user_id uuid primary key references public.profiles (id) on delete cascade,
  order_status boolean not null default true,
  promo boolean not null default true,
  review_reminder boolean not null default false,
  new_products boolean not null default false
);

create table public.app_ratings (
  user_id uuid primary key references public.profiles (id) on delete cascade,
  rating integer not null check (rating between 1 and 5),
  feedback text not null default '',
  updated_at timestamptz not null default now()
);

-- Pesanan: satu pesanan per toko
create table public.orders (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  buyer_id uuid not null references public.profiles (id),
  shop_id uuid not null references public.shops (id),
  status public.order_status not null default 'NEW',
  subtotal integer not null check (subtotal >= 0),
  delivery_fee integer not null check (delivery_fee >= 0),
  address text not null,
  notes text not null default '',
  payment_method text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index orders_buyer_idx on public.orders (buyer_id);
create index orders_shop_idx on public.orders (shop_id);

-- Item pesanan menyimpan nama dan harga saat dibeli
create table public.order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references public.orders (id) on delete cascade,
  product_id uuid references public.products (id) on delete set null,
  product_name text not null,
  unit_price integer not null check (unit_price > 0),
  quantity integer not null check (quantity > 0)
);
create index order_items_order_idx on public.order_items (order_id);

-- Penghitung kode pesanan harian (TS-yyyymmdd-NNN)
create table public.order_counters (
  day text primary key,
  last integer not null default 0
);

-- Kunci semua tabel
alter table public.profiles enable row level security;
alter table public.shops enable row level security;
alter table public.certificates enable row level security;
alter table public.products enable row level security;
alter table public.reviews enable row level security;
alter table public.favorites enable row level security;
alter table public.addresses enable row level security;
alter table public.notification_prefs enable row level security;
alter table public.app_ratings enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;
alter table public.order_counters enable row level security;