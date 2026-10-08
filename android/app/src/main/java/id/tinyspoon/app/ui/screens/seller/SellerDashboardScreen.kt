package id.tinyspoon.app.ui.screens.seller

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
import id.tinyspoon.app.ui.theme.*

private fun formatRupiahCompact(amount: Int): String = when {
    amount >= 1_000_000 -> "Rp ${"%.1f".format(amount / 1_000_000.0)}jt"
    amount >= 1_000 -> "Rp ${amount / 1_000}K"
    else -> "Rp $amount"
}

@Composable
fun SellerDashboardScreen(
    orders: List<SellerOrder>,
    onBack: () -> Unit,
    onManageProducts: () -> Unit,
    onManageOrders: () -> Unit,
    onSellerProfile: () -> Unit
) {
    val incomingCount = orders.count { it.status != OrderStatus.REJECTED }
    val doneOrders = orders.filter { it.status == OrderStatus.DONE }
    val revenue = doneOrders.sumOf { it.total }
    val newCount = orders.count { it.status == OrderStatus.NEW }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
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
                            text = "Dashboard Seller",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Dapur Bunda",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✅ Terverifikasi",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Ringkasan Hari Ini",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SellerStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📦",
                        value = incomingCount.toString(),
                        label = "Pesanan Masuk"
                    )
                    SellerStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "✅",
                        value = doneOrders.size.toString(),
                        label = "Selesai"
                    )
                    SellerStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "💰",
                        value = formatRupiahCompact(revenue),
                        label = "Pendapatan"
                    )
                }
            }

            item {
                Text(
                    text = "Menu Utama",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SellerMenuCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🍱",
                        title = "Produk Saya",
                        subtitle = "Kelola menu MPASI",
                        onClick = onManageProducts
                    )
                    SellerMenuCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📋",
                        title = "Pesanan",
                        subtitle = if (newCount > 0) "$newCount pesanan baru"
                        else "Lihat & proses pesanan",
                        onClick = onManageOrders
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pesanan Terbaru",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Lihat Semua →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        modifier = Modifier.clickable { onManageOrders() }
                    )
                }
            }

            items(orders.take(3), key = { it.id }) { order ->
                SellerOrderItem(
                    orderId = order.id,
                    customerName = order.customerName,
                    items = order.items,
                    total = order.total,
                    status = order.status.label,
                    statusColor = order.status.color
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "📜", fontSize = 32.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sertifikat BPOM Aktif",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "Berlaku hingga 31 Desember 2025",
                                fontSize = 12.sp,
                                color = Color(0xFF388E3C)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerStatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SellerMenuCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(OrangePrimaryLight, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SellerOrderItem(
    orderId: String,
    customerName: String,
    items: String,
    total: Int,
    status: String,
    statusColor: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customerName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = items,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Rp $total",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        Color(statusColor).copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(statusColor)
                )
            }
        }
    }
}