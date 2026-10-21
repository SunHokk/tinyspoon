package id.tinyspoon.app.data

import id.tinyspoon.app.ui.screens.cart.CartItem
import id.tinyspoon.app.ui.screens.seller.OrderStatus
import id.tinyspoon.app.ui.screens.seller.SellerOrder
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class OrdersResult(val orders: List<SellerOrder>? = null, val error: String? = null)

data class PlaceOrderResult(val codes: List<String> = emptyList(), val error: String? = null)

@Serializable
internal data class OrderItemRow(
    @SerialName("product_name") val productName: String,
    val quantity: Int
)

@Serializable
internal data class OrderRow(
    val code: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("buyer_name") val buyerName: String,
    val status: String,
    val subtotal: Int,
    @SerialName("delivery_fee") val deliveryFee: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("order_items") val orderItems: List<OrderItemRow> = emptyList()
)

private fun formatOrderDate(iso: String): String = try {
    OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID")))
} catch (e: Exception) {
    iso.take(10)
}

private fun OrderRow.toSellerOrder() = SellerOrder(
    id = code,
    customerName = buyerName,
    items = orderItems.joinToString(", ") { "${it.productName} x${it.quantity}" },
    total = subtotal,
    status = when (status) {
        "COOKING" -> OrderStatus.COOKING
        "SHIPPING" -> OrderStatus.SHIPPING
        "DONE" -> OrderStatus.DONE
        "REJECTED" -> OrderStatus.REJECTED
        else -> OrderStatus.NEW
    },
    sellerName = shopName,
    date = formatOrderDate(createdAt),
    deliveryFee = deliveryFee
)

private fun orderError(e: Exception): String =
    if (e is RestException) {
        (e.message?.lineSequence()?.firstOrNull() ?: "Terjadi kesalahan").take(120)
    } else {
        "Tidak dapat terhubung ke server. Periksa internet kamu"
    }

object OrderRepository {

    suspend fun loadMyOrders(): OrdersResult = try {
        val rows = SupabaseProvider.client.from("orders")
            .select(
                Columns.raw(
                    "code,shop_name,buyer_name,status,subtotal,delivery_fee,created_at,order_items(product_name,quantity)"
                )
            ) { order("created_at", Order.DESCENDING) }
            .decodeList<OrderRow>()
        OrdersResult(orders = rows.map { it.toSellerOrder() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        OrdersResult(error = "Gagal memuat pesanan. ${orderError(e)}")
    }

    suspend fun placeOrder(
        items: List<CartItem>,
        address: String,
        notes: String,
        paymentMethod: String
    ): PlaceOrderResult = try {
        val payload = buildJsonObject {
            put("p_items", buildJsonArray {
                items.forEach { item ->
                    add(buildJsonObject {
                        put("product_id", item.product.id)
                        put("quantity", item.quantity)
                    })
                }
            })
            put("p_address", address)
            put("p_notes", notes)
            put("p_payment_method", paymentMethod)
        }
        val rows = SupabaseProvider.client.postgrest
            .rpc("place_order", payload)
            .decodeList<JsonObject>()
        PlaceOrderResult(codes = rows.mapNotNull { it["code"]?.jsonPrimitive?.contentOrNull })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        PlaceOrderResult(error = orderError(e))
    }
}