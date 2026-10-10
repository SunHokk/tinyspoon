package id.tinyspoon.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.screens.auth.SellerStatus
import id.tinyspoon.app.ui.screens.auth.UserAccount
import id.tinyspoon.app.ui.screens.auth.UserRole
import id.tinyspoon.app.ui.screens.seller.SellerStatCard
import id.tinyspoon.app.ui.theme.*

@Composable
fun AdminDashboardScreen(
    accounts: List<UserAccount>,
    onLogout: () -> Unit
) {
    val buyers = accounts.count { it.role == UserRole.BUYER }
    val sellers = accounts.count { it.role == UserRole.SELLER }
    val pending = accounts.count { it.sellerStatus == SellerStatus.PENDING }

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
                    text = "Ringkasan Akun",
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
                        emoji = "⏳",
                        value = pending.toString(),
                        label = "Menunggu Verifikasi"
                    )
                }
            }

            item {
                Text(
                    text = "Akun Terdaftar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(accounts, key = { it.id }) { account ->
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
                                text = account.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = account.email,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            if (account.shopName != null) {
                                Text(
                                    text = "Toko: ${account.shopName}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(OrangePrimaryLight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = account.sellerStatus?.let { "${account.role.label} • ${it.label}" }
                                    ?: account.role.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
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