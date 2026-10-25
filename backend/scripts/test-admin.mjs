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
const uploaded = []

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

const day = (offset) => new Date(Date.now() + offset * 86_400_000).toISOString().slice(0, 10)
const pdf = () => new Blob(['%PDF-1.4 berkas uji'], { type: 'application/pdf' })
const upload = (client, path, body = pdf(), contentType = 'application/pdf') =>
  client.storage.from('certificates').upload(path, body, { contentType })
const signed = (client, path) => client.storage.from('certificates').createSignedUrl(path, 60)

try {
  const buyer = await signIn('gilbert@email.com', SEED_DEMO_PASSWORD)
  const dapur = await signIn('dapurbunda@email.com', SEED_DEMO_PASSWORD)
  const mpasi = await signIn('mpasisehat@email.com', SEED_DEMO_PASSWORD)
  const admin = await signIn('admin@tinyspoon.id', SEED_ADMIN_PASSWORD)

  const { data: demoShops } = await service
    .from('shops')
    .select('id, name')
    .in('name', ['Dapur Bunda'])
  const dapurShopId = demoShops[0].id
  const { data: adminProfile } = await service
    .from('profiles')
    .select('id')
    .eq('email', 'admin@tinyspoon.id')
    .single()

  const stamp = Date.now()
  const shopName = `Toko Verifikasi ${stamp}`
  const email = `uji.verifikasi.${stamp}@example.com`
  const signUp = await newAnon().auth.signUp({
    email,
    password: SEED_DEMO_PASSWORD,
    options: { data: { name: 'Penjual Uji', role: 'SELLER', shop_name: shopName } },
  })
  if (signUp.error || !signUp.data.session) {
    throw new Error(
      `Daftar penjual uji gagal: ${signUp.error?.message ?? 'tidak ada sesi (Confirm email masih aktif?)'}`
    )
  }
  const sellerId = signUp.data.user.id
  cleanup.push(() => service.auth.admin.deleteUser(sellerId))
  cleanup.push(async () => {
    if (uploaded.length > 0) await service.storage.from('certificates').remove(uploaded)
  })

  const seller = await signIn(email, SEED_DEMO_PASSWORD)
  const { data: shopRow } = await service.from('shops').select('id').eq('owner_id', sellerId).single()
  const shopId = shopRow.id
  const doc1 = `${shopId}/${stamp}_sertifikat.pdf`
  const doc2 = `${shopId}/${stamp}_sertifikat_revisi.pdf`

  const certArgs = (overrides = {}) => ({
    p_type: 'Sertifikat Produksi Pangan Industri Rumah Tangga',
    p_number: 'P-IRT-1234567890',
    p_issued_by: 'Dinas Kesehatan Kota Jakarta Selatan',
    p_issued_date: day(-400),
    p_expired_date: day(400),
    p_document_path: doc1,
    ...overrides,
  })
  const submit = (client, overrides) => client.rpc('submit_certificate', certArgs(overrides))

  console.log('\nPenyimpanan dokumen')
  {
    check('anon tidak bisa mengunggah', !!(await upload(newAnon(), `${shopId}/anon_${stamp}.pdf`)).error)
    check('pembeli tidak bisa mengunggah', !!(await upload(buyer, `${shopId}/buyer_${stamp}.pdf`)).error)
    check(
      'penjual tidak bisa mengunggah ke folder toko lain',
      !!(await upload(seller, `${dapurShopId}/uji_${stamp}.pdf`)).error
    )
    check(
      'format file yang tidak diizinkan ditolak (txt)',
      !!(await upload(seller, `${shopId}/uji_${stamp}.txt`, new Blob(['uji'], { type: 'text/plain' }), 'text/plain')).error
    )

    const first = await upload(seller, doc1)
    if (!first.error) uploaded.push(doc1)
    check('penjual bisa mengunggah ke folder tokonya', !first.error, first.error?.message)

    check('penjual bisa membuka dokumennya sendiri', !(await signed(seller, doc1)).error)
    check('penjual toko lain tidak bisa membuka dokumen itu', !!(await signed(mpasi, doc1)).error)
    check('pembeli tidak bisa membuka dokumen itu', !!(await signed(buyer, doc1)).error)
    check('admin bisa membuka dokumen', !(await signed(admin, doc1)).error)
  }

  console.log('\nKirim sertifikat')
  {
    check('anon tidak bisa mengirim', !!(await submit(newAnon(), {})).error)
    check('pembeli tidak bisa mengirim', !!(await submit(buyer, {})).error)
    check('penjual yang sudah terverifikasi tidak bisa mengirim', !!(await submit(dapur, {})).error)

    check('jenis sertifikat tidak dikenal ditolak', !!(await submit(seller, { p_type: 'Sertifikat Palsu' })).error)
    check('nomor terlalu pendek ditolak', !!(await submit(seller, { p_number: 'P-IRT' })).error)
    check('penerbit terlalu pendek ditolak', !!(await submit(seller, { p_issued_by: 'ab' })).error)
    check('tanggal terbit di masa depan ditolak', !!(await submit(seller, { p_issued_date: day(30) })).error)
    check(
      'berlaku hingga sebelum tanggal terbit ditolak',
      !!(await submit(seller, { p_issued_date: day(-10), p_expired_date: day(-20) })).error
    )
    check(
      'sertifikat yang sudah kedaluwarsa ditolak',
      !!(await submit(seller, { p_issued_date: day(-400), p_expired_date: day(-5) })).error
    )
    check(
      'dokumen di folder toko lain ditolak',
      !!(await submit(seller, { p_document_path: `${dapurShopId}/sertifikat.pdf` })).error
    )
    check(
      'dokumen yang belum diunggah ditolak',
      !!(await submit(seller, { p_document_path: `${shopId}/tidak-ada.pdf` })).error
    )

    const first = await submit(seller, {})
    check(
      'berhasil mengirim sertifikat (status PENDING)',
      !first.error && first.data?.status === 'PENDING',
      first.error?.message
    )

    const { data: shopNow } = await seller
      .from('shops')
      .select('verification_status, rejection_reason')
      .eq('id', shopId)
      .single()
    check('toko tetap berstatus PENDING', shopNow?.verification_status === 'PENDING')
    check('pengajuan kedua saat masih diperiksa ditolak', !!(await submit(seller, {})).error)

    const { data: ownCerts } = await seller.from('certificates').select('id').eq('shop_id', shopId)
    const { data: buyerCerts } = await buyer.from('certificates').select('id').eq('shop_id', shopId)
    check('penjual melihat sertifikat yang dikirimnya', ownCerts?.length === 1)
    check('pembeli tidak melihat sertifikat yang belum disetujui', (buyerCerts ?? []).length === 0)

    const { error: productError } = await seller.from('products').insert({
      shop_id: shopId,
      name: 'Produk Sebelum Disetujui',
      price: 10000,
      emoji: '🍲',
      age_group: 'M6_8',
      texture: 'HALUS',
      allergens: [],
      protein: 3,
      carbs: 12,
      fat: 2,
      fiber: 1,
    })
    check('penjual belum terverifikasi tidak bisa menambah produk', !!productError)

    const { data: pending, error: pendingError } = await admin
      .from('shops')
      .select('id, name, profiles(name, email), certificates(number, status)')
      .eq('verification_status', 'PENDING')
    const mine = pending?.find((s) => s.id === shopId)
    check(
      'admin melihat pengajuan beserta pemilik dan sertifikat',
      !pendingError && mine?.profiles?.email === email && mine?.certificates?.length === 1,
      pendingError?.message
    )
  }

  console.log('\nVerifikasi oleh admin')
  {
    const review = (client, approve, reason) =>
      client.rpc('review_shop', { p_shop_id: shopId, p_approve: approve, p_reason: reason ?? null })

    check('pembeli tidak bisa memverifikasi', !!(await review(buyer, true)).error)
    check('penjual tidak bisa memverifikasi', !!(await review(dapur, true)).error)
    check('anon tidak bisa memverifikasi', !!(await review(newAnon(), true)).error)
    check('menolak tanpa alasan ditolak', !!(await review(admin, false)).error)
    check('alasan penolakan terlalu pendek ditolak', !!(await review(admin, false, 'ab')).error)

    const rejected = await review(admin, false, 'Foto sertifikat tidak jelas')
    check(
      'admin bisa menolak dengan alasan',
      !rejected.error &&
        rejected.data?.verification_status === 'REJECTED' &&
        rejected.data?.rejection_reason === 'Foto sertifikat tidak jelas',
      rejected.error?.message
    )

    const { data: rejectedCert } = await service
      .from('certificates')
      .select('status, reviewed_at')
      .eq('shop_id', shopId)
      .single()
    check('sertifikat ditandai REJECTED dan ada waktu pemeriksaan', rejectedCert?.status === 'REJECTED' && !!rejectedCert?.reviewed_at)

    const { data: ownShop } = await seller
      .from('shops')
      .select('verification_status, rejection_reason')
      .eq('id', shopId)
      .single()
    check(
      'penjual melihat status ditolak beserta alasan',
      ownShop?.verification_status === 'REJECTED' && ownShop?.rejection_reason === 'Foto sertifikat tidak jelas'
    )
    check('toko yang sudah ditolak tidak bisa ditinjau ulang tanpa kirim ulang', !!(await review(admin, true)).error)

    const second = await upload(seller, doc2)
    if (!second.error) uploaded.push(doc2)
    check('penjual mengunggah dokumen revisi', !second.error, second.error?.message)

    const resubmit = await submit(seller, { p_document_path: doc2, p_number: 'P-IRT-0987654321' })
    check('penjual bisa mengirim ulang setelah ditolak', !resubmit.error, resubmit.error?.message)

    const { data: afterResubmit } = await seller
      .from('shops')
      .select('verification_status, rejection_reason')
      .eq('id', shopId)
      .single()
    check(
      'status kembali PENDING dan alasan lama dihapus',
      afterResubmit?.verification_status === 'PENDING' && afterResubmit?.rejection_reason === null
    )

    const approved = await review(admin, true)
    check(
      'admin bisa menyetujui',
      !approved.error && approved.data?.verification_status === 'APPROVED',
      approved.error?.message
    )

    const { data: activeCert } = await service
      .from('certificates')
      .select('status, number')
      .eq('shop_id', shopId)
      .eq('status', 'ACTIVE')
      .single()
    check('sertifikat terbaru menjadi ACTIVE', activeCert?.number === 'P-IRT-0987654321')

    check('menyetujui dua kali ditolak', !!(await review(admin, true)).error)
    check('toko yang sudah terverifikasi tidak bisa mengirim sertifikat lagi', !!(await submit(seller, {})).error)

    const { data: seenShop } = await buyer.from('shops').select('name').eq('name', shopName)
    check('pembeli kini melihat toko yang disetujui', seenShop?.length === 1)
    const { data: seenCert } = await buyer.from('certificates').select('status').eq('shop_id', shopId)
    check('pembeli melihat sertifikat aktifnya', seenCert?.length === 1 && seenCert[0].status === 'ACTIVE')

    const { error: productError } = await seller.from('products').insert({
      shop_id: shopId,
      name: 'Produk Toko Baru',
      price: 15000,
      emoji: '🍲',
      age_group: 'M6_8',
      texture: 'HALUS',
      allergens: [],
      protein: 3,
      carbs: 12,
      fat: 2,
      fiber: 1,
    })
    check('penjual yang baru disetujui bisa menambah produk', !productError, productError?.message)
  }

  console.log('\nKelola pengguna')
  {
    const toggle = (client, userId, active) =>
      client.rpc('set_user_active', { p_user_id: userId, p_active: active })

    check('pembeli tidak bisa menonaktifkan akun', !!(await toggle(buyer, sellerId, false)).error)
    check('penjual tidak bisa menonaktifkan akun', !!(await toggle(dapur, sellerId, false)).error)
    check('akun admin tidak bisa dinonaktifkan', !!(await toggle(admin, adminProfile.id, false)).error)

    const { data: list, error: listError } = await admin
      .from('profiles')
      .select('id, name, role, is_active, shops(name, verification_status)')
      .ilike('name', '%Penjual Uji%')
    check(
      'admin bisa mencari akun beserta tokonya',
      !listError && list?.length === 1 && list[0].shops?.name === shopName,
      listError?.message
    )

    const off = await toggle(admin, sellerId, false)
    check('admin bisa menonaktifkan penjual', !off.error && off.data?.is_active === false, off.error?.message)

    const blocked = await newAnon().auth.signInWithPassword({ email, password: SEED_DEMO_PASSWORD })
    check('login akun nonaktif diblokir di tingkat Auth', !!blocked.error)

    const { data: bannedUser } = await service.auth.admin.getUserById(sellerId)
    check('banned_until terisi', !!bannedUser?.user?.banned_until)

    const { data: hiddenShop } = await buyer.from('shops').select('id').eq('name', shopName)
    check('toko penjual nonaktif hilang dari pembeli', (hiddenShop ?? []).length === 0)

    const on = await toggle(admin, sellerId, true)
    check('admin bisa mengaktifkan kembali', !on.error && on.data?.is_active === true, on.error?.message)

    const allowed = await newAnon().auth.signInWithPassword({ email, password: SEED_DEMO_PASSWORD })
    check('login berhasil lagi setelah diaktifkan', !allowed.error, allowed.error?.message)

    const { data: unbanned } = await service.auth.admin.getUserById(sellerId)
    check('banned_until dikosongkan', !unbanned?.user?.banned_until)
  }

  console.log('\nRingkasan admin')
  {
    check('anon tidak bisa membaca ringkasan', !!(await newAnon().rpc('admin_overview')).error)
    check('pembeli tidak bisa membaca ringkasan', !!(await buyer.rpc('admin_overview')).error)
    check('penjual tidak bisa membaca ringkasan', !!(await dapur.rpc('admin_overview')).error)

    const { data: ov, error } = await admin.rpc('admin_overview')
    check('admin bisa membaca ringkasan', !error && !!ov, error?.message)

    const { count: productCount } = await service
      .from('products')
      .select('id', { count: 'exact', head: true })
      .is('deleted_at', null)
    const { count: orderCount } = await service
      .from('orders')
      .select('id', { count: 'exact', head: true })
    const { count: reviewCount } = await service
      .from('reviews')
      .select('id', { count: 'exact', head: true })
    const { data: doneRows } = await service.from('orders').select('subtotal').eq('status', 'DONE')
    const revenue = (doneRows ?? []).reduce((sum, row) => sum + row.subtotal, 0)

    check('jumlah produk sesuai database', ov?.products === productCount)
    check('jumlah pesanan sesuai database', ov?.orders === orderCount)
    check('jumlah ulasan sesuai database', ov?.reviews === reviewCount)
    check('transaksi selesai sesuai database', ov?.revenue === revenue)
    check('penjual minimal 4 (3 demo + penjual uji)', ov?.sellers >= 4)
    check('pembeli minimal 1', ov?.buyers >= 1)
    check(
      'pesanan per status memuat kelima status dan totalnya sesuai',
      JSON.stringify(Object.keys(ov?.orders_by_status ?? {}).sort()) ===
        JSON.stringify(['COOKING', 'DONE', 'NEW', 'REJECTED', 'SHIPPING']) &&
        Object.values(ov.orders_by_status).reduce((a, b) => a + b, 0) === orderCount
    )
    check(
      'seller teratas maksimal 3 dan terurut menurun',
      Array.isArray(ov?.top_sellers) &&
        ov.top_sellers.length <= 3 &&
        ov.top_sellers.every((t, i, arr) => i === 0 || arr[i - 1].revenue >= t.revenue)
    )
    check('rata-rata rating antara 0 dan 5', ov?.average_rating >= 0 && ov?.average_rating <= 5)
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