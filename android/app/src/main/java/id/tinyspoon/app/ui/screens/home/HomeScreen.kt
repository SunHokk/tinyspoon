package id.tinyspoon.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.theme.*

data class Product(
    val id: String,
    val name: String,
    val price: Int,
    val sellerName: String,
    val ageGroup: String,
    val texture: String,
    val rating: Float,
    val imageEmoji: String,
    val allergens: List<String> = emptyList()
)

val dummyProducts = listOf(
    Product("1", "Bubur Ayam Sayuran", 25000, "Dapur Bunda", "6-8 bulan", "Halus", 4.8f, "🍚", emptyList()),
    Product("2", "Puree Wortel Kentang", 20000, "MPASI Sehat", "6-8 bulan", "Halus", 4.6f, "🥕", listOf("Susu")),
    Product("3", "Tim Ikan Salmon", 35000, "Nutrisi Bayi", "9-11 bulan", "Lembut", 4.9f, "🐟", listOf("Seafood")),
    Product("4", "Nasi Lembek Ayam", 28000, "Dapur Bunda", "9-11 bulan", "Lembut", 4.7f, "🍗", listOf("Telur")),
    Product("5", "Bubur Kacang Hijau", 18000, "MPASI Sehat", "6-8 bulan", "Halus", 4.5f, "🌱", listOf("Kacang")),
    Product("6", "Sup Sayuran Mix", 22000, "Nutrisi Bayi", "12+ bulan", "Kasar", 4.8f, "🥦", listOf("Gluten")),
    Product("7", "Nasi Tim Hati Ayam", 30000, "Dapur Bunda", "9-11 bulan", "Lembut", 4.7f, "🍱", listOf("Telur")),
    Product("8", "Puree Labu Kuning", 19000, "MPASI Sehat", "6-8 bulan", "Halus", 4.6f, "🎃", listOf("Susu")),
)

val ageCategories = listOf("Semua", "6-8 bulan", "9-11 bulan", "12+ bulan")
val textureCategories = listOf("Semua", "Halus", "Lembut", "Kasar")
val allergenOptions = listOf("Susu", "Telur", "Kacang", "Gluten", "Seafood")

@Composable
fun HomeScreen(
    products: List<Product> = dummyProducts,
    onProductClick: (Product) -> Unit,
    onProfileClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedTexture by remember { mutableStateOf("Semua") }
    var excludedAllergens by remember { mutableStateOf(setOf<String>()) }

    val isFilterActive = selectedCategory != "Semua" ||
            selectedTexture != "Semua" ||
            excludedAllergens.isNotEmpty()

    fun resetFilters() {
        selectedCategory = "Semua"
        selectedTexture = "Semua"
        excludedAllergens = emptySet()
    }

    val filteredProducts = products.filter { product ->
        val matchSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                product.sellerName.contains(searchQuery, ignoreCase = true)
        val matchCategory = selectedCategory == "Semua" || product.ageGroup == selectedCategory
        val matchTexture = selectedTexture == "Semua" || product.texture == selectedTexture
        val matchAllergen = excludedAllergens.none { it in product.allergens }
        matchSearch && matchCategory && matchTexture && matchAllergen
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OrangePrimary)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📍 Jakarta Selatan",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Halo, Bunda! 👋",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable { onProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👤", fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari MPASI...", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            cursorColor = OrangePrimary
                        ),
                        singleLine = true
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        color = Color(0xFFFFE0CC),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Promo Hari Ini! 🎉",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Diskon 20% untuk\npesanan pertama",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Text(text = "🍼", fontSize = 40.sp)
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kategori Usia",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (isFilterActive) {
                        Text(
                            text = "Reset",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { resetFilters() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ageCategories) { category ->
                        CategoryChip(
                            text = category,
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tekstur",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(textureCategories) { texture ->
                        CategoryChip(
                            text = texture,
                            isSelected = selectedTexture == texture,
                            onClick = { selectedTexture = texture }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Bebas Alergen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allergenOptions) { allergen ->
                        val isSelected = allergen in excludedAllergens
                        CategoryChip(
                            text = "Tanpa $allergen",
                            isSelected = isSelected,
                            onClick = {
                                excludedAllergens = if (isSelected) {
                                    excludedAllergens - allergen
                                } else {
                                    excludedAllergens + allergen
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = if (isFilterActive || searchQuery.isNotBlank())
                    "Hasil (${filteredProducts.size})"
                else
                    "Produk Terdekat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredProducts.isEmpty()) {
            item {
                EmptyResult(
                    onReset = {
                        resetFilters()
                        searchQuery = ""
                    }
                )
            }
        } else {
            items(filteredProducts, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) OrangePrimary else Color.White,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (isSelected) Color.White else TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EmptyResult(onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🔍", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Produk tidak ditemukan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Coba ubah filter atau kata kunci pencarian",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .border(1.dp, OrangePrimary, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable { onReset() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Hapus Semua Filter",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OrangePrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(text = product.imageEmoji, fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.sellerName,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AgeChip(text = product.ageGroup)
                    AgeChip(text = product.texture)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rp ${product.price}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⭐", fontSize = 12.sp)
                        Text(
                            text = " ${product.rating}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgeChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = OrangePrimaryLight,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = OrangePrimary
        )
    }
}