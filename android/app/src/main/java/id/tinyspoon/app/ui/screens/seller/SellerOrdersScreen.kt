package id.tinyspoon.app.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.screens.home.CategoryChip
import id.tinyspoon.app.ui.theme.*

enum class OrderStatus(
    val label: String,
    val color: Long,
    val actionLabel: String?
) {
    NEW("Baru", 0xFFFF9800, "Terima & Masak"),
    COOKING("Dimasak", 0xFF2196F3, "Kirim Pesanan"),
    SHIPPING("Dikirim", 0xFF9C27B0, "Tandai Selesai"),
    DONE("Selesai", 0xFF4CAF50, null),
    REJECTED("Ditolak", 0xFFE53935, null)
}

fun OrderStatus.next(): OrderStatus? = when (this) {
    OrderStatus.NEW -> OrderStatus.COOKING
    OrderStatus.COOKING -> OrderStatus.SHIPPING
    OrderStatus.SHIPPING -> OrderStatus.DONE
    else -> null
}

data class SellerOrder(
    val id: String,
    val customerName: String,
    val items: String,
    val total: Int,
    val status: OrderStatus
)

val dummySellerOrders = listOf(
    SellerOrder("TS-20240920-001", "Bunda Sari", "Bubur Ayam Sayuran x2", 50000, OrderStatus.NEW),
    SellerOrder("TS-20240920-002", "Mama Rara", "Tim Ikan Salmon x1", 35000, OrderStatus.COOKING),
    SellerOrder("TS-20240920-003", "Ibu Dewi", "Puree Wortel x3", 60000, OrderStatus.DONE),
    SellerOrder("TS-20240920-004", "Kak Anisa", "Nasi Lembek Ayam x1, Bubur Kacang Hijau x1", 46000, OrderStatus.NEW),
    SellerOrder("TS-20240920-005", "Mbak Tiara", "Nasi Tim Hati Ayam x2", 60000, OrderStatus.SHIPPING),
    SellerOrder("TS-20240919-006", "Ibu Maya", "Bubur Ayam Sayuran x1", 25000, OrderStatus.DONE),
)

@Composable
fun SellerOrdersScreen(
    orders: List<SellerOrder>,
    onBack: () -> Unit,
    onUpdateStatus: (String, OrderStatus) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var orderToReject by remember { mutableStateOf<SellerOrder?>(null) }

    val filterOptions: List<OrderStatus?> = listOf(null) + OrderStatus.values().toList()
    val filteredOrders = if (selectedFilter == null) {
        orders
    } else {
        orders.filter { it.status == selectedFilter }
    }
    val newCount = orders.count { it.status == OrderStatus.NEW }

    orderToReject?.let { order ->
        AlertDialog(
            onDismissRequest = { orderToReject = null },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(order.id, OrderStatus.REJECTED)
                        orderToReject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Tolak", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToReject = null }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            title = {
                Text("Tolak pesanan?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    text = "Pesanan dari ${order.customerName} akan ditolak dan tidak bisa dikembalikan.",
                    color = TextSecondary
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
    ) {
        SellerHeader(
            title = "Pesanan",
            subtitle = "${orders.size} pesanan • $newCount baru",
            onBack = onBack
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { option ->
                val count = if (option == null) orders.size
                else orders.count { it.status == option }
                CategoryChip(
                    text = "${option?.label ?: "Semua"} ($count)",
                    isSelected = selectedFilter == option,
                    onClick = { selectedFilter = option }
                )
            }
        }

        if (filteredOrders.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "📋", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tidak ada pesanan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Belum ada pesanan dengan status ini",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    SellerOrderCard(
                        order = order,
                        onAdvance = { newStatus -> onUpdateStatus(order.id, newStatus) },
                        onReject = { orderToReject = order }
                    )
                }
            }
        }
    }
}

@Composable
private fun SellerOrderCard(
    order: SellerOrder,
    onAdvance: (OrderStatus) -> Unit,
    onReject: () -> Unit
) {
    val actionLabel = order.status.actionLabel

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = order.id,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                OrderStatusBadge(status = order.status)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = BorderOrange
            )

            order.items.split(", ").forEach { item ->
                Text(
                    text = "• $item",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Total", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = "Rp ${order.total}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            }

            if (actionLabel != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = BorderOrange
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.status == OrderStatus.NEW) {
                        ActionButton(
                            text = "Tolak",
                            color = Color.Red,
                            onClick = onReject,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    PrimaryAction(
                        text = actionLabel,
                        onClick = { order.status.next()?.let(onAdvance) },
                        modifier = Modifier.weight(if (order.status == OrderStatus.NEW) 2f else 1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderStatusBadge(status: OrderStatus) {
    Box(
        modifier = Modifier
            .background(
                Color(status.color).copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(status.color)
        )
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(OrangePrimary)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}