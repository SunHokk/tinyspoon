package id.tinyspoon.app.data

import id.tinyspoon.app.BuildConfig
import id.tinyspoon.app.ui.screens.auth.SellerStatus
import id.tinyspoon.app.ui.screens.auth.UserAccount
import id.tinyspoon.app.ui.screens.auth.UserRole
import id.tinyspoon.app.ui.screens.product.CertStatus
import id.tinyspoon.app.ui.screens.product.SellerCertificate
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

data class AdminData(
    val accounts: List<UserAccount>,
    val certificates: Map<String, SellerCertificate>,
    val documentPaths: Map<String, String>
)

@Serializable
internal data class AdminProfileRow(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
internal data class AdminShopRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    @SerialName("verification_status") val verificationStatus: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

@Serializable
internal data class AdminCertRow(
    @SerialName("shop_id") val shopId: String,
    val type: String,
    val number: String,
    @SerialName("issued_by") val issuedBy: String,
    @SerialName("issued_date") val issuedDate: String,
    @SerialName("expired_date") val expiredDate: String,
    @SerialName("document_path") val documentPath: String,
    val status: String
)

@Serializable
internal data class AdminIdRow(val id: String)

private val adminDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))

private fun adminDate(iso: String): String = try {
    LocalDate.parse(iso.take(10)).format(adminDateFormat)
} catch (e: Exception) {
    iso.take(10)
}

private fun adminError(e: Exception): String =
    if (e is RestException) {
        (e.message?.lineSequence()?.firstOrNull() ?: "Terjadi kesalahan").take(120)
    } else {
        "Tidak dapat terhubung ke server. Periksa internet kamu"
    }

object AdminRepository {

    suspend fun loadAdminData(): AdminData? {
        return try {
            val client = SupabaseProvider.client

            val profiles = client.from("profiles")
                .select(Columns.list("id", "name", "email", "role", "is_active")) {
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<AdminProfileRow>()

            val shops = client.from("shops")
                .select(
                    Columns.list("id", "owner_id", "name", "verification_status", "rejection_reason")
                )
                .decodeList<AdminShopRow>()

            val certs = client.from("certificates")
                .select(
                    Columns.list(
                        "shop_id", "type", "number", "issued_by", "issued_date",
                        "expired_date", "document_path", "status"
                    )
                ) { order("submitted_at", Order.DESCENDING) }
                .decodeList<AdminCertRow>()

            val shopByOwner = shops.associateBy { it.ownerId }
            val latestCertByShop = LinkedHashMap<String, AdminCertRow>()
            certs.forEach { latestCertByShop.putIfAbsent(it.shopId, it) }

            val accounts = profiles.map { p ->
                val shop = shopByOwner[p.id]
                UserAccount(
                    id = p.id,
                    name = p.name,
                    email = p.email,
                    role = when (p.role) {
                        "SELLER" -> UserRole.SELLER
                        "ADMIN" -> UserRole.ADMIN
                        else -> UserRole.BUYER
                    },
                    shopName = shop?.name,
                    sellerStatus = shop?.let {
                        when (it.verificationStatus) {
                            "APPROVED" -> SellerStatus.APPROVED
                            "REJECTED" -> SellerStatus.REJECTED
                            else -> SellerStatus.PENDING
                        }
                    },
                    rejectionReason = shop?.rejectionReason,
                    isActive = p.isActive
                )
            }

            val certificates = HashMap<String, SellerCertificate>()
            val paths = HashMap<String, String>()
            shops.forEach { shop ->
                latestCertByShop[shop.id]?.let { c ->
                    certificates[shop.name] = SellerCertificate(
                        sellerName = shop.name,
                        certNumber = c.number,
                        issuedBy = c.issuedBy,
                        issuedDate = adminDate(c.issuedDate),
                        expiredDate = adminDate(c.expiredDate),
                        certType = c.type,
                        status = when (c.status) {
                            "ACTIVE" -> CertStatus.ACTIVE
                            "EXPIRED" -> CertStatus.EXPIRED
                            else -> CertStatus.PENDING
                        },
                        documentName = c.documentPath
                            .substringAfterLast('/')
                            .replaceFirst(Regex("^\\d+-"), "")
                    )
                    paths[shop.name] = c.documentPath
                }
            }

            AdminData(accounts, certificates, paths)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Mengembalikan pesan error, atau null jika berhasil. */
    suspend fun reviewShop(ownerId: String, approve: Boolean, reason: String?): String? = try {
        val client = SupabaseProvider.client
        val shop = client.from("shops")
            .select(Columns.list("id")) { filter { eq("owner_id", ownerId) } }
            .decodeSingle<AdminIdRow>()
        client.postgrest.rpc(
            "review_shop",
            buildJsonObject {
                put("p_shop_id", shop.id)
                put("p_approve", approve)
                if (!approve) put("p_reason", reason ?: "")
            }
        )
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        adminError(e)
    }

    suspend fun setUserActive(userId: String, active: Boolean): String? = try {
        SupabaseProvider.client.postgrest.rpc(
            "set_user_active",
            buildJsonObject {
                put("p_user_id", userId)
                put("p_active", active)
            }
        )
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        adminError(e)
    }

    /** Tautan sementara (5 menit) untuk membuka dokumen sertifikat di bucket privat. */
    suspend fun createDocumentUrl(path: String): String? {
        return try {
            val signed = SupabaseProvider.client.storage
                .from("certificates")
                .createSignedUrl(path, 5.minutes)
            if (signed.startsWith("http")) signed
            else "${BuildConfig.SUPABASE_URL}/storage/v1/${signed.trimStart('/')}"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}