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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
    val allergens: List<String> = emptyList(),
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val fiber: Int = 0
)

val Product.hasNutrition: Boolean
    get() = protein + carbs + fat + fiber > 0

val ageCategories = listOf("Semua", "6-8 bulan", "9-11 bulan", "12+ bulan")
val textureCategories = listOf("Semua", "Halus", "Lembut", "Kasar")
val allergenOptions = listOf("Susu", "Telur", "Kacang", "Gluten", "Seafood")
val nutritionOptions = listOf("Tinggi Protein", "Tinggi Serat", "Rendah Lemak")

fun Product.matchesNutrition(option: String): Boolean = hasNutrition && when (option) {
    "Tinggi Protein" -> protein >= 8
    "Tinggi Serat" -> fiber >= 4
    "Rendah Lemak" -> fat <= 2
    else -> true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    products: List<Product>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onProductClick: (Product) -> Unit,
    onFavoritesClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedTexture by remember { mutableStateOf("Semua") }
    var selectedNutrition by remember { mutableStateOf(setOf<String>()) }
    var excludedAllergens by remember { mutableStateOf(setOf<String>()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val activeFilterCount = (if (selectedCategory != "Semua") 1 else 0) +
            (if (selectedTexture != "Semua") 1 else 0) +
            selectedNutrition.size +
            excludedAllergens.size
    val isFilterActive = activeFilterCount > 0

    fun resetFilters() {
        selectedCategory = "Semua"
        selectedTexture = "Semua"
        selectedNutrition = emptySet()
        excludedAllergens = emptySet()
    }

    val filteredProducts = products.filter { product ->
        val matchSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                product.sellerName.contains(searchQuery, ignoreCase = true)
        val matchCategory = selectedCategory == "Semua" || product.ageGroup == selectedCategory
        val matchTexture = selectedTexture == "Semua" || product.texture == selectedTexture
        val matchNutrition = selectedNutrition.all { product.matchesNutrition(it) }
        val matchAllergen = excludedAllergens.none { it in product.allergens }
        matchSearch && matchCategory && matchTexture && matchNutrition && matchAllergen
    }

    if (showFilterSheet) {
        FilterSheet(
            initialCategory = selectedCategory,
            initialTexture = selectedTexture,
            initialNutrition = selectedNutrition,
            initialAllergens = excludedAllergens,
            onApply = { category, texture, nutrition, allergens ->
                selectedCategory = category
                selectedTexture = texture
                selectedNutrition = nutrition
                excludedAllergens = allergens
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { onFavoritesClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "❤️", fontSize = 20.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { onProfileClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "👤", fontSize = 20.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari MPASI...", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable { showFilterSheet = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filter",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                            if (isFilterActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(OrangePrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$activeFilterCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isFilterActive) {
            item {
                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCategory != "Semua") {
                        item { ActiveFilterChip(selectedCategory) { selectedCategory = "Semua" } }
                    }
                    if (selectedTexture != "Semua") {
                        item { ActiveFilterChip(selectedTexture) { selectedTexture = "Semua" } }
                    }
                    items(selectedNutrition.toList()) { option ->
                        ActiveFilterChip(option) { selectedNutrition = selectedNutrition - option }
                    }
                    items(excludedAllergens.toList()) { allergen ->
                        ActiveFilterChip("Tanpa $allergen") {
                            excludedAllergens = excludedAllergens - allergen
                        }
                    }
                    item {
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

        if (isLoading && products.isEmpty()) {
            item { LoadingProducts() }
        } else if (errorMessage != null && products.isEmpty()) {
            item { ErrorProducts(message = errorMessage, onRetry = onRetry) }
        } else if (filteredProducts.isEmpty()) {
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
fun ActiveFilterChip(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(OrangePrimary)
            .clickable { onRemove() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "✕", fontSize = 11.sp, color = Color.White)
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    initialCategory: String,
    initialTexture: String,
    initialNutrition: Set<String>,
    initialAllergens: Set<String>,
    onApply: (String, String, Set<String>, Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(initialCategory) }
    var texture by remember { mutableStateOf(initialTexture) }
    var nutrition by remember { mutableStateOf(initialNutrition) }
    var allergens by remember { mutableStateOf(initialAllergens) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BackgroundCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Reset",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            category = "Semua"
                            texture = "Semua"
                            nutrition = emptySet()
                            allergens = emptySet()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            FilterSectionTitle("Kategori Usia")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ageCategories.forEach { item ->
                    CategoryChip(item, category == item) { category = item }
                }
            }

            FilterSectionTitle("Tekstur")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                textureCategories.forEach { item ->
                    CategoryChip(item, texture == item) { texture = item }
                }
            }

            FilterSectionTitle("Kandungan Gizi")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nutritionOptions.forEach { option ->
                    val selected = option in nutrition
                    CategoryChip(option, selected) {
                        nutrition = if (selected) nutrition - option else nutrition + option
                    }
                }
            }

            FilterSectionTitle("Bebas Alergen")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allergenOptions.forEach { allergen ->
                    val selected = allergen in allergens
                    CategoryChip("Tanpa $allergen", selected) {
                        allergens = if (selected) allergens - allergen else allergens + allergen
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onApply(category, texture, nutrition, allergens) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text(
                    text = "Terapkan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
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

@Composable
fun LoadingProducts() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = OrangePrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Memuat produk...", fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
fun ErrorProducts(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📡", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .border(1.dp, OrangePrimary, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Coba Lagi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
        }
    }
}