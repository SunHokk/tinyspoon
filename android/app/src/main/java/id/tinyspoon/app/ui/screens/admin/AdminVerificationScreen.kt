package id.tinyspoon.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import id.tinyspoon.app.ui.screens.auth.SellerStatus
import id.tinyspoon.app.ui.screens.auth.UserAccount
import id.tinyspoon.app.ui.screens.auth.UserRole
import id.tinyspoon.app.ui.screens.product.CertDetail
import id.tinyspoon.app.ui.screens.product.SellerCertificate
import id.tinyspoon.app.ui.screens.seller.ActionButton
import id.tinyspoon.app.ui.screens.seller.SellerHeader
import id.tinyspoon.app.ui.theme.*

@Composable
fun AdminVerificationScreen(
    accounts: List<UserAccount>,
    certificates: Map<String, SellerCertificate>,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit,
    onViewDocument: (String) -> Unit,
    onBack: () -> Unit
) {
    val sellers = accounts.filter { it.role == UserRole.SELLER }
    val pending = sellers.filter { it.sellerStatus == SellerStatus.PENDING }
    val processed = sellers.filter {
        it.sellerStatus == SellerStatus.APPROVED || it.sellerStatus == SellerStatus.REJECTED
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
        SellerHeader(
            title = "Verifikasi Seller",
            subtitle = "${pending.size} menunggu",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Menunggu (${pending.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (pending.isEmpty()) {
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
                items(pending, key = { "pending-${it.id}" }) { account ->
                    val certificate = account.shopName?.let { certificates[it] }
                    if (certificate != null) {
                        VerificationCard(
                            account = account,
                            certificate = certificate,
                            onApprove = { onApprove(account.id) },
                            onReject = { rejectTarget = account },
                            onViewDocument = { account.shopName?.let(onViewDocument) }
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
                    text = "Sudah Diproses (${processed.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(processed, key = { "done-${it.id}" }) { account ->
                val certificate = account.shopName?.let { certificates[it] }
                val approved = account.sellerStatus == SellerStatus.APPROVED
                val statusColor = if (approved) Color(0xFF4CAF50) else Color(0xFFE53935)

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
                                text = account.shopName ?: account.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Pemilik: ${account.name}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            if (certificate != null) {
                                Text(
                                    text = certificate.certNumber,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            if (!approved && !account.rejectionReason.isNullOrBlank()) {
                                Text(
                                    text = "Alasan: ${account.rejectionReason}",
                                    fontSize = 11.sp,
                                    color = statusColor
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = account.sellerStatus?.label ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationCard(
    account: UserAccount,
    certificate: SellerCertificate,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewDocument: () -> Unit
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

            Spacer(modifier = Modifier.height(4.dp))
            ActionButton(
                text = "Lihat Dokumen",
                color = OrangePrimary,
                onClick = onViewDocument,
                modifier = Modifier.fillMaxWidth()
            )

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