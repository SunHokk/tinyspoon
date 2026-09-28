package id.tinyspoon.app.ui.screens.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.theme.*

data class OrderStatus(
    val step: Int,
    val title: String,
    val description: String,
    val time: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

@Composable
fun OrderTrackingScreen(
    orderId: String = "TS-20240920-001",
    onBack: () -> Unit
) {
    val orderStatuses = listOf(
        OrderStatus(1, "Pesanan Dibuat", "Pesanan kamu telah berhasil dibuat", "09:00", true, false),
        OrderStatus(2, "Pesanan Dikonfirmasi", "Seller sedang mempersiapkan pesananmu", "09:05", true, false),
        OrderStatus(3, "Sedang Dimasak", "MPASI si kecil sedang dimasak dengan penuh kasih", "09:15", true, true),
        OrderStatus(4, "Dalam Pengiriman", "Pesanan sedang dalam perjalanan ke rumahmu", "", false, false),
        OrderStatus(5, "Pesanan Tiba", "Selamat menikmati! Jangan lupa beri ulasan", "", false, false),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrangePrimary)
                .padding(16.dp)
                .padding(top = 24.dp)
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
                Column {
                    Text(
                        text = "Lacak Pesanan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ID: $orderId",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🍳", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sedang Dimasak",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                    Text(
                        text = "MPASI si kecil sedang dimasak dengan penuh kasih",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Status Pesanan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    orderStatuses.forEachIndexed { index, status ->
                        OrderTimelineItem(
                            status = status,
                            isLast = index == orderStatuses.size - 1
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Info Pengiriman",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "Kurir", value = "TinySpoon Express 🛵")
                    InfoRow(label = "Estimasi", value = "15-30 menit")
                    InfoRow(label = "Seller", value = "Dapur Bunda")
                }
            }
        }
    }
}

@Composable
fun OrderTimelineItem(
    status: OrderStatus,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = when {
                            status.isCompleted || status.isCurrent -> OrangePrimary
                            else -> Color(0xFFE0E0E0)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (status.isCompleted && !status.isCurrent) "✓" else "${status.step}",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(
                            if (status.isCompleted) OrangePrimary else Color(0xFFE0E0E0)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f).padding(bottom = if (!isLast) 16.dp else 0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = status.title,
                    fontSize = 14.sp,
                    fontWeight = if (status.isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (status.isCompleted || status.isCurrent) TextPrimary else TextSecondary
                )
                if (status.time.isNotEmpty()) {
                    Text(
                        text = status.time,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            if (status.isCompleted || status.isCurrent) {
                Text(
                    text = status.description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}