package id.tinyspoon.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.screens.auth.SellerStatus
import id.tinyspoon.app.ui.screens.auth.UserAccount
import id.tinyspoon.app.ui.screens.auth.UserRole
import id.tinyspoon.app.ui.screens.home.Product
import id.tinyspoon.app.ui.screens.product.Review
import id.tinyspoon.app.ui.screens.seller.OrderStatus
import id.tinyspoon.app.ui.screens.seller.SellerMenuCard
import id.tinyspoon.app.ui.screens.seller.SellerOrder
import id.tinyspoon.app.ui.screens.seller.SellerStatCard
import id.tinyspoon.app.ui.theme.*
import java.util.Locale

private fun formatAdminRupiah(amount: Int): String = when {
    amount >= 1_000_000 -> "Rp ${"%.1f".format(Locale.US, amount / 1_000_000.0)}jt"
    amount >= 1_000 -> "Rp ${amount / 1_000}K"
    else -> "Rp $amount"
}

@Composable
fun AdminDashboardScreen(
    accounts: List<UserAccount>,
    products: List<Product>,
    orders: List<SellerOrder>,
    reviews: List<Review>,
    onOpenVerification: () -> Unit,
    onOpenUsers: () -> Unit,
    onLogout: () -> Unit
) {
    val buyers = accounts.count { it.role == UserRole.BUYER }
    val sellers = accounts.count { it.role == UserRole.SELLER }
    val pending = accounts.count {
        it.role == UserRole.SELLER && it.sellerStatus == SellerStatus.PENDING
    }
    val inactive = accounts.count { !it.isActive }
    val doneOrders = orders.filter { it.status == OrderStatus.DONE }
    val revenue = doneOrders.sumOf { it.total }
    val averageRating =
        if (reviews.isEmpty()) 0.0 else reviews.sumOf { it.rating }.toDouble() / reviews.size
    val topSellers = doneOrders
        .groupBy { it.sellerName }
        .map { (shop, list) -> shop to list.sumOf { it.total } }
        .sortedByDescending { it.second }
        .take(3)

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
            Column {
                Text(
                    text = "Dashboard Admin",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "TinySpoon",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Ringkasan Platform",
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
                        emoji = "👪",
                        value = buyers.toString(),
                        label = "Pembeli"
                    )
                    SellerStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🏪",
                        value = sellers.toString(),
                        label = "Penjual"
                    )
                    SellerStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🍱",
                        value = products.size.toString(),
                        label = "Produk"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SellerStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📦",
                        value = orders.size.toString(),
                        label = "Pesanan"
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
                        value = formatAdminRupiah(revenue),
                        label = "Transaksi Selesai"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SellerStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "💬",
                        value = reviews.size.toString(),
                        label = "Ulasan"
                    )
                    SellerStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "⭐",
                        value = String.format(Locale.US, "%.1f", averageRating),
                        label = "Rata-rata Rating"
                    )
                    SellerStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🚫",
                        value = inactive.toString(),
                        label = "Akun Nonaktif"
                    )
                }
            }

            item {
                Text(
                    text = "Kelola",
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
                        emoji = "📜",
                        title = "Verifikasi Seller",
                        subtitle = if (pending > 0) "$pending menunggu" else "Tidak ada antrean",
                        onClick = onOpenVerification
                    )
                    SellerMenuCard(
                        modifier = Modifier.weight(1f),
                        emoji = "👥",
                        title = "Kelola Pengguna",
                        subtitle = "${accounts.size} akun",
                        onClick = onOpenUsers
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pesanan per Status",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OrderStatus.values().forEach { status ->
                                val count = orders.count { it.status == status }
                                StatusBarRow(
                                    label = status.label,
                                    count = count,
                                    total = orders.size,
                                    color = Color(status.color)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Seller Teratas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Berdasarkan transaksi selesai",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (topSellers.isEmpty()) {
                            Text(
                                text = "Belum ada transaksi selesai",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                topSellers.forEachIndexed { index, (shop, amount) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(OrangePrimaryLight, RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OrangePrimary
                                            )
                                        }
                                        Text(
                                            text = shop,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatAdminRupiah(amount),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OrangePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Keluar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
private fun StatusBarRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val fraction = if (total == 0) 0f else (count.toFloat() / total).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.width(64.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFEEEEEE))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
        Text(
            text = count.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.width(24.dp)
        )
    }
}