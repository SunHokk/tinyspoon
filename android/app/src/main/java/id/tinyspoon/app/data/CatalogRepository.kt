package id.tinyspoon.app.data

import id.tinyspoon.app.ui.screens.home.Product
import id.tinyspoon.app.ui.screens.product.CertStatus
import id.tinyspoon.app.ui.screens.product.Review
import id.tinyspoon.app.ui.screens.product.SellerCertificate
import id.tinyspoon.app.ui.screens.seller.ShopProfile
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CatalogData(
    val products: List<Product>,
    val reviews: List<Review>,
    val favoriteIds: Set<String>
)

data class CatalogResult(val data: CatalogData? = null, val error: String? = null)

data class ShopDetail(val shop: ShopProfile?, val certificate: SellerCertificate?)

@Serializable
internal data class ProductRow(
    val id: String,
    @SerialName("shop_name") val shopName: String,
    val name: String,
    val price: Int,
    val emoji: String,
    @SerialName("age_group") val ageGroup: String,
    val texture: String,
    val allergens: List<String> = emptyList(),
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val fiber: Int = 0,
    @SerialName("deleted_at") val deletedAt: String? = null,
    val rating: Double = 0.0
)

@Serializable
internal data class ReviewRow(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("author_name") val authorName: String = "",
    val rating: Int,
    val comment: String,
    @SerialName("baby_age") val babyAge: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
internal data class FavoriteRow(@SerialName("product_id") val productId: String)

@Serializable
internal data class FavoriteInsert(@SerialName("product_id") val productId: String)

@Serializable
internal data class ShopDetailRow(
    val id: String,
    val name: String,
    val description: String = "",
    val address: String = "",
    val phone: String = "",
    @SerialName("open_hours") val openHours: String = ""
)

@Serializable
internal data class CertRow(
    val type: String,
    val number: String,
    @SerialName("issued_by") val issuedBy: String,
    @SerialName("issued_date") val issuedDate: String,
    @SerialName("expired_date") val expiredDate: String,
    val status: String
)

private val idLocale = Locale("id", "ID")

private val ageLabels = mapOf("M6_8" to "6-8 bulan", "M9_11" to "9-11 bulan", "M12_PLUS" to "12+ bulan")
private val textureLabels = mapOf("HALUS" to "Halus", "LEMBUT" to "Lembut", "KASAR" to "Kasar")
private val allergenLabels = mapOf(
    "SUSU" to "Susu", "TELUR" to "Telur", "KACANG" to "Kacang",
    "GLUTEN" to "Gluten", "SEAFOOD" to "Seafood"
)

private fun formatDate(iso: String, pattern: String): String = try {
    LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern(pattern, idLocale))
} catch (e: Exception) {
    iso.take(10)
}

private fun formatTimestamp(iso: String, pattern: String): String = try {
    OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern(pattern, idLocale))
} catch (e: Exception) {
    iso.take(10)
}

private fun ProductRow.toProduct() = Product(
    id = id,
    name = name,
    price = price,
    sellerName = shopName,
    ageGroup = ageLabels[ageGroup] ?: ageGroup,
    texture = textureLabels[texture] ?: texture,
    rating = rating.toFloat(),
    imageEmoji = emoji,
    allergens = allergens.map { allergenLabels[it] ?: it },
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber
)

private fun ReviewRow.toReview() = Review(
    id = id,
    productId = productId,
    userName = authorName.ifBlank { "Pengguna" },
    rating = rating,
    comment = comment,
    date = formatTimestamp(createdAt, "d MMM yyyy"),
    babyAge = babyAge ?: ""
)

private fun friendlyError(e: Exception): String =
    if (e is RestException) {
        (e.message?.lineSequence()?.firstOrNull() ?: "Terjadi kesalahan").take(120)
    } else {
        "Tidak dapat terhubung ke server. Periksa internet kamu"
    }

object CatalogRepository {

    suspend fun loadCatalog(): CatalogResult = try {
        val client = SupabaseProvider.client

        val products = client.from("products_with_stats")
            .select(
                Columns.list(
                    "id", "shop_name", "name", "price", "emoji", "age_group", "texture",
                    "allergens", "protein", "carbs", "fat", "fiber", "deleted_at", "rating"
                )
            ) { order("created_at", Order.ASCENDING) }
            .decodeList<ProductRow>()
            .filter { it.deletedAt == null }
            .map { it.toProduct() }

        val reviews = client.from("reviews")
            .select(
                Columns.list(
                    "id", "product_id", "author_name", "rating", "comment", "baby_age", "created_at"
                )
            ) { order("created_at", Order.DESCENDING) }
            .decodeList<ReviewRow>()
            .map { it.toReview() }

        val favorites = client.from("favorites")
            .select(Columns.list("product_id"))
            .decodeList<FavoriteRow>()
            .map { it.productId }
            .toSet()

        CatalogResult(data = CatalogData(products, reviews, favorites))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        CatalogResult(error = "Gagal memuat produk. ${friendlyError(e)}")
    }

    suspend fun loadShopDetail(shopName: String): ShopDetail = try {
        val client = SupabaseProvider.client

        val shop = client.from("shops")
            .select(Columns.list("id", "name", "description", "address", "phone", "open_hours")) {
                filter { eq("name", shopName) }
            }
            .decodeList<ShopDetailRow>()
            .firstOrNull()

        val cert = if (shop == null) null else {
            client.from("certificates")
                .select(
                    Columns.list("type", "number", "issued_by", "issued_date", "expired_date", "status")
                ) {
                    filter {
                        eq("shop_id", shop.id)
                        eq("status", "ACTIVE")
                    }
                    order("submitted_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<CertRow>()
                .firstOrNull()
        }

        ShopDetail(
            shop = shop?.let {
                ShopProfile(
                    name = it.name,
                    description = it.description,
                    address = it.address,
                    phone = it.phone,
                    openHours = it.openHours
                )
            },
            certificate = cert?.let {
                SellerCertificate(
                    sellerName = shopName,
                    certNumber = it.number,
                    issuedBy = it.issuedBy,
                    issuedDate = formatDate(it.issuedDate, "dd MMMM yyyy"),
                    expiredDate = formatDate(it.expiredDate, "dd MMMM yyyy"),
                    certType = it.type,
                    status = when (it.status) {
                        "ACTIVE" -> CertStatus.ACTIVE
                        "EXPIRED" -> CertStatus.EXPIRED
                        else -> CertStatus.PENDING
                    }
                )
            }
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ShopDetail(null, null)
    }

    /** Mengembalikan pesan error, atau null jika berhasil. */
    suspend fun submitReview(productId: String, rating: Int, comment: String): String? = try {
        SupabaseProvider.client.postgrest.rpc(
            "submit_review",
            buildJsonObject {
                put("p_product_id", productId)
                put("p_rating", rating)
                put("p_comment", comment)
            }
        )
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        friendlyError(e)
    }

    suspend fun addFavorite(productId: String): Boolean = try {
        SupabaseProvider.client.from("favorites").insert(FavoriteInsert(productId))
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    suspend fun removeFavorite(productId: String): Boolean = try {
        SupabaseProvider.client.from("favorites").delete {
            filter { eq("product_id", productId) }
        }
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
}