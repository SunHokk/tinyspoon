package id.tinyspoon.app.data

import id.tinyspoon.app.ui.screens.home.Product
import id.tinyspoon.app.ui.screens.product.CertStatus
import id.tinyspoon.app.ui.screens.product.SellerCertificate
import id.tinyspoon.app.ui.screens.seller.OrderStatus
import id.tinyspoon.app.ui.screens.seller.ShopProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
internal data class SellerIdRow(val id: String)

@Serializable
internal data class SellerShopRef(val id: String, val name: String)

@Serializable
internal data class SellerProductInsert(
    @SerialName("shop_id") val shopId: String,
    val name: String,
    val price: Int,
    val emoji: String,
    @SerialName("age_group") val ageGroup: String,
    val texture: String,
    val allergens: List<String>,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val fiber: Int
)

@Serializable
internal data class SellerProductUpdate(
    val name: String,
    val price: Int,
    val emoji: String,
    @SerialName("age_group") val ageGroup: String,
    val texture: String,
    val allergens: List<String>,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val fiber: Int
)

@Serializable
internal data class SellerProductSoftDelete(
    @SerialName("deleted_at") val deletedAt: String
)

@Serializable
internal data class SellerShopUpdate(
    val description: String,
    val address: String,
    val phone: String,
    @SerialName("open_hours") val openHours: String
)

@Serializable
internal data class SellerCertRow(
    val type: String,
    val number: String,
    @SerialName("issued_by") val issuedBy: String,
    @SerialName("issued_date") val issuedDate: String,
    @SerialName("expired_date") val expiredDate: String,
    @SerialName("document_path") val documentPath: String,
    val status: String
)

private val sellerLocale = Locale("id", "ID")
private val certDisplayFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMMM yyyy", sellerLocale)

private val ageKeys = mapOf("6-8 bulan" to "M6_8", "9-11 bulan" to "M9_11", "12+ bulan" to "M12_PLUS")

private fun formatCertDate(iso: String): String = try {
    LocalDate.parse(iso.take(10)).format(certDisplayFormat)
} catch (e: Exception) {
    iso.take(10)
}

private fun sellerError(e: Exception): String =
    if (e is RestException) {
        (e.message?.lineSequence()?.firstOrNull() ?: "Terjadi kesalahan").take(120)
    } else {
        "Tidak dapat terhubung ke server. Periksa internet kamu"
    }

object SellerRepository {

