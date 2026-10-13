import { createClient } from '@supabase/supabase-js'

const {
  SUPABASE_URL,
  SUPABASE_ANON_KEY,
  SUPABASE_SERVICE_ROLE_KEY,
  SEED_DEMO_PASSWORD,
  SEED_ADMIN_PASSWORD,
} = process.env

for (const [name, value] of Object.entries({
  SUPABASE_URL,
  SUPABASE_ANON_KEY,
  SUPABASE_SERVICE_ROLE_KEY,
  SEED_DEMO_PASSWORD,
  SEED_ADMIN_PASSWORD,
})) {
  if (!value) {
    console.error(`Isi ${name} di backend/.env`)
    process.exit(1)
  }
}

const options = { auth: { persistSession: false, autoRefreshToken: false } }
const newAnon = () => createClient(SUPABASE_URL, SUPABASE_ANON_KEY, options)
const service = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, options)

let passed = 0
let failed = 0
const cleanup = []
const orderIds = []

function check(name, ok, detail = '') {
  if (ok) {
    passed++
    console.log(`  ✓ ${name}`)
  } else {
    failed++
    console.log(`  ✗ ${name} ${detail}`)
  }
}

async function signIn(email, password) {
  const client = newAnon()
  const { error } = await client.auth.signInWithPassword({ email, password })
  if (error) throw new Error(`Login ${email} gagal: ${error.message}`)
  return client
}

const ADDRESS = 'Gilbert\nJl. Kemang Raya No. 10, Jakarta Selatan'

