package id.tinyspoon.app.ui.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import id.tinyspoon.app.ui.screens.home.Product
import id.tinyspoon.app.ui.theme.*
import id.tinyspoon.app.ui.screens.seller.ShopProfile
import java.util.Locale

data class SellerCertificate(
    val sellerName: String,
    val certNumber: String,
    val issuedBy: String,
    val issuedDate: String,
    val expiredDate: String,
    val certType: String,
    val status: CertStatus,
    val documentName: String = ""
)

enum class CertStatus {
    ACTIVE, EXPIRED, PENDING
}

@Composable
fun SellerCertificateScreen(
    sellerName: String,
    products: List<Product>,
    shop: ShopProfile? = null,
    certificate: SellerCertificate? = null,
    isLoading: Boolean = false,
    onBack: () -> Unit
) {
    val sellerProducts = products.filter { it.sellerName == sellerName }
    val scrollState = rememberScrollState()
    val ratedProducts = sellerProducts.filter { it.rating > 0f }
    val sellerRating = if (ratedProducts.isEmpty()) "-"
    else String.format(Locale.US, "%.1f⭐", ratedProducts.map { it.rating }.average())

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
                    text = "Profil Seller",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(OrangePrimary, RoundedCornerShape(36.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sellerName.first().toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = sellerName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        SellerStat(value = "${sellerProducts.size}", label = "Produk")
                        SellerStat(value = sellerRating, label = "Rating")
                    }
                }
            }

            if (isLoading && shop == null && certificate == null) {
                Text(
                    text = "Memuat info toko...",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (shop != null && shop.description.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🏪", fontSize = 24.sp)
                            Text(
                                text = "Info Toko",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = shop.description,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = BorderOrange)
                        Spacer(modifier = Modifier.height(12.dp))

                        CertDetail(label = "Alamat", value = shop.address)
                        CertDetail(label = "Jam Operasional", value = shop.openHours)
                        CertDetail(label = "Telepon", value = shop.phone)
                    }
                }
            }

            if (certificate != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "📜", fontSize = 24.sp)
                            Text(
                                text = "Sertifikat Resmi",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val statusColor = when (certificate.status) {
                            CertStatus.ACTIVE -> Color(0xFF4CAF50)
                            CertStatus.EXPIRED -> Color.Red
                            CertStatus.PENDING -> Color(0xFFFF9800)
                        }
                        val statusText = when (certificate.status) {
                            CertStatus.ACTIVE -> "✅ Aktif & Terverifikasi"
                            CertStatus.EXPIRED -> "❌ Kadaluarsa"
                            CertStatus.PENDING -> "⏳ Dalam Proses"
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    statusColor.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderOrange)
                        Spacer(modifier = Modifier.height(16.dp))

                        CertDetail(label = "Jenis Sertifikat", value = certificate.certType)
                        CertDetail(label = "Nomor Sertifikat", value = certificate.certNumber)
                        CertDetail(label = "Diterbitkan Oleh", value = certificate.issuedBy)
                        CertDetail(label = "Tanggal Terbit", value = certificate.issuedDate)
                        CertDetail(label = "Berlaku Hingga", value = certificate.expiredDate)
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
                        text = "Produk dari $sellerName",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    sellerProducts.forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(OrangePrimaryLight, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = product.imageEmoji, fontSize = 24.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Rp ${product.price}",
                                    fontSize = 12.sp,
                                    color = OrangePrimary
                                )
                            }
                            Text(
                                text = "⭐ ${product.rating}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        if (product != sellerProducts.last()) {
                            HorizontalDivider(color = BorderOrange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OrangePrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun CertDetail(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}