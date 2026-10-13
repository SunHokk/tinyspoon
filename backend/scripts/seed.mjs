import { createClient } from '@supabase/supabase-js'

const {
  SUPABASE_URL,
  SUPABASE_SERVICE_ROLE_KEY,
  SEED_DEMO_PASSWORD,
  SEED_ADMIN_PASSWORD,
} = process.env

function requireEnv(name, value, minLength = 1) {
  if (!value || value.length < minLength) {
    console.error(
      `Isi ${name} di backend/.env${minLength > 1 ? ` (minimal ${minLength} karakter)` : ''}`
    )
    process.exit(1)
  }
}

requireEnv('SUPABASE_URL', SUPABASE_URL)
requireEnv('SUPABASE_SERVICE_ROLE_KEY', SUPABASE_SERVICE_ROLE_KEY)
requireEnv('SEED_DEMO_PASSWORD', SEED_DEMO_PASSWORD, 10)
requireEnv('SEED_ADMIN_PASSWORD', SEED_ADMIN_PASSWORD, 10)

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
  auth: { autoRefreshToken: false, persistSession: false },
})

const accounts = [
  {
    email: 'gilbert@email.com',
    name: 'Gilbert',
    role: 'BUYER',
    password: SEED_DEMO_PASSWORD,
  },
  {
    email: 'dapurbunda@email.com',
    name: 'Bunda Lia',
    role: 'SELLER',
    password: SEED_DEMO_PASSWORD,
    shop: {
      name: 'Dapur Bunda',
      description:
        'Dapur rumahan yang menyiapkan MPASI segar setiap hari tanpa pengawet, dimasak sesuai tahapan usia bayi.',
      address: 'Jl. Cipete Raya No. 25, Jakarta Selatan',
      phone: '081234567890',
      open_hours: '07.00 - 17.00',
    },
    certificate: {
      type: 'Sertifikat Produksi Pangan Industri Rumah Tangga',
      number: 'BPOM-MPASI-2024-001234',
      issued_by: 'BPOM (Badan Pengawas Obat dan Makanan)',
      issued_date: '2024-01-01',
      expired_date: '2027-12-31',
    },
  },
  {
    email: 'mpasisehat@email.com',
    name: 'Pak Dimas',
    role: 'SELLER',
    password: SEED_DEMO_PASSWORD,
    shop: {
      name: 'MPASI Sehat',
      description:
        'Spesialis puree dan bubur sayur-buah dengan bahan organik pilihan untuk si kecil.',
      address: 'Jl. Fatmawati No. 8, Jakarta Selatan',
      phone: '081298765432',
      open_hours: '08.00 - 16.00',
    },
    certificate: {
      type: 'Sertifikat Produksi Pangan Industri Rumah Tangga',
      number: 'BPOM-MPASI-2024-005678',
      issued_by: 'BPOM (Badan Pengawas Obat dan Makanan)',
      issued_date: '2024-03-15',
      expired_date: '2027-03-14',
    },
  },
  {
    email: 'nutrisibayi@email.com',
    name: 'Bu Rani',
    role: 'SELLER',
    password: SEED_DEMO_PASSWORD,
    shop: {
      name: 'Nutrisi Bayi',
      description:
        'MPASI tinggi protein hewani seperti ikan, ayam, dan hati, dengan tekstur sesuai usia.',
      address: 'Jl. Kemang Selatan No. 3, Jakarta Selatan',
      phone: '081377788899',
      open_hours: '09.00 - 18.00',
    },
    certificate: {
      type: 'Sertifikat Halal & Produksi Pangan',
      number: 'BPOM-MPASI-2023-009012',
      issued_by: 'BPOM (Badan Pengawas Obat dan Makanan)',
      issued_date: '2023-06-10',
      expired_date: '2027-06-09',
    },
  },
  {
    email: 'admin@tinyspoon.id',
    name: 'Admin TinySpoon',
    role: 'ADMIN',
    password: SEED_ADMIN_PASSWORD,
  },
]

async function must(promise, label) {
  const { data, error } = await promise
  if (error) throw new Error(`${label}: ${error.message}`)
  return data
}

async function findProfileId(email) {
  const data = await must(
    supabase.from('profiles').select('id').eq('email', email.toLowerCase()).maybeSingle(),
    `Cari profil ${email}`
  )
  return data?.id ?? null
}

for (const account of accounts) {
  let id = await findProfileId(account.email)

  if (id) {
    await must(
      supabase.auth.admin.updateUserById(id, {
        password: account.password,
        email_confirm: true,
      }),
      `Perbarui ${account.email}`
    )
    console.log(`• ${account.email} sudah ada, password disamakan dengan .env`)
  } else {
    const { data, error } = await supabase.auth.admin.createUser({
      email: account.email,
      password: account.password,
      email_confirm: true,
      user_metadata: {
        name: account.name,
        role: account.role === 'SELLER' ? 'SELLER' : 'BUYER',
        shop_name: account.shop?.name,
      },
    })
    if (error) throw new Error(`Buat ${account.email}: ${error.message}`)
    id = data.user.id
    console.log(`✓ dibuat ${account.email} (${account.role})`)
  }

  if (account.role === 'ADMIN') {
    await must(
      supabase.from('profiles').update({ role: 'ADMIN' }).eq('id', id),
      'Set peran admin'
    )
  }

  if (account.role === 'SELLER') {
    const { name: _shopName, ...shopFields } = account.shop
    const shop = await must(
      supabase
        .from('shops')
        .update({ ...shopFields, verification_status: 'APPROVED', rejection_reason: null })
        .eq('owner_id', id)
        .select('id')
        .single(),
      `Perbarui toko ${account.shop.name}`
    )

    const existing = await must(
      supabase
        .from('certificates')
        .select('id')
        .eq('shop_id', shop.id)
        .eq('number', account.certificate.number),
      'Cek sertifikat'
    )
    if (existing.length === 0) {
      await must(
        supabase.from('certificates').insert({
          shop_id: shop.id,
          type: account.certificate.type,
          number: account.certificate.number,
          issued_by: account.certificate.issued_by,
          issued_date: account.certificate.issued_date,
          expired_date: account.certificate.expired_date,
          document_path: 'seed/sertifikat_contoh.pdf',
          status: 'ACTIVE',
          reviewed_at: new Date().toISOString(),
        }),
        'Isi sertifikat'
      )
    }
  }
}

console.log('\nSeed selesai.')