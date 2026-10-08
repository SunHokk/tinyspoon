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
import id.tinyspoon.app.ui.screens.home.Product
import id.tinyspoon.app.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

data class Review(
    val id: String,
    val productId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val date: String,
    val babyAge: String
)

private val sampleReviews = listOf(
    Review("", "", "Bunda Sari", 5, "MPASI-nya enak banget! Anak saya langsung suka dan habis 1 porsi. Teksturnya juga pas untuk usia 7 bulan.", "20 Sep 2024", "7 bulan"),
    Review("", "", "Mama Rara", 5, "Bahan-bahannya segar dan bergizi. Packaging juga rapi dan higienis. Recommended banget!", "18 Sep 2024", "8 bulan"),
    Review("", "", "Ibu Dewi", 4, "Rasanya enak dan anak saya suka. Tapi pengirimannya agak lama, semoga bisa lebih cepat.", "15 Sep 2024", "6 bulan"),
    Review("", "", "Bunda Tika", 5, "Udah berlangganan 3 bulan, selalu puas! Nutrisinya lengkap dan anak jadi lebih sehat.", "10 Sep 2024", "9 bulan"),
    Review("", "", "Mama Cici", 4, "Produknya bagus dan terpercaya karena ada sertifikat BPOM-nya. Harga juga wajar.", "5 Sep 2024", "7 bulan"),
)

fun seedReviewsFor(products: List<Product>): List<Review> =
    products.flatMap { product ->
        val fives = ((product.rating - 4f) * sampleReviews.size).roundToInt()
            .coerceIn(0, sampleReviews.size)
        sampleReviews.mapIndexed { index, sample ->
            sample.copy(
                id = "seed-${product.id}-$index",
                productId = product.id,
                rating = if (index < fives) 5 else 4
            )
        }
    }

fun List<Review>.averageRating(): Float =
    if (isEmpty()) 0f else sumOf { it.rating }.toFloat() / size

fun withRatingsFrom(products: List<Product>, reviews: List<Review>): List<Product> =
    products.map { product ->
        val own = reviews.filter { it.productId == product.id }
        if (own.isEmpty()) product
        else product.copy(rating = (own.averageRating() * 10).roundToInt() / 10f)
    }

@Composable
fun ReviewScreen(
    productName: String,
    reviews: List<Review>,
    onBack: () -> Unit,
    onSubmitReview: (rating: Int, comment: String) -> Unit
) {
    var showReviewForm by remember { mutableStateOf(false) }
    var userRating by remember { mutableStateOf(0) }
    var userComment by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val averageRating = reviews.averageRating()

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", averageRating),
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
                            text = "${reviews.size} ulasan",
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
                                        modifier = Modifier.clickable {
                                            userRating = index + 1
                                            formError = null
                                        }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = userComment,
                                onValueChange = {
                                    userComment = it
                                    formError = null
                                },
                                label = { Text("Ceritakan pengalamanmu...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                isError = formError != null,
                                colors = tinySpoonFieldColors(),
                                minLines = 3
                            )
                            if (formError != null) {
                                Text(
                                    text = formError!!,
                                    color = Color.Red,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    when {
                                        userRating == 0 -> formError = "Pilih rating dulu!"
                                        userComment.isBlank() -> formError = "Tulis ulasan dulu!"
                                        else -> {
                                            onSubmitReview(userRating, userComment.trim())
                                            showReviewForm = false
                                            userRating = 0
                                            userComment = ""
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

            if (reviews.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "💬", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada ulasan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Jadilah yang pertama menulis ulasan untuk produk ini",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Semua Ulasan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(reviews, key = { it.id }) { review ->
                    ReviewCard(review = review)
                }
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
                        if (review.babyAge.isNotBlank()) {
                            Text(
                                text = "Bayi ${review.babyAge}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
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