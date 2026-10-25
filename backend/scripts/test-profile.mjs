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

const LONG_ADDRESS = 'Jl. Kemang Raya No. 10, Jakarta Selatan'

try {
  const { data: gilbert } = await service
    .from('profiles')
    .select('id')
    .eq('email', 'gilbert@email.com')
    .single()

  async function resetGilbert() {
    await service.from('reviews').delete().eq('user_id', gilbert.id)
    await service.from('favorites').delete().eq('user_id', gilbert.id)
    await service.from('addresses').delete().eq('user_id', gilbert.id)
    await service.from('app_ratings').delete().eq('user_id', gilbert.id)
    await service
      .from('notification_prefs')
      .update({ order_status: true, promo: true, review_reminder: false, new_products: false })
      .eq('user_id', gilbert.id)
  }
  await resetGilbert()
  cleanup.push(resetGilbert)

  const { data: productRows } = await service
    .from('products')
    .select('id, name')
    .in('name', ['Bubur Ayam Sayuran', 'Tim Ikan Salmon', 'Puree Wortel Kentang'])
    .is('deleted_at', null)
  const pid = Object.fromEntries(productRows.map((p) => [p.name, p.id]))

  const buyer = await signIn('gilbert@email.com', SEED_DEMO_PASSWORD)
  const seller = await signIn('dapurbunda@email.com', SEED_DEMO_PASSWORD)

  const stamp = Date.now()
  const other = newAnon()
  const signUp = await other.auth.signUp({
    email: `uji.pembeli3.${stamp}@example.com`,
    password: SEED_DEMO_PASSWORD,
    options: { data: { name: 'Pembeli Uji' } },
  })
  if (signUp.error || !signUp.data.session) {
    throw new Error(
      `Daftar pembeli uji gagal: ${signUp.error?.message ?? 'tidak ada sesi (Confirm email masih aktif?)'}`
    )
  }
  const otherId = signUp.data.user.id
  cleanup.push(() => service.auth.admin.deleteUser(otherId))

  const stats = async (productId) => {
    const { data } = await buyer
      .from('products_with_stats')
      .select('rating, review_count')
      .eq('id', productId)
      .single()
    return data
  }

  console.log('\nUlasan')
  {
    const base = { p_product_id: pid['Bubur Ayam Sayuran'], p_rating: 5, p_comment: 'Enak sekali' }

    check('anon tidak bisa menulis ulasan', !!(await newAnon().rpc('submit_review', base)).error)
    check('penjual tidak bisa menulis ulasan', !!(await seller.rpc('submit_review', base)).error)
    check('rating 0 ditolak', !!(await buyer.rpc('submit_review', { ...base, p_rating: 0 })).error)
    check('rating 6 ditolak', !!(await buyer.rpc('submit_review', { ...base, p_rating: 6 })).error)
    check('komentar kosong ditolak', !!(await buyer.rpc('submit_review', { ...base, p_comment: '   ' })).error)
    check(
      'komentar lebih dari 500 karakter ditolak',
      !!(await buyer.rpc('submit_review', { ...base, p_comment: 'a'.repeat(501) })).error
    )

    const before = await stats(pid['Bubur Ayam Sayuran'])
    check('awal: rating 4.8 dari 5 ulasan contoh', before?.rating === 4.8 && before?.review_count === 5)

    const first = await buyer.rpc('submit_review', { ...base, p_rating: 1, p_comment: 'Terlalu lama' })
    check('pembeli bisa menulis ulasan', !first.error, first.error?.message)
    check('nama penulis tersimpan', first.data?.author_name === 'Gilbert')
    const afterFirst = await stats(pid['Bubur Ayam Sayuran'])
    check('rating jadi 4.2 dari 6 ulasan', afterFirst?.rating === 4.2 && afterFirst?.review_count === 6)

    const second = await buyer.rpc('submit_review', { ...base, p_rating: 5, p_comment: 'Wangi sekali' })
    check('ulasan kedua menggantikan yang pertama', !second.error, second.error?.message)
    const afterSecond = await stats(pid['Bubur Ayam Sayuran'])
    check('jumlah tetap 6 dan rating jadi 4.8', afterSecond?.rating === 4.8 && afterSecond?.review_count === 6)

    const { data: seenByOther } = await other
      .from('reviews')
      .select('author_name, rating, comment')
      .eq('product_id', pid['Bubur Ayam Sayuran'])
      .eq('author_name', 'Gilbert')
    check(
      'pembeli lain melihat ulasan beserta nama penulis',
      seenByOther?.length === 1 && seenByOther[0].comment === 'Wangi sekali'
    )

    check(
      'pembeli tidak bisa mengubah ulasan langsung',
      !!(await buyer.from('reviews').update({ rating: 1 }).eq('product_id', pid['Bubur Ayam Sayuran'])).error
    )
    check(
      'pembeli tidak bisa menghapus ulasan langsung',
      !!(await buyer.from('reviews').delete().eq('product_id', pid['Bubur Ayam Sayuran'])).error
    )
  }

  console.log('\nFavorit')
  {
    const add = (client, productId) => client.from('favorites').insert({ product_id: productId })

    check('anon tidak bisa menambah favorit', !!(await add(newAnon(), pid['Tim Ikan Salmon'])).error)
    check('penjual tidak bisa menambah favorit', !!(await add(seller, pid['Tim Ikan Salmon'])).error)

    const first = await add(buyer, pid['Tim Ikan Salmon'])
    check('pembeli bisa menambah favorit', !first.error, first.error?.message)
    check('favorit ganda ditolak', !!(await add(buyer, pid['Tim Ikan Salmon'])).error)

    const spoof = await buyer
      .from('favorites')
      .insert({ product_id: pid['Bubur Ayam Sayuran'], user_id: otherId })
    check('tidak bisa memalsukan pemilik favorit', !!spoof.error)

    const { data: mine } = await buyer.from('favorites').select('product_id')
    check('pembeli melihat favoritnya (1)', mine?.length === 1)
    const { data: others } = await other.from('favorites').select('product_id')
    check('pembeli lain tidak melihat favorit Gilbert', (others ?? []).length === 0)

    const removal = await buyer.from('favorites').delete().eq('product_id', pid['Tim Ikan Salmon'])
    const { data: afterRemoval } = await buyer.from('favorites').select('product_id')
    check('bisa menghapus favorit', !removal.error && (afterRemoval ?? []).length === 0)
  }

  console.log('\nProduk tersembunyi')
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
    const review = await buyer.rpc('submit_review', {
      p_product_id: pid['Puree Wortel Kentang'],
      p_rating: 5,
      p_comment: 'Enak',
    })
    check('tidak bisa mengulas produk toko nonaktif', !!review.error)
    const favorite = await buyer.from('favorites').insert({ product_id: pid['Puree Wortel Kentang'] })
    check('tidak bisa memfavoritkan produk toko nonaktif', !!favorite.error)
    await service.from('profiles').update({ is_active: true }).eq('id', owner.owner_id)
  }

  console.log('\nAlamat')
  {
    const save = (client, args) =>
      client.rpc('save_address', {
        p_id: null,
        p_label: 'Rumah',
        p_recipient: 'Gilbert',
        p_full_address: LONG_ADDRESS,
        p_make_default: false,
        ...args,
      })
    const listAddresses = async () => {
      const { data } = await buyer
        .from('addresses')
        .select('id, label, recipient, is_default')
        .order('created_at')
      return data ?? []
    }

    check('penjual tidak bisa menyimpan alamat', !!(await save(seller, {})).error)
    check('label tidak dikenal ditolak', !!(await save(buyer, { p_label: 'Kos' })).error)
    check('nama penerima terlalu pendek ditolak', !!(await save(buyer, { p_recipient: 'Gi' })).error)
    check('alamat terlalu pendek ditolak', !!(await save(buyer, { p_full_address: 'hh' })).error)

    const first = await save(buyer, {})
    check('alamat pertama otomatis jadi utama', first.data?.is_default === true, first.error?.message)

    const second = await save(buyer, { p_label: 'Kantor', p_recipient: 'Santo', p_make_default: false })
    check('alamat kedua tidak jadi utama', second.data?.is_default === false, second.error?.message)

    const third = await save(buyer, { p_label: 'Lainnya', p_make_default: true })
    let rows = await listAddresses()
    check(
      'alamat dengan "jadikan utama" menggantikan yang lama (tepat 1 utama)',
      third.data?.is_default === true && rows.filter((a) => a.is_default).length === 1
    )

    const switched = await buyer.rpc('set_default_address', { p_id: first.data.id })
    rows = await listAddresses()
    check(
      'set_default_address memindahkan status utama',
      !switched.error &&
        rows.find((a) => a.is_default)?.id === first.data.id &&
        rows.filter((a) => a.is_default).length === 1,
      switched.error?.message
    )

    const edited = await save(buyer, {
      p_id: second.data.id,
      p_label: 'Kantor',
      p_recipient: 'Santo Baru',
      p_make_default: false,
    })
    check(
      'mengubah alamat tidak mengubah status utama',
      !edited.error && edited.data?.recipient === 'Santo Baru' && edited.data?.is_default === false,
      edited.error?.message
    )

    const { data: seenByOther } = await other.from('addresses').select('id')
    check('pembeli lain tidak melihat alamat Gilbert', (seenByOther ?? []).length === 0)
    check(
      'pembeli lain tidak bisa menjadikan utama alamat Gilbert',
      !!(await other.rpc('set_default_address', { p_id: first.data.id })).error
    )
    check(
      'pembeli lain tidak bisa menghapus alamat Gilbert',
      !!(await other.rpc('delete_address', { p_id: first.data.id })).error
    )
    check(
      'pembeli lain tidak bisa mengubah alamat Gilbert',
      !!(await save(other, { p_id: first.data.id })).error
    )

    const removed = await buyer.rpc('delete_address', { p_id: first.data.id })
    rows = await listAddresses()
    check(
      'menghapus alamat utama memindahkan status ke alamat lain',
      !removed.error && rows.length === 2 && rows.filter((a) => a.is_default).length === 1,
      removed.error?.message
    )

    for (let i = rows.length; i < 10; i++) {
      await save(buyer, { p_label: 'Lainnya' })
    }
    check('batas 10 alamat ditolak di alamat ke-11', !!(await save(buyer, {})).error)
  }

  console.log('\nPreferensi notifikasi')
  {
    const { data: own } = await buyer.from('notification_prefs').select('*')
    check('pembeli hanya melihat barisnya sendiri', own?.length === 1 && own[0].user_id === gilbert.id)

    const update = await buyer
      .from('notification_prefs')
      .update({ promo: false, new_products: true })
      .eq('user_id', gilbert.id)
      .select()
    check(
      'bisa mengubah saklar notifikasi',
      !update.error && update.data?.[0]?.promo === false && update.data?.[0]?.new_products === true,
      update.error?.message
    )

    const spoof = await buyer
      .from('notification_prefs')
      .update({ user_id: otherId })
      .eq('user_id', gilbert.id)
    check('tidak bisa memindahkan baris ke pengguna lain', !!spoof.error)

    const { data: foreign } = await other
      .from('notification_prefs')
      .update({ promo: false })
      .eq('user_id', gilbert.id)
      .select()
    check('pembeli lain tidak bisa mengubah preferensi Gilbert', (foreign ?? []).length === 0)
  }

  console.log('\nRating aplikasi')
  {
    check('rating 0 ditolak', !!(await buyer.rpc('submit_app_rating', { p_rating: 0 })).error)
    check('rating 6 ditolak', !!(await buyer.rpc('submit_app_rating', { p_rating: 6 })).error)
    check('penjual tidak bisa memberi rating', !!(await seller.rpc('submit_app_rating', { p_rating: 5 })).error)

    const first = await buyer.rpc('submit_app_rating', { p_rating: 4, p_feedback: 'Bagus' })
    check('pembeli bisa memberi rating 4', first.data?.rating === 4, first.error?.message)
    const second = await buyer.rpc('submit_app_rating', { p_rating: 5, p_feedback: '' })
    check('rating kedua menggantikan yang pertama', second.data?.rating === 5, second.error?.message)

    const { data: mine } = await buyer.from('app_ratings').select('rating')
    check('hanya ada satu rating milik Gilbert (5)', mine?.length === 1 && mine[0].rating === 5)
    const { data: seenByOther } = await other.from('app_ratings').select('rating')
    check('pembeli lain tidak melihat rating Gilbert', (seenByOther ?? []).length === 0)
    check(
      'tidak bisa menghapus rating langsung',
      !!(await buyer.from('app_ratings').delete().eq('user_id', gilbert.id)).error
    )
  }
} catch (error) {
  failed++
  console.error(`\nTes berhenti: ${error.message}`)
} finally {
  for (const fn of cleanup.reverse()) {
    await fn()
  }
}

console.log(`\n${passed} lulus, ${failed} gagal`)
process.exitCode = failed > 0 ? 1 : 0