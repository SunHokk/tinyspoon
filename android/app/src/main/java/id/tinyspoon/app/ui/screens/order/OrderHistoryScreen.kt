package id.tinyspoon.app.ui.screens.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.screens.seller.OrderStatus
import id.tinyspoon.app.ui.screens.seller.SellerOrder
import id.tinyspoon.app.ui.theme.*

data class OrderHistory(
    val orderId: String,
    val date: String,
    val items: List<String>,
    val total: Int,
    val status: String,
    val statusColor: Long,
    val canTrack: Boolean = false
)

val dummyOrderHistory = listOf(
    OrderHistory(
        orderId = "TS-20240920-001",
        date = "20 September 2024",
        items = listOf("Bubur Ayam Sayuran x1", "Puree Wortel Kentang x2"),
        total = 65000,
        status = "Selesai",
        statusColor = 0xFF4CAF50
    ),
    OrderHistory(
        orderId = "TS-20240918-002",
        date = "18 September 2024",
        items = listOf("Tim Ikan Salmon x1"),
        total = 35000,
        status = "Selesai",
        statusColor = 0xFF4CAF50
    ),
    OrderHistory(
        orderId = "TS-20240915-003",
        date = "15 September 2024",
        items = listOf("Nasi Lembek Ayam x1", "Bubur Kacang Hijau x1"),
        total = 46000,
        status = "Dibatalkan",
        statusColor = 0xFFE53935
    ),
    OrderHistory(
        orderId = "TS-20240910-004",
        date = "10 September 2024",
        items = listOf("Sup Sayuran Mix x2"),
        total = 44000,
        status = "Selesai",
        statusColor = 0xFF4CAF50
    ),
)

private fun SellerOrder.toHistory(): OrderHistory = OrderHistory(
    orderId = id,
    date = date,
    items = items.split(", "),
    total = total + deliveryFee,
    status = when (status) {
        OrderStatus.NEW -> "Menunggu"
        OrderStatus.COOKING -> "Dimasak"
        OrderStatus.SHIPPING -> "Dikirim"
        OrderStatus.DONE -> "Selesai"
        OrderStatus.REJECTED -> "Ditolak"
    },
    statusColor = status.color,
    canTrack = status == OrderStatus.NEW ||
            status == OrderStatus.COOKING ||
            status == OrderStatus.SHIPPING
)

@Composable
fun OrderHistoryScreen(
    orders: List<SellerOrder>,
    onBack: () -> Unit,
    onTrackOrder: (String) -> Unit
) {
    val allOrders = orders.map { it.toHistory() } + dummyOrderHistory

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrangePrimary)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Riwayat Pesanan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (allOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "📋", fontSize = 64.sp)
                    Text(
                        text = "Belum ada pesanan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Yuk pesan MPASI untuk si kecil!",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allOrders) { order ->
                    OrderHistoryCard(
                        order = order,
                        onTrackOrder = { onTrackOrder(order.orderId) }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderHistoryCard(
    order: OrderHistory,
    onTrackOrder: () -> Unit
) {
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
                Column {
                    Text(
                        text = order.orderId,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = order.date,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            Color(order.statusColor).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = order.status,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(order.statusColor)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = BorderOrange
            )

            order.items.forEach { item ->
                Text(
                    text = "• $item",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = BorderOrange
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Rp ${order.total}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }

                if (order.canTrack) {
                    Button(
                        onClick = onTrackOrder,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text(
                            text = "Lacak Pesanan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else if (order.status == "Selesai") {
                    OutlinedButton(
                        onClick = onTrackOrder,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = OrangePrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary)
                    ) {
                        Text(
                            text = "Pesan Lagi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}