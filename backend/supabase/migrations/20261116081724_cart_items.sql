-- Fase 8: keranjang pembeli disimpan di server

create table public.cart_items (
  user_id uuid not null references public.profiles (id) on delete cascade,
  product_id uuid not null references public.products (id) on delete cascade,
  quantity integer not null check (quantity between 1 and 50),
  primary key (user_id, product_id)
);

alter table public.cart_items enable row level security;

grant select, insert, update, delete on public.cart_items to authenticated;

create policy "cart_select" on public.cart_items
  for select to authenticated
  using (user_id = auth.uid() and public.app_role() = 'BUYER');

create policy "cart_insert" on public.cart_items
  for insert to authenticated
  with check (user_id = auth.uid() and public.app_role() = 'BUYER');

create policy "cart_update" on public.cart_items
  for update to authenticated
  using (user_id = auth.uid() and public.app_role() = 'BUYER')
  with check (user_id = auth.uid() and public.app_role() = 'BUYER');

create policy "cart_delete" on public.cart_items
  for delete to authenticated
  using (user_id = auth.uid() and public.app_role() = 'BUYER');