    /** Mengembalikan pesan error, atau null jika berhasil. */
    suspend fun saveProduct(product: Product, isNew: Boolean): String? {
        val ageKey = ageKeys[product.ageGroup] ?: return "Kategori usia tidak valid"
        val textureKey = product.texture.uppercase()
        val allergenKeys = product.allergens.map { it.uppercase() }

        return try {
            val client = SupabaseProvider.client
            if (isNew) {
                val uid = client.auth.currentUserOrNull()?.id ?: return "Sesi tidak ditemukan"
                val shop = client.from("shops")
                    .select(Columns.list("id")) { filter { eq("owner_id", uid) } }
                    .decodeSingle<SellerIdRow>()
                client.from("products").insert(
                    SellerProductInsert(
                        shopId = shop.id,
                        name = product.name,
                        price = product.price,
                        emoji = product.imageEmoji,
                        ageGroup = ageKey,
                        texture = textureKey,
                        allergens = allergenKeys,
                        protein = product.protein,
                        carbs = product.carbs,
                        fat = product.fat,
                        fiber = product.fiber
                    )
                )
            } else {
                client.from("products").update(
                    SellerProductUpdate(
                        name = product.name,
                        price = product.price,
                        emoji = product.imageEmoji,
                        ageGroup = ageKey,
                        texture = textureKey,
                        allergens = allergenKeys,
                        protein = product.protein,
                        carbs = product.carbs,
                        fat = product.fat,
                        fiber = product.fiber
                    )
                ) { filter { eq("id", product.id) } }
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sellerError(e)
        }
    }

    suspend fun deleteProduct(productId: String): String? = try {
        SupabaseProvider.client.from("products")
            .update(SellerProductSoftDelete(OffsetDateTime.now().toString())) {
                filter { eq("id", productId) }
            }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        sellerError(e)
    }

    suspend fun updateShop(shop: ShopProfile): String? {
        return try {
            val client = SupabaseProvider.client
            val uid = client.auth.currentUserOrNull()?.id ?: return "Sesi tidak ditemukan"
            client.from("shops").update(
                SellerShopUpdate(
                    description = shop.description,
                    address = shop.address,
                    phone = shop.phone,
                    openHours = shop.openHours
                )
            ) { filter { eq("owner_id", uid) } }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sellerError(e)
        }
    }

    suspend fun updateOrderStatus(orderCode: String, newStatus: OrderStatus): String? = try {
        val client = SupabaseProvider.client
        val row = client.from("orders")
            .select(Columns.list("id")) { filter { eq("code", orderCode) } }
            .decodeSingle<SellerIdRow>()
        client.postgrest.rpc(
            "set_order_status",
            buildJsonObject {
                put("p_order_id", row.id)
                put("p_status", newStatus.name)
            }
        )
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        sellerError(e)
    }

    /** Sertifikat terbaru milik toko penjual yang sedang login (null jika belum ada). */
    suspend fun loadMyCertificate(): SellerCertificate? {
        return try {
            val client = SupabaseProvider.client
            val uid = client.auth.currentUserOrNull()?.id ?: return null
            val shop = client.from("shops")
                .select(Columns.list("id", "name")) { filter { eq("owner_id", uid) } }
                .decodeSingleOrNull<SellerShopRef>() ?: return null
            val cert = client.from("certificates")
                .select(
                    Columns.list(
                        "type", "number", "issued_by", "issued_date",
                        "expired_date", "document_path", "status"
                    )
                ) {
                    filter { eq("shop_id", shop.id) }
                    order("submitted_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<SellerCertRow>()
                .firstOrNull() ?: return null

            SellerCertificate(
                sellerName = shop.name,
                certNumber = cert.number,
                issuedBy = cert.issuedBy,
                issuedDate = formatCertDate(cert.issuedDate),
                expiredDate = formatCertDate(cert.expiredDate),
                certType = cert.type,
                status = when (cert.status) {
                    "ACTIVE" -> CertStatus.ACTIVE
                    "EXPIRED" -> CertStatus.EXPIRED
                    else -> CertStatus.PENDING
                },
                documentName = cert.documentPath
                    .substringAfterLast('/')
                    .replaceFirst(Regex("^\\d+-"), "")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Unggah dokumen ke Storage lalu kirim sertifikat. Mengembalikan pesan error atau null. */
    suspend fun submitCertificate(cert: SellerCertificate, bytes: ByteArray): String? {
        if (bytes.size > 5 * 1024 * 1024) return "Ukuran file maksimal 5 MB"

        val fileName = cert.documentName
        val mime = when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> return "Format file harus PDF, JPG, atau PNG"
        }

        return try {
            val client = SupabaseProvider.client
            val uid = client.auth.currentUserOrNull()?.id ?: return "Sesi tidak ditemukan"
            val shop = client.from("shops")
                .select(Columns.list("id")) { filter { eq("owner_id", uid) } }
                .decodeSingle<SellerIdRow>()

            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val path = "${shop.id}/${System.currentTimeMillis()}-$safeName"

            client.storage.from("certificates").upload(path, bytes) {
                upsert = false
                contentType = ContentType.parse(mime)
            }

            val issued = LocalDate.parse(cert.issuedDate, certDisplayFormat)
            val expired = LocalDate.parse(cert.expiredDate, certDisplayFormat)

            client.postgrest.rpc(
                "submit_certificate",
                buildJsonObject {
                    put("p_type", cert.certType)
                    put("p_number", cert.certNumber)
                    put("p_issued_by", cert.issuedBy)
                    put("p_issued_date", issued.toString())
                    put("p_expired_date", expired.toString())
                    put("p_document_path", path)
                }
            )
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sellerError(e)
        }
    }
}