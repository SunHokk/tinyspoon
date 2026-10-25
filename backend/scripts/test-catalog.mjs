import { createClient } from '@supabase/supabase-js'

const {
  SUPABASE_URL,
  SUPABASE_ANON_KEY,
  SUPABASE_SERVICE_ROLE_KEY,
  SEED_DEMO_PASSWORD,
} = process.env

for (const [name, value] of Object.entries({
  SUPABASE_URL,
  SUPABASE_ANON_KEY,
  SUPABASE_SERVICE_ROLE_KEY,
  SEED_DEMO_PASSWORD,
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

try {
  console.log('\nAnon (belum login)')
  {
    const { data, error } = await newAnon().from('products_with_stats').select('id')
    check('tidak bisa membaca produk', !!error || (data ?? []).length === 0)
  }

  const buyer = await signIn('gilbert@email.com', SEED_DEMO_PASSWORD)

  console.log('\nPembeli (gilbert@email.com)')
  {
    const { data: products, error } = await buyer
      .from('products_with_stats')
      .select('*')
      .order('name')
    check('melihat 8 produk toko terverifikasi', !error && products.length === 8, error?.message)

    const salmon = products?.find((p) => p.name === 'Tim Ikan Salmon')
    check(
      'data produk benar (toko, harga, gizi, alergen)',
      salmon?.shop_name === 'Nutrisi Bayi' &&
        salmon?.price === 35000 &&
        salmon?.protein === 12 &&
        salmon?.allergens?.includes('SEAFOOD')
    )
    check('rating 5 dari 5 ulasan contoh', salmon?.rating === 5 && salmon?.review_count === 5)

    const { error: insertError } = await buyer.from('products').insert({
      shop_id: salmon.shop_id,
      name: 'Produk Palsu',
      price: 1000,
      emoji: '🍚',
      age_group: 'M6_8',
      texture: 'HALUS',
      allergens: [],
      protein: 1,
      carbs: 1,
      fat: 1,
      fiber: 1,
    })
    check('tidak bisa menambah produk', !!insertError)

    const { data: updated } = await buyer
      .from('products')
      .update({ price: 1 })
      .eq('id', salmon.id)
      .select()
    check('tidak bisa mengubah produk', (updated ?? []).length === 0)

    const { data: certs } = await buyer.from('certificates').select('number, status')
    check(
      'hanya melihat sertifikat aktif dari 3 toko',
      (certs ?? []).length === 3 && certs.every((c) => c.status === 'ACTIVE')
    )

    const { data: profiles } = await buyer.from('profiles').select('id')
    check('tetap hanya melihat profilnya sendiri', (profiles ?? []).length === 1)
  }

  console.log('\nPenjual (dapurbunda@email.com)')
  {
    const seller = await signIn('dapurbunda@email.com', SEED_DEMO_PASSWORD)

    const { data: mine } = await seller
      .from('products_with_stats')
      .select('id, name')
      .eq('shop_name', 'Dapur Bunda')
    check('melihat 3 produk tokonya', mine?.length === 3)

    const { data: shops } = await seller.from('shops').select('id, name')
    const ownShop = shops.find((s) => s.name === 'Dapur Bunda')
    const otherShop = shops.find((s) => s.name === 'MPASI Sehat')

    const base = {
      name: 'Produk Uji',
      price: 10000,
      emoji: '🍲',
      age_group: 'M9_11',
      texture: 'LEMBUT',
      allergens: ['TELUR'],
      protein: 5,
      carbs: 10,
      fat: 2,
      fiber: 1,
    }

    const { data: created, error: createError } = await seller
      .from('products')
      .insert({ ...base, shop_id: ownShop.id })
      .select('id')
      .single()
    check('bisa menambah produk di tokonya', !createError && !!created?.id, createError?.message)
    if (created?.id) {
      cleanup.push(() => service.from('products').delete().eq('id', created.id))
    }

    const { error: foreignError } = await seller
      .from('products')
      .insert({ ...base, shop_id: otherShop.id })
    check('tidak bisa menambah produk di toko lain', !!foreignError)

    const { error: fiberError } = await seller
      .from('products')
      .insert({ ...base, shop_id: ownShop.id, fiber: 100 })
    check('serat 100g ditolak (batas 20g)', !!fiberError)

    const { error: zeroError } = await seller
      .from('products')
      .insert({ ...base, shop_id: ownShop.id, protein: 0, carbs: 0, fat: 0, fiber: 0 })
    check('gizi semuanya 0 ditolak', !!zeroError)

    const { data: edited } = await seller
      .from('products')
      .update({ price: 12345 })
      .eq('id', created.id)
      .select('price')
    check('bisa mengubah produknya', edited?.[0]?.price === 12345)

    const { data: salmonRow } = await service
      .from('products')
      .select('id')
      .eq('name', 'Tim Ikan Salmon')
      .single()
    const { data: foreignEdit } = await seller
      .from('products')
      .update({ price: 1 })
      .eq('id', salmonRow.id)
      .select()
    check('tidak bisa mengubah produk toko lain', (foreignEdit ?? []).length === 0)

    const { error: softError } = await seller
      .from('products')
      .update({ deleted_at: new Date().toISOString() })
      .eq('id', created.id)
    check('bisa menghapus (soft delete) produknya', !softError, softError?.message)

    const { data: seenByBuyer } = await buyer
      .from('products_with_stats')
      .select('id')
      .eq('id', created.id)
    check('produk yang dihapus tidak terlihat pembeli', (seenByBuyer ?? []).length === 0)

    const { error: hardDeleteError } = await seller
      .from('products')
      .delete()
      .eq('id', created.id)
    check('hapus permanen dilarang', !!hardDeleteError)

    const { error: shopEditError } = await seller
      .from('shops')
      .update({ description: ownShop.description ?? 'Dapur rumahan MPASI' })
      .eq('id', ownShop.id)
    check('bisa mengubah profil tokonya', !shopEditError, shopEditError?.message)

    const { error: statusError } = await seller
      .from('shops')
      .update({ verification_status: 'REJECTED' })
      .eq('id', ownShop.id)
    check('tidak bisa mengubah status verifikasi', !!statusError)

    const { error: nameError } = await seller
      .from('shops')
      .update({ name: 'Nama Baru' })
      .eq('id', ownShop.id)
    check('tidak bisa mengubah nama toko', !!nameError)

    const { data: otherEdit } = await seller
      .from('shops')
      .update({ description: 'diubah paksa' })
      .eq('id', otherShop.id)
      .select()
    check('tidak bisa mengubah toko lain', (otherEdit ?? []).length === 0)
  }

  console.log('\nVisibilitas toko')
  {
    const { data: owner } = await service
      .from('shops')
      .select('owner_id')
      .eq('name', 'MPASI Sehat')
      .single()
    cleanup.push(() =>
      service.from('profiles').update({ is_active: true }).eq('id', owner.owner_id)
    )

    await service.from('profiles').update({ is_active: false }).eq('id', owner.owner_id)
    const { data: visible } = await buyer.from('products_with_stats').select('id')
    check('produk penjual nonaktif hilang dari pembeli (5 tersisa)', visible?.length === 5)
    const { data: shopsSeen } = await buyer.from('shops').select('id')
    check('tokonya juga hilang (2 toko)', shopsSeen?.length === 2)

    await service.from('profiles').update({ is_active: true }).eq('id', owner.owner_id)
    const { data: back } = await buyer.from('products_with_stats').select('id')
    check('diaktifkan lagi: 8 produk kembali', back?.length === 8)

    const stamp = Date.now()
    const signUp = await newAnon().auth.signUp({
      email: `uji.toko.${stamp}@example.com`,
      password: SEED_DEMO_PASSWORD,
      options: { data: { name: 'Uji Toko', role: 'SELLER', shop_name: `Toko Menunggu ${stamp}` } },
    })
    if (signUp.error) {
      check('daftar penjual uji', false, signUp.error.message)
    } else {
      cleanup.push(() => service.auth.admin.deleteUser(signUp.data.user.id))
      const { data: pending } = await buyer
        .from('shops')
        .select('id')
        .eq('name', `Toko Menunggu ${stamp}`)
      check('toko berstatus PENDING tidak terlihat pembeli', (pending ?? []).length === 0)
    }
  }
} catch (error) {
  failed++
  console.error(`\nTes berhenti: ${error.message}`)
} finally {
  for (const fn of cleanup) {
    await fn()
  }
}

console.log(`\n${passed} lulus, ${failed} gagal`)
process.exitCode = failed > 0 ? 1 : 0