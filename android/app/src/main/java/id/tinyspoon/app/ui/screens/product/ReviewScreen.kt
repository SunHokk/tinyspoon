package id.tinyspoon.app.ui.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class Review(
    val userName: String,
    val rating: Int,
    val comment: String,
    val date: String,
    val babyAge: String
)

val dummyReviews = listOf(
    Review("Bunda Sari", 5, "MPASI-nya enak banget! Anak saya langsung suka dan habis 1 porsi. Teksturnya juga pas untuk usia 7 bulan.", "20 Sep 2024", "7 bulan"),
    Review("Mama Rara", 5, "Bahan-bahannya segar dan bergizi. Packaging juga rapi dan higienis. Recommended banget!", "18 Sep 2024", "8 bulan"),
    Review("Ibu Dewi", 4, "Rasanya enak dan anak saya suka. Tapi pengirimannya agak lama, semoga bisa lebih cepat.", "15 Sep 2024", "6 bulan"),
    Review("Bunda Tika", 5, "Udah berlangganan 3 bulan, selalu puas! Nutrisinya lengkap dan anak jadi lebih sehat.", "10 Sep 2024", "9 bulan"),
    Review("Mama Cici", 4, "Produknya bagus dan terpercaya karena ada sertifikat BPOM-nya. Harga juga wajar.", "5 Sep 2024", "7 bulan"),
)

@Composable
fun ReviewScreen(
    productName: String,
    onBack: () -> Unit,
    onSubmitReview: () -> Unit
) {
    var showReviewForm by remember { mutableStateOf(false) }
    var userRating by remember { mutableStateOf(0) }
    var userComment by remember { mutableStateOf("") }
    var commentError by remember { mutableStateOf<String?>(null) }

    val averageRating = dummyReviews.map { it.rating }.average()

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
                Column {
                    Text(
                        text = "Ulasan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = productName,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
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
                        Text(
                            text = String.format("%.1f", averageRating),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { index ->
                                Text(
                                    text = if (index < averageRating.toInt()) "⭐" else "☆",
                                    fontSize = 20.sp
                                )
                            }
                        }
                        Text(
                            text = "${dummyReviews.size} ulasan",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showReviewForm = !showReviewForm },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (showReviewForm) "Tutup Form" else "✍️ Tulis Ulasan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (showReviewForm) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Tulis Ulasanmu",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = "Rating",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(5) { index ->
                                    Text(
                                        text = if (index < userRating) "⭐" else "☆",
                                        fontSize = 32.sp,
                                        modifier = Modifier.clickable { userRating = index + 1 }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = userComment,
                                onValueChange = {
                                    userComment = it
                                    commentError = null
                                },
                                label = { Text("Ceritakan pengalamanmu...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                isError = commentError != null,
                                colors = tinySpoonFieldColors(),
                                minLines = 3
                            )
                            if (commentError != null) {
                                Text(
                                    text = commentError!!,
                                    color = Color.Red,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    when {
                                        userRating == 0 -> commentError = "Pilih rating dulu!"
                                        userComment.isBlank() -> commentError = "Tulis ulasan dulu!"
                                        else -> {
                                            showReviewForm = false
                                            onSubmitReview()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Kirim Ulasan",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Semua Ulasan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(dummyReviews) { review ->
                ReviewCard(review = review)
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OrangePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.userName.first().toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = review.userName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Bayi ${review.babyAge}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = review.date,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(review.rating) {
                    Text(text = "⭐", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = review.comment,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}