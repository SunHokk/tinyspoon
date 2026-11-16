package id.tinyspoon.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CartRow(
    @SerialName("product_id") val productId: String,
    val quantity: Int
)

@Serializable
private data class CartUpsertRow(
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
    val quantity: Int
)

object CartRepository {

    // Semua tulis ke server diantre supaya urutannya sama dengan urutan klik
    private val lock = Mutex()

    internal suspend fun load(): List<CartRow>? = try {
        SupabaseProvider.client.from("cart_items")
            .select(Columns.raw("product_id,quantity"))
            .decodeList<CartRow>()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun setQuantity(productId: String, quantity: Int): Boolean =
        saveAll(listOf(productId to quantity))

    suspend fun saveAll(items: List<Pair<String, Int>>): Boolean {
        if (items.isEmpty()) return true
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return false
        return try {
            lock.withLock {
                SupabaseProvider.client.from("cart_items").upsert(
                    items.map { CartUpsertRow(userId, it.first, it.second.coerceIn(1, 50)) }
                )
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    suspend fun remove(productIds: List<String>): Boolean {
        if (productIds.isEmpty()) return true
        return try {
            lock.withLock {
                SupabaseProvider.client.from("cart_items").delete {
                    filter { isIn("product_id", productIds) }
                }
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }
}