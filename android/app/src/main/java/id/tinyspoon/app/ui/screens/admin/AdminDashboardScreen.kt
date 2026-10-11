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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import id.tinyspoon.app.ui.screens.auth.SellerStatus
import id.tinyspoon.app.ui.screens.auth.UserAccount
import id.tinyspoon.app.ui.screens.auth.UserRole
import id.tinyspoon.app.ui.screens.product.CertDetail
import id.tinyspoon.app.ui.screens.product.SellerCertificate
import id.tinyspoon.app.ui.screens.seller.ActionButton
import id.tinyspoon.app.ui.screens.seller.SellerStatCard
import id.tinyspoon.app.ui.theme.*

@Composable
fun AdminDashboardScreen(
    accounts: List<UserAccount>,
    certificates: Map<String, SellerCertificate>,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val buyers = accounts.count { it.role == UserRole.BUYER }
    val sellers = accounts.count { it.role == UserRole.SELLER }
    val pendingSellers = accounts.filter {
        it.role == UserRole.SELLER && it.sellerStatus == SellerStatus.PENDING
    }
    var rejectTarget by remember { mutableStateOf<UserAccount?>(null) }

    rejectTarget?.let { account ->
        RejectDialog(
            shopName = account.shopName ?: account.name,
            onDismiss = { rejectTarget = null },
            onConfirm = { reason ->
                onReject(account.id, reason)
                rejectTarget = null
            }
        )
    }

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
                        value = pendingSellers.size.toString(),
                        label = "Menunggu Verifikasi"
                    )
                }
            }

            item {
                Text(
                    text = "Perlu Verifikasi (${pendingSellers.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (pendingSellers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "Tidak ada pengajuan yang menunggu verifikasi",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            } else {
                items(pendingSellers, key = { "pending-${it.id}" }) { account ->
                    val certificate = account.shopName?.let { certificates[it] }
                    if (certificate != null) {
                        VerificationCard(
                            account = account,
                            certificate = certificate,
                            onApprove = { onApprove(account.id) },
                            onReject = { rejectTarget = account }
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = account.shopName ?: account.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${account.name} • ${account.email}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Belum mengirim sertifikat",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF9800),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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

            items(accounts, key = { "acc-${it.id}" }) { account ->
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

@Composable
private fun VerificationCard(
    account: UserAccount,
    certificate: SellerCertificate,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = account.shopName ?: account.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Pemilik: ${account.name} • ${account.email}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = BorderOrange
            )

            CertDetail(label = "Jenis Sertifikat", value = certificate.certType)
            CertDetail(label = "Nomor Sertifikat", value = certificate.certNumber)
            CertDetail(label = "Diterbitkan Oleh", value = certificate.issuedBy)
            CertDetail(label = "Tanggal Terbit", value = certificate.issuedDate)
            CertDetail(label = "Berlaku Hingga", value = certificate.expiredDate)
            CertDetail(label = "Dokumen", value = "📎 ${certificate.documentName}")

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = BorderOrange
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    text = "Tolak",
                    color = Color.Red,
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4CAF50))
                        .clickable { onApprove() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Setujui",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RejectDialog(
    shopName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (reason.trim().length < 5) {
                        error = "Tulis alasan minimal 5 karakter"
                    } else {
                        onConfirm(reason.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Tolak", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        },
        title = {
            Text("Tolak \"$shopName\"?", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Alasan ini akan ditampilkan ke penjual supaya bisa memperbaiki dan mengirim ulang.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        error = null
                    },
                    label = { Text("Alasan penolakan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = error != null,
                    colors = tinySpoonFieldColors(),
                    minLines = 2
                )
                if (error != null) {
                    Text(text = error!!, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}