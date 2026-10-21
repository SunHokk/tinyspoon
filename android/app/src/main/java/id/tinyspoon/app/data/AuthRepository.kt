package id.tinyspoon.app.data

import id.tinyspoon.app.ui.screens.auth.SellerStatus
import id.tinyspoon.app.ui.screens.auth.UserAccount
import id.tinyspoon.app.ui.screens.auth.UserRole
import id.tinyspoon.app.ui.screens.seller.ShopProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import id.tinyspoon.app.ui.screens.product.SellerCertificate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class SessionUser(
    val account: UserAccount,
    val shop: ShopProfile?,
    val certificate: SellerCertificate? = null
)

data class AuthResult(val user: SessionUser? = null, val error: String? = null)

@Serializable
internal data class ProfileRow(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
internal data class ShopRow(
    val name: String,
    val description: String = "",
    val address: String = "",
    val phone: String = "",
    @SerialName("open_hours") val openHours: String = "",
    @SerialName("verification_status") val verificationStatus: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

object AuthRepository {

    private val auth get() = SupabaseProvider.client.auth

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            loadCurrentUser()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult(error = friendlyError(e))
        }
    }

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        shopName: String
    ): AuthResult {
        return try {
            if (role == UserRole.SELLER) {
                val available = SupabaseProvider.client.postgrest
                    .rpc("check_shop_name", buildJsonObject { put("p_name", shopName.trim()) })
                    .decodeAs<Boolean>()
                if (!available) return AuthResult(error = "Nama toko sudah dipakai")
            }

            auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    put("name", name.trim())
                    put("role", if (role == UserRole.SELLER) "SELLER" else "BUYER")
                    if (role == UserRole.SELLER) put("shop_name", shopName.trim())
                }
            }

            if (auth.currentSessionOrNull() == null) {
                AuthResult(error = "Akun berhasil dibuat. Silakan masuk dengan akun tersebut")
            } else {
                loadCurrentUser()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult(error = friendlyError(e))
        }
    }

    /** Dipanggil saat app dibuka: kembalikan pengguna jika sesi tersimpan masih valid. */
    suspend fun restoreSession(): SessionUser? {
        return try {
            withTimeoutOrNull(8_000) {
                auth.awaitInitialization()
                if (auth.currentSessionOrNull() == null) null else loadCurrentUser().user
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Gagal menghubungi server saat logout tidak perlu menghalangi pengguna.
        }
    }

    private suspend fun loadCurrentUser(): AuthResult {
        val uid = auth.currentUserOrNull()?.id
            ?: return AuthResult(error = "Sesi tidak ditemukan")

        val profile = SupabaseProvider.client.from("profiles")
            .select(Columns.list("id", "name", "email", "role", "is_active")) {
                filter { eq("id", uid) }
            }
            .decodeSingle<ProfileRow>()

        if (!profile.isActive) {
            auth.signOut()
            return AuthResult(error = "Akun ini dinonaktifkan oleh admin")
        }

        val role = when (profile.role) {
            "SELLER" -> UserRole.SELLER
            "ADMIN" -> UserRole.ADMIN
            else -> UserRole.BUYER
        }

        val shop = if (role == UserRole.SELLER) {
            SupabaseProvider.client.from("shops")
                .select(
                    Columns.list(
                        "name", "description", "address", "phone",
                        "open_hours", "verification_status", "rejection_reason"
                    )
                ) { filter { eq("owner_id", uid) } }
                .decodeSingleOrNull<ShopRow>()
        } else null

        val account = UserAccount(
            id = profile.id,
            name = profile.name,
            email = profile.email,
            password = "",
            role = role,
            shopName = shop?.name,
            sellerStatus = shop?.let {
                when (it.verificationStatus) {
                    "APPROVED" -> SellerStatus.APPROVED
                    "REJECTED" -> SellerStatus.REJECTED
                    else -> SellerStatus.PENDING
                }
            },
            rejectionReason = shop?.rejectionReason,
            isActive = true
        )

        val shopProfile = shop?.let {
            ShopProfile(
                name = it.name,
                description = it.description,
                address = it.address,
                phone = it.phone,
                openHours = it.openHours
            )
        }

        val certificate = if (role == UserRole.SELLER) SellerRepository.loadMyCertificate() else null
        return AuthResult(user = SessionUser(account, shopProfile, certificate))
    }

    private fun friendlyError(e: Exception): String {
        val msg = (e.message ?: "").lowercase()
        return when {
            "invalid login credentials" in msg -> "Email atau password salah"
            "banned" in msg -> "Akun ini dinonaktifkan oleh admin"
            "already registered" in msg || "already been registered" in msg -> "Email sudah terdaftar"
            "database error" in msg -> "Pendaftaran gagal. Nama toko mungkin sudah dipakai"
            "rate limit" in msg -> "Terlalu banyak percobaan. Coba lagi beberapa saat"
            e is RestException -> "Terjadi kesalahan: ${e.message?.take(100)}"
            else -> "Tidak dapat terhubung ke server. Periksa internet kamu"
        }
    }
}