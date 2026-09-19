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

@Composable
fun ProductDetailScreen(
    product: Product,
    onBack: () -> Unit,
    onAddToCart: () -> Unit,
    onSellerClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCream)
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(OrangePrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.imageEmoji,
                    fontSize = 120.sp
                )

                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onBack() }
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = product.sellerName,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "Rp ${product.price}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(5) { index ->
                        Text(
                            text = if (index < product.rating.toInt()) "⭐" else "☆",
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "${product.rating} / 5.0",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailChip(text = "👶 ${product.ageGroup}")
                    DetailChip(text = "🥣 ${product.texture}")
                }

                HorizontalDivider(color = BorderOrange)

                Text(
                    text = "Informasi Nutrisi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NutritionItem(label = "Protein", value = "5g")
                    NutritionItem(label = "Karbohidrat", value = "20g")
                    NutritionItem(label = "Lemak", value = "3g")
                    NutritionItem(label = "Serat", value = "2g")
                }

                HorizontalDivider(color = BorderOrange)

                Text(
                    text = "Deskripsi Produk",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Makanan bayi bergizi tinggi yang dibuat dari bahan-bahan segar pilihan. Cocok untuk bayi usia ${product.ageGroup} dengan tekstur ${product.texture.lowercase()} yang mudah dicerna. Diproses dengan standar kebersihan tinggi dan telah mendapat sertifikasi kelayakan pangan.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )

                HorizontalDivider(color = BorderOrange)

                Text(
                    text = "Info Seller",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = OrangePrimaryLight,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(OrangePrimary, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = product.sellerName.first().toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = product.sellerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onSellerClick() }
                        ) {
                            Text(text = "✅", fontSize = 12.sp)
                            Text(
                                text = " Seller Tersertifikasi • Lihat Sertifikat ➡",
                                fontSize = 12.sp,
                                color = OrangePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        Button(
            onClick = onAddToCart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-16).dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "🛒 Tambah ke Keranjang",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun DetailChip(text: String) {
    Box(
        modifier = Modifier
            .background(OrangePrimaryLight, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 13.sp, color = OrangePrimary)
    }
}

@Composable
fun NutritionItem(label: String, value: String) {
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