try {
  const { data: productRows } = await service
    .from('products')
    .select('id, name')
    .in('name', ['Bubur Ayam Sayuran', 'Tim Ikan Salmon', 'Nasi Lembek Ayam'])
    .is('deleted_at', null)
  const pid = Object.fromEntries(productRows.map((p) => [p.name, p.id]))

  const buyer = await signIn('gilbert@email.com', SEED_DEMO_PASSWORD)
  const dapur = await signIn('dapurbunda@email.com', SEED_DEMO_PASSWORD)
  const nutrisi = await signIn('nutrisibayi@email.com', SEED_DEMO_PASSWORD)
  const mpasi = await signIn('mpasisehat@email.com', SEED_DEMO_PASSWORD)
  const admin = await signIn('admin@tinyspoon.id', SEED_ADMIN_PASSWORD)

  console.log('\nHak membuat pesanan')
  {
    const base = {
      p_items: [{ product_id: pid['Bubur Ayam Sayuran'], quantity: 1 }],
      p_address: ADDRESS,
      p_notes: '',
      p_payment_method: 'QRIS',
    }
    const anonRes = await newAnon().rpc('place_order', base)
    check('anon tidak bisa membuat pesanan', !!anonRes.error)

    const sellerRes = await dapur.rpc('place_order', base)
    check('penjual tidak bisa membuat pesanan', !!sellerRes.error)

    const adminRes = await admin.rpc('place_order', base)
    check('admin tidak bisa membuat pesanan', !!adminRes.error)
  }

  console.log('\nValidasi pesanan')
  {
    const item = { product_id: pid['Bubur Ayam Sayuran'], quantity: 1 }
    const call = (overrides) =>
      buyer.rpc('place_order', {
        p_items: [item],
        p_address: ADDRESS,
        p_notes: '',
        p_payment_method: 'QRIS',
        ...overrides,
      })

    check('keranjang kosong ditolak', !!(await call({ p_items: [] })).error)
    check('jumlah 0 ditolak', !!(await call({ p_items: [{ ...item, quantity: 0 }] })).error)
    check('jumlah 51 ditolak', !!(await call({ p_items: [{ ...item, quantity: 51 }] })).error)
    check('alamat terlalu pendek ditolak', !!(await call({ p_address: 'hh' })).error)
    check('metode bayar tidak dikenal ditolak', !!(await call({ p_payment_method: 'Kripto' })).error)
    check(
      'produk yang tidak ada ditolak',
      !!(await call({
        p_items: [{ product_id: '00000000-0000-0000-0000-000000000000', quantity: 1 }],
      })).error
    )

    const { data: dapurShop } = await service
      .from('shops')
      .select('id')
      .eq('name', 'Dapur Bunda')
      .single()
    const { data: ghost } = await service
      .from('products')
      .insert({
        shop_id: dapurShop.id,
        name: 'Produk Terhapus Uji',
        price: 5000,
        emoji: '🍲',
        age_group: 'M6_8',
        texture: 'HALUS',
        allergens: [],
        protein: 1,
        carbs: 1,
        fat: 1,
        fiber: 1,
        deleted_at: new Date().toISOString(),
      })
      .select('id')
      .single()
    cleanup.push(() => service.from('products').delete().eq('id', ghost.id))
    check(
      'produk yang sudah dihapus ditolak',
      !!(await call({ p_items: [{ product_id: ghost.id, quantity: 1 }] })).error
    )

    const { count } = await service
      .from('orders')
      .select('id', { count: 'exact', head: true })
      .eq('buyer_name', 'Gilbert')
      .gte('created_at', new Date(Date.now() - 60_000).toISOString())
    check('pesanan yang gagal tidak meninggalkan data', count === 0)
  }

  console.log('\nMembuat pesanan (dua toko)')
  let dapurOrder
  let nutrisiOrder
  {
    const { data: placed, error } = await buyer.rpc('place_order', {
      p_items: [
        { product_id: pid['Bubur Ayam Sayuran'], quantity: 2 },
        { product_id: pid['Tim Ikan Salmon'], quantity: 1 },
        { product_id: pid['Nasi Lembek Ayam'], quantity: 1 },
      ],
      p_address: ADDRESS,
      p_notes: 'Tanpa garam tambahan',
      p_payment_method: 'QRIS',
    })
    check('berhasil membuat pesanan', !error, error?.message)
    for (const order of placed ?? []) orderIds.push(order.id)

    check('dipecah menjadi 2 pesanan (satu per toko)', placed?.length === 2)
    dapurOrder = placed?.find((o) => o.shop_name === 'Dapur Bunda')
    nutrisiOrder = placed?.find((o) => o.shop_name === 'Nutrisi Bayi')

    check(
      'Dapur Bunda: subtotal 78000, ongkir 10000, status NEW',
      dapurOrder?.subtotal === 78000 &&
        dapurOrder?.delivery_fee === 10000 &&
        dapurOrder?.status === 'NEW'
    )
    check(
      'Nutrisi Bayi: subtotal 35000, ongkir 10000',
      nutrisiOrder?.subtotal === 35000 && nutrisiOrder?.delivery_fee === 10000
    )
    check(
      'nama pembeli dan metode bayar tercatat',
      dapurOrder?.buyer_name === 'Gilbert' &&
        dapurOrder?.payment_method === 'QRIS' &&
        dapurOrder?.notes === 'Tanpa garam tambahan'
    )

    const pattern = /^TS-\d{8}-\d{3}$/
    const seqOf = (o) => Number(o.code.slice(-3))
    check(
      'kode pesanan berformat TS-yyyymmdd-NNN dan berurutan',
      pattern.test(dapurOrder?.code ?? '') &&
        pattern.test(nutrisiOrder?.code ?? '') &&
        seqOf(nutrisiOrder) === seqOf(dapurOrder) + 1
    )

    const { data: items } = await buyer
      .from('order_items')
      .select('product_name, unit_price, quantity')
      .eq('order_id', dapurOrder.id)
    check(
      'item pesanan menyimpan nama, harga, dan jumlah',
      items?.length === 2 &&
        items.reduce((sum, i) => sum + i.unit_price * i.quantity, 0) === 78000
    )
  }

  console.log('\nProduk kembar digabung')
  {
    const { data: merged, error } = await buyer.rpc('place_order', {
      p_items: [
        { product_id: pid['Bubur Ayam Sayuran'], quantity: 1 },
        { product_id: pid['Bubur Ayam Sayuran'], quantity: 2 },
      ],
      p_address: ADDRESS,
      p_notes: '',
      p_payment_method: 'Transfer Bank',
    })
    for (const order of merged ?? []) orderIds.push(order.id)
    check('berhasil', !error, error?.message)
    const { data: items } = await buyer
      .from('order_items')
      .select('quantity')
      .eq('order_id', merged?.[0]?.id)
    check('menjadi satu item dengan jumlah 3', items?.length === 1 && items[0].quantity === 3)
  }

  console.log('\nSiapa melihat pesanan apa')
  {
    const { data: mine } = await buyer
      .from('orders')
      .select('id, order_items(product_name)')
      .in('id', orderIds)
    check('pembeli melihat semua pesanannya beserta item', mine?.length === orderIds.length)

    const { data: dapurSees } = await dapur.from('orders').select('id').in('id', orderIds)
    check(
      'Dapur Bunda hanya melihat pesanan tokonya',
      dapurSees?.some((o) => o.id === dapurOrder.id) &&
        !dapurSees?.some((o) => o.id === nutrisiOrder.id)
    )

    const { data: nutrisiSees } = await nutrisi.from('orders').select('id').in('id', orderIds)
    check(
      'Nutrisi Bayi hanya melihat pesanan tokonya',
      nutrisiSees?.length === 1 && nutrisiSees[0].id === nutrisiOrder.id
    )

    const { data: mpasiSees } = await mpasi.from('orders').select('id').in('id', orderIds)
    check('MPASI Sehat tidak melihat pesanan toko lain', (mpasiSees ?? []).length === 0)

    const stamp = Date.now()
    const signUp = await newAnon().auth.signUp({
      email: `uji.pembeli2.${stamp}@example.com`,
      password: SEED_DEMO_PASSWORD,
      options: { data: { name: 'Pembeli Kedua' } },
    })
    if (signUp.error || !signUp.data.session) {
      check('daftar pembeli kedua', false, signUp.error?.message ?? 'tidak ada sesi (Confirm email aktif?)')
    } else {
      cleanup.push(() => service.auth.admin.deleteUser(signUp.data.user.id))
      const { data: others } = await signUp.data.session
        ? await createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
            ...options,
            global: { headers: { Authorization: `Bearer ${signUp.data.session.access_token}` } },
          })
            .from('orders')
            .select('id')
        : { data: [] }
      check('pembeli lain tidak melihat pesanan Gilbert', (others ?? []).length === 0)
    }

    const { count } = await admin.from('orders').select('id', { count: 'exact', head: true })
    check('admin melihat semua pesanan', count >= orderIds.length)
  }

  console.log('\nTulis langsung dilarang')
  {
    const { error: insertError } = await buyer.from('orders').insert({
      code: 'TS-00000000-001',
      buyer_id: dapurOrder.buyer_id,
      shop_id: dapurOrder.shop_id,
      subtotal: 1,
      delivery_fee: 0,
      address: ADDRESS,
      payment_method: 'QRIS',
    })
    check('pembeli tidak bisa insert pesanan langsung', !!insertError)

    const { data: updated, error: updateError } = await buyer
      .from('orders')
      .update({ status: 'DONE' })
      .eq('id', dapurOrder.id)
      .select()
    check('pembeli tidak bisa mengubah status langsung', !!updateError || (updated ?? []).length === 0)

    const { error: sellerUpdateError } = await dapur
      .from('orders')
      .update({ subtotal: 1 })
      .eq('id', dapurOrder.id)
    check('penjual tidak bisa mengubah isi pesanan langsung', !!sellerUpdateError)
  }

  console.log('\nAlur status pesanan')
  {
    const set = (client, id, status) =>
      client.rpc('set_order_status', { p_order_id: id, p_status: status })

    check('pembeli tidak bisa mengubah status', !!(await set(buyer, dapurOrder.id, 'COOKING')).error)
    check(
      'penjual toko lain tidak bisa mengubah status',
      !!(await set(nutrisi, dapurOrder.id, 'COOKING')).error
    )
    check('NEW → SHIPPING ditolak (loncat)', !!(await set(dapur, dapurOrder.id, 'SHIPPING')).error)
    check('NEW → DONE ditolak (loncat)', !!(await set(dapur, dapurOrder.id, 'DONE')).error)

    const step1 = await set(dapur, dapurOrder.id, 'COOKING')
    check('NEW → COOKING', step1.data?.status === 'COOKING', step1.error?.message)
    const step2 = await set(dapur, dapurOrder.id, 'SHIPPING')
    check('COOKING → SHIPPING', step2.data?.status === 'SHIPPING', step2.error?.message)
    check('SHIPPING → REJECTED ditolak', !!(await set(dapur, dapurOrder.id, 'REJECTED')).error)
    const step3 = await set(dapur, dapurOrder.id, 'DONE')
    check('SHIPPING → DONE', step3.data?.status === 'DONE', step3.error?.message)
    check('DONE tidak bisa diubah lagi', !!(await set(dapur, dapurOrder.id, 'COOKING')).error)

    const { data: seenByBuyer } = await buyer
      .from('orders')
      .select('status')
      .eq('id', dapurOrder.id)
      .single()
    check('pembeli melihat status DONE', seenByBuyer?.status === 'DONE')

    const rejected = await set(nutrisi, nutrisiOrder.id, 'REJECTED')
    check('NEW → REJECTED (ditolak penjual)', rejected.data?.status === 'REJECTED', rejected.error?.message)
    check('REJECTED tidak bisa dilanjutkan', !!(await set(nutrisi, nutrisiOrder.id, 'COOKING')).error)
  }

  console.log('\nAkun pembeli nonaktif')
  {
    const { data: profile } = await service
      .from('profiles')
      .select('id')
      .eq('email', 'gilbert@email.com')
      .single()
    cleanup.push(() => service.from('profiles').update({ is_active: true }).eq('id', profile.id))

    await service.from('profiles').update({ is_active: false }).eq('id', profile.id)
    const { error } = await buyer.rpc('place_order', {
      p_items: [{ product_id: pid['Bubur Ayam Sayuran'], quantity: 1 }],
      p_address: ADDRESS,
      p_notes: '',
      p_payment_method: 'QRIS',
    })
    check('pembeli nonaktif tidak bisa memesan', !!error)
    const { data: seen } = await buyer.from('orders').select('id')
    check('pembeli nonaktif tidak bisa membaca pesanan', (seen ?? []).length === 0)
    await service.from('profiles').update({ is_active: true }).eq('id', profile.id)
  }
} catch (error) {
  failed++
  console.error(`\nTes berhenti: ${error.message}`)
} finally {
  if (orderIds.length > 0) {
    await service.from('orders').delete().in('id', orderIds)
  }
  for (const fn of cleanup) {
    await fn()
  }
}

console.log(`\n${passed} lulus, ${failed} gagal`)
process.exitCode = failed > 0 ? 1 : 0