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

const createdUserIds = []

try {
  console.log('\nAnon (belum login)')
  {
    const anon = newAnon()
    const { data, error } = await anon.from('profiles').select('id')
    check('tidak bisa membaca profiles', !!error || (data ?? []).length === 0)
    const { data: shops, error: shopsError } = await anon.from('shops').select('id')
    check('tidak bisa membaca shops', !!shopsError || (shops ?? []).length === 0)
  }

  console.log('\nPembeli (gilbert@email.com)')
  {
    const buyer = await signIn('gilbert@email.com', SEED_DEMO_PASSWORD)
    const { data: own, error } = await buyer
      .from('profiles')
      .select('id, name, role, is_active')
    check('hanya melihat profilnya sendiri', !error && own.length === 1 && own[0].role === 'BUYER', error?.message)

    const { error: roleError } = await buyer
      .from('profiles')
      .update({ role: 'ADMIN' })
      .eq('id', own[0].id)
    check('tidak bisa mengubah perannya menjadi ADMIN', !!roleError)

    const { error: activeError } = await buyer
      .from('profiles')
      .update({ is_active: false })
      .eq('id', own[0].id)
    check('tidak bisa mengubah is_active', !!activeError)

    const { error: nameError } = await buyer
      .from('profiles')
      .update({ name: own[0].name })
      .eq('id', own[0].id)
    check('bisa mengubah namanya sendiri', !nameError, nameError?.message)

    const { data: shops } = await buyer.from('shops').select('id')
    check('belum bisa melihat toko lain (Fase 2 membukanya)', (shops ?? []).length === 0)
  }

  console.log('\nPenjual (dapurbunda@email.com)')
  {
    const seller = await signIn('dapurbunda@email.com', SEED_DEMO_PASSWORD)
    const { data, error } = await seller
      .from('profiles')
      .select('name, role, shops(name, verification_status)')
    const shop = Array.isArray(data?.[0]?.shops) ? data[0].shops[0] : data?.[0]?.shops
    check('peran SELLER', !error && data?.[0]?.role === 'SELLER', error?.message)
    check('tokonya Dapur Bunda dan APPROVED', shop?.name === 'Dapur Bunda' && shop?.verification_status === 'APPROVED')

    const { data: shops } = await seller.from('shops').select('name')
    check('hanya melihat tokonya sendiri', (shops ?? []).length === 1)
  }

  console.log('\nAdmin (admin@tinyspoon.id)')
  {
    const admin = await signIn('admin@tinyspoon.id', SEED_ADMIN_PASSWORD)
    const { count: profileCount, error } = await admin
      .from('profiles')
      .select('id', { count: 'exact', head: true })
    check('melihat semua profil (minimal 5)', !error && profileCount >= 5, error?.message)
    const { count: shopCount } = await admin
      .from('shops')
      .select('id', { count: 'exact', head: true })
    check('melihat semua toko (minimal 3)', shopCount >= 3)
  }

  console.log('\nPendaftaran')
  {
    const stamp = Date.now()
    const anon = newAnon()

    const { data: freeName } = await anon.rpc('check_shop_name', { p_name: `Toko Uji ${stamp}` })
    const { data: takenName } = await anon.rpc('check_shop_name', { p_name: 'dapur bunda' })
    check('nama toko baru tersedia', freeName === true)
    check('nama toko yang sudah ada ditolak (tanpa peduli huruf besar)', takenName === false)

    const buyerSignUp = await newAnon().auth.signUp({
      email: `uji.pembeli.${stamp}@example.com`,
      password: SEED_DEMO_PASSWORD,
      options: { data: { name: 'Uji Pembeli', role: 'ADMIN' } },
    })
    if (buyerSignUp.error) {
      check('daftar pembeli', false, buyerSignUp.error.message)
    } else {
      createdUserIds.push(buyerSignUp.data.user.id)
      const { data: profile } = await service
        .from('profiles')
        .select('role, is_active')
        .eq('id', buyerSignUp.data.user.id)
        .single()
      check('permintaan peran ADMIN saat daftar diubah menjadi BUYER', profile?.role === 'BUYER')
      if (!buyerSignUp.data.session) {
        console.log('    ! Confirm email masih aktif di dashboard (matikan agar daftar langsung login)')
      }
    }

    const sellerSignUp = await newAnon().auth.signUp({
      email: `uji.penjual.${stamp}@example.com`,
      password: SEED_DEMO_PASSWORD,
      options: { data: { name: 'Uji Penjual', role: 'SELLER', shop_name: `Toko Uji ${stamp}` } },
    })
    if (sellerSignUp.error) {
      check('daftar penjual', false, sellerSignUp.error.message)
    } else {
      createdUserIds.push(sellerSignUp.data.user.id)
      const { data: shop } = await service
        .from('shops')
        .select('name, verification_status')
        .eq('owner_id', sellerSignUp.data.user.id)
        .single()
      check(
        'penjual baru otomatis punya toko berstatus PENDING',
        shop?.name === `Toko Uji ${stamp}` && shop?.verification_status === 'PENDING'
      )
    }
  }
} catch (error) {
  failed++
  console.error(`\nTes berhenti: ${error.message}`)
} finally {
  for (const id of createdUserIds) {
    await service.auth.admin.deleteUser(id)
  }
}

console.log(`\n${passed} lulus, ${failed} gagal`)
process.exitCode = failed > 0 ? 1 : 0