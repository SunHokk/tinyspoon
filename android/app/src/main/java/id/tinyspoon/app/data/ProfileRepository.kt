package id.tinyspoon.app.data

import id.tinyspoon.app.ui.screens.profile.NotificationPrefs
import id.tinyspoon.app.ui.screens.profile.SavedAddress
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class BuyerProfileData(
    val addresses: List<SavedAddress>,
    val prefs: NotificationPrefs,
    val appRating: Int,
    val reviewCount: Int
)

@Serializable
internal data class ProfileAddressRow(
    val id: String,
    val label: String,
    val recipient: String,
    @SerialName("full_address") val fullAddress: String,
    @SerialName("is_default") val isDefault: Boolean
)

@Serializable
internal data class ProfilePrefsRow(
    @SerialName("order_status") val orderStatus: Boolean = true,
    val promo: Boolean = true,
    @SerialName("review_reminder") val reviewReminder: Boolean = false,
    @SerialName("new_products") val newProducts: Boolean = false
)

@Serializable
internal data class ProfilePrefsUpdate(
    @SerialName("order_status") val orderStatus: Boolean,
    val promo: Boolean,
    @SerialName("review_reminder") val reviewReminder: Boolean,
    @SerialName("new_products") val newProducts: Boolean
)

@Serializable
internal data class ProfileRatingRow(val rating: Int)

@Serializable
internal data class ProfileIdRow(val id: String)

private fun profileError(e: Exception): String =
    if (e is RestException) {
        (e.message?.lineSequence()?.firstOrNull() ?: "Terjadi kesalahan").take(120)
    } else {
        "Tidak dapat terhubung ke server. Periksa internet kamu"
    }

object ProfileRepository {

    suspend fun loadProfileData(): BuyerProfileData? {
        return try {
            val client = SupabaseProvider.client
            val uid = client.auth.currentUserOrNull()?.id ?: return null

            val addresses = client.from("addresses")
                .select(
                    Columns.list("id", "label", "recipient", "full_address", "is_default")
                ) { order("created_at", Order.ASCENDING) }
                .decodeList<ProfileAddressRow>()
                .map {
                    SavedAddress(
                        id = it.id,
                        label = it.label,
                        recipient = it.recipient,
                        fullAddress = it.fullAddress,
                        isDefault = it.isDefault
                    )
                }
                .sortedByDescending { it.isDefault }

            val prefs = client.from("notification_prefs")
                .select(
                    Columns.list("order_status", "promo", "review_reminder", "new_products")
                ) { filter { eq("user_id", uid) } }
                .decodeSingleOrNull<ProfilePrefsRow>()

            val rating = client.from("app_ratings")
                .select(Columns.list("rating")) { filter { eq("user_id", uid) } }
                .decodeSingleOrNull<ProfileRatingRow>()

            val reviewCount = client.from("reviews")
                .select(Columns.list("id")) { filter { eq("user_id", uid) } }
                .decodeList<ProfileIdRow>()
                .size

            BuyerProfileData(
                addresses = addresses,
                prefs = NotificationPrefs(
                    orderStatus = prefs?.orderStatus ?: true,
                    promo = prefs?.promo ?: true,
                    reviewReminder = prefs?.reviewReminder ?: false,
                    newProducts = prefs?.newProducts ?: false
                ),
                appRating = rating?.rating ?: 0,
                reviewCount = reviewCount
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Mengembalikan pesan error, atau null jika berhasil. */
    suspend fun saveAddress(address: SavedAddress, isNew: Boolean): String? = try {
        SupabaseProvider.client.postgrest.rpc(
            "save_address",
            buildJsonObject {
                if (isNew) put("p_id", JsonNull) else put("p_id", address.id)
                put("p_label", address.label)
                put("p_recipient", address.recipient)
                put("p_full_address", address.fullAddress)
                put("p_make_default", address.isDefault)
            }
        )
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        profileError(e)
    }

    suspend fun setDefaultAddress(id: String): String? = try {
        SupabaseProvider.client.postgrest.rpc(
            "set_default_address",
            buildJsonObject { put("p_id", id) }
        )
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        profileError(e)
    }

    suspend fun deleteAddress(id: String): String? = try {
        SupabaseProvider.client.postgrest.rpc(
            "delete_address",
            buildJsonObject { put("p_id", id) }
        )
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        profileError(e)
    }

    suspend fun updatePrefs(prefs: NotificationPrefs): String? {
        return try {
            val client = SupabaseProvider.client
            val uid = client.auth.currentUserOrNull()?.id ?: return "Sesi tidak ditemukan"
            client.from("notification_prefs").update(
                ProfilePrefsUpdate(
                    orderStatus = prefs.orderStatus,
                    promo = prefs.promo,
                    reviewReminder = prefs.reviewReminder,
                    newProducts = prefs.newProducts
                )
            ) { filter { eq("user_id", uid) } }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            profileError(e)
        }
    }

    suspend fun submitAppRating(rating: Int, feedback: String): String? = try {
        SupabaseProvider.client.postgrest.rpc(
            "submit_app_rating",
            buildJsonObject {
                put("p_rating", rating)
                put("p_feedback", feedback)
            }
        )
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        profileError(e)
    }
}