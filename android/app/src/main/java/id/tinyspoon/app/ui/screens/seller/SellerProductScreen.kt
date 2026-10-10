package id.tinyspoon.app.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import id.tinyspoon.app.ui.screens.home.AgeChip
import id.tinyspoon.app.ui.screens.home.CategoryChip
import id.tinyspoon.app.ui.screens.home.Product
import id.tinyspoon.app.ui.screens.home.ageCategories
import id.tinyspoon.app.ui.screens.home.allergenOptions
import id.tinyspoon.app.ui.screens.home.textureCategories
import id.tinyspoon.app.ui.theme.*
import java.util.UUID

@Composable
fun SellerProductsScreen(
    sellerName: String,
    products: List<Product>,
    onBack: () -> Unit,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val myProducts = products.filter { it.sellerName == sellerName }

    BackHandler(enabled = showForm) {
        showForm = false
        editingProduct = null
    }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(product.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            title = {
                Text("Hapus produk?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    text = "${product.name} akan dihapus dari daftar produkmu.",
                    color = TextSecondary
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showForm) {
        ProductForm(
            initial = editingProduct,
            sellerName = sellerName,
            onBack = {
                showForm = false
                editingProduct = null
            },
            onSubmit = { saved ->
                onSaveProduct(saved)
                showForm = false
                editingProduct = null
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCream)
        ) {
            SellerHeader(
                title = "Produk Saya",
                subtitle = "$sellerName • ${myProducts.size} produk",
                onBack = onBack
            )

            if (myProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "🍽️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada produk",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tambahkan menu MPASI pertamamu",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myProducts, key = { it.id }) { product ->
                        SellerProductItem(
                            product = product,
                            onEdit = {
                                editingProduct = product
                                showForm = true
                            },
                            onDelete = { productToDelete = product }
                        )
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
                    onClick = {
                        editingProduct = null
                        showForm = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "+ Tambah Produk",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SellerHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit
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
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SellerProductItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        color = TextPrimary
                    )
                    Text(
                        text = "Rp ${product.price}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AgeChip(text = product.ageGroup)
                        AgeChip(text = product.texture)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (product.allergens.isEmpty()) "Tanpa alergen umum"
                        else "Alergen: ${product.allergens.joinToString(", ")}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = BorderOrange
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    text = "Edit",
                    color = OrangePrimary,
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "Hapus",
                    color = Color.Red,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(1.dp, color, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun NutritionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(3)) },
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = tinySpoonFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
private fun ProductForm(
    initial: Product?,
    sellerName: String,
    onBack: () -> Unit,
    onSubmit: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var priceText by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var emoji by remember { mutableStateOf(initial?.imageEmoji ?: "🍲") }
    var ageGroup by remember { mutableStateOf(initial?.ageGroup ?: ageCategories[1]) }
    var texture by remember { mutableStateOf(initial?.texture ?: textureCategories[1]) }
    var allergens by remember { mutableStateOf(initial?.allergens?.toSet() ?: emptySet()) }
    var proteinText by remember { mutableStateOf(initial?.protein?.toString() ?: "") }
    var carbsText by remember { mutableStateOf(initial?.carbs?.toString() ?: "") }
    var fatText by remember { mutableStateOf(initial?.fat?.toString() ?: "") }
    var fiberText by remember { mutableStateOf(initial?.fiber?.toString() ?: "") }
    var nutritionError by remember { mutableStateOf<String?>(null) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var emojiError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true

        if (name.isBlank()) {
            nameError = "Nama produk tidak boleh kosong"
            isValid = false
        } else if (name.trim().length < 3) {
            nameError = "Nama minimal 3 karakter"
            isValid = false
        }

        val price = priceText.toIntOrNull()
        if (price == null || price <= 0) {
            priceError = "Masukkan harga yang valid"
            isValid = false
        }

        if (emoji.isBlank()) {
            emojiError = "Emoji tidak boleh kosong"
            isValid = false
        }

        val nutritionValues = listOf(proteinText, carbsText, fatText, fiberText).map { it.toIntOrNull() }
        if (nutritionValues.any { it == null || it !in 0..100 }) {
            nutritionError = "Isi keempat nilai gizi dengan angka 0-100"
            isValid = false
        } else if (nutritionValues.sumOf { it ?: 0 } == 0) {
            nutritionError = "Isi minimal satu nilai gizi lebih dari 0"
            isValid = false
        }

        return isValid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
    ) {
        SellerHeader(
            title = if (initial == null) "Tambah Produk" else "Edit Produk",
            subtitle = sellerName,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormSection(title = "Informasi Produk") {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Nama produk") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = nameError != null,
                    colors = tinySpoonFieldColors(),
                    singleLine = true
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it.filter { c -> c.isDigit() }.take(7)
                        priceError = null
                    },
                    label = { Text("Harga (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = priceError != null,
                    colors = tinySpoonFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (priceError != null) {
                    Text(
                        text = priceError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emoji,
                    onValueChange = {
                        emoji = it.take(4)
                        emojiError = null
                    },
                    label = { Text("Emoji produk") },
                    supportingText = { Text("Ketik 1 emoji sebagai gambar produk") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = emojiError != null,
                    colors = tinySpoonFieldColors(),
                    singleLine = true
                )
                if (emojiError != null) {
                    Text(
                        text = emojiError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            FormSection(title = "Informasi Gizi (per porsi)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutritionField(
                        label = "Protein (g)",
                        value = proteinText,
                        onValueChange = {
                            proteinText = it
                            nutritionError = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NutritionField(
                        label = "Karbohidrat (g)",
                        value = carbsText,
                        onValueChange = {
                            carbsText = it
                            nutritionError = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutritionField(
                        label = "Lemak (g)",
                        value = fatText,
                        onValueChange = {
                            fatText = it
                            nutritionError = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NutritionField(
                        label = "Serat (g)",
                        value = fiberText,
                        onValueChange = {
                            fiberText = it
                            nutritionError = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (nutritionError != null) {
                    Text(
                        text = nutritionError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                    )
                }
            }

            FormSection(title = "Usia Bayi") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ageCategories.drop(1)) { age ->
                        CategoryChip(
                            text = age,
                            isSelected = ageGroup == age,
                            onClick = { ageGroup = age }
                        )
                    }
                }
            }

            FormSection(title = "Tekstur") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(textureCategories.drop(1)) { option ->
                        CategoryChip(
                            text = option,
                            isSelected = texture == option,
                            onClick = { texture = option }
                        )
                    }
                }
            }

            FormSection(title = "Kandungan Alergen") {
                Text(
                    text = "Pilih bahan yang terkandung di produk ini",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allergenOptions) { allergen ->
                        val isSelected = allergen in allergens
                        CategoryChip(
                            text = allergen,
                            isSelected = isSelected,
                            onClick = {
                                allergens = if (isSelected) allergens - allergen
                                else allergens + allergen
                            }
                        )
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
                onClick = {
                    if (validate()) {
                        onSubmit(
                            Product(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                price = priceText.toInt(),
                                sellerName = sellerName,
                                ageGroup = ageGroup,
                                texture = texture,
                                rating = initial?.rating ?: 0f,
                                imageEmoji = emoji.trim(),
                                allergens = allergenOptions.filter { it in allergens },
                                protein = proteinText.toInt(),
                                carbs = carbsText.toInt(),
                                fat = fatText.toInt(),
                                fiber = fiberText.toInt()
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (initial == null) "Simpan Produk" else "Simpan Perubahan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}