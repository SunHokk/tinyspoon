package id.tinyspoon.app.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.theme.*

data class ShopProfile(
    val name: String,
    val description: String,
    val address: String,
    val phone: String,
    val openHours: String
)

val dummyShops = mapOf(
    "Dapur Bunda" to ShopProfile(
        name = "Dapur Bunda",
        description = "Dapur rumahan yang menyiapkan MPASI segar setiap hari tanpa pengawet, dimasak sesuai tahapan usia bayi.",
        address = "Jl. Cipete Raya No. 25, Jakarta Selatan",
        phone = "081234567890",
        openHours = "07.00 - 17.00"
    ),
    "MPASI Sehat" to ShopProfile(
        name = "MPASI Sehat",
        description = "Spesialis puree dan bubur sayur-buah dengan bahan organik pilihan untuk si kecil.",
        address = "Jl. Fatmawati No. 8, Jakarta Selatan",
        phone = "081298765432",
        openHours = "08.00 - 16.00"
    ),
    "Nutrisi Bayi" to ShopProfile(
        name = "Nutrisi Bayi",
        description = "MPASI tinggi protein hewani seperti ikan, ayam, dan hati, dengan tekstur sesuai usia.",
        address = "Jl. Kemang Selatan No. 3, Jakarta Selatan",
        phone = "081377788899",
        openHours = "09.00 - 18.00"
    )
)

@Composable
fun SellerShopProfileScreen(
    shop: ShopProfile,
    onBack: () -> Unit,
    onSave: (ShopProfile) -> Unit
) {
    var description by remember { mutableStateOf(shop.description) }
    var address by remember { mutableStateOf(shop.address) }
    var phone by remember { mutableStateOf(shop.phone) }
    var openHours by remember { mutableStateOf(shop.openHours) }

    var descriptionError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var hoursError by remember { mutableStateOf<String?>(null) }

    val hasChanges = description.trim() != shop.description ||
            address.trim() != shop.address ||
            phone != shop.phone ||
            openHours.trim() != shop.openHours

    fun validate(): Boolean {
        var isValid = true

        if (description.isBlank()) {
            descriptionError = "Deskripsi tidak boleh kosong"
            isValid = false
        } else if (description.trim().length < 20) {
            descriptionError = "Deskripsi minimal 20 karakter"
            isValid = false
        }

        if (address.isBlank()) {
            addressError = "Alamat tidak boleh kosong"
            isValid = false
        } else if (address.trim().length < 10) {
            addressError = "Tulis alamat selengkap mungkin"
            isValid = false
        }

        if (phone.isBlank()) {
            phoneError = "Nomor telepon tidak boleh kosong"
            isValid = false
        } else if (phone.length < 9 || !(phone.startsWith("0") || phone.startsWith("62"))) {
            phoneError = "Nomor harus diawali 0 atau 62, minimal 9 digit"
            isValid = false
        }

        if (openHours.isBlank()) {
            hoursError = "Jam operasional tidak boleh kosong"
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
            title = "Profil Toko",
            subtitle = shop.name,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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
                            .background(OrangePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shop.name.first().toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = shop.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nama toko") },
                        supportingText = { Text("Nama toko tidak dapat diubah") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = tinySpoonFieldColors(),
                        singleLine = true
                    )
                }
            }

            ShopSection(title = "Tentang Toko") {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it.take(200)
                        descriptionError = null
                    },
                    label = { Text("Deskripsi toko") },
                    supportingText = { Text("${description.length}/200") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = descriptionError != null,
                    colors = tinySpoonFieldColors(),
                    minLines = 3
                )
                if (descriptionError != null) {
                    Text(
                        text = descriptionError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            ShopSection(title = "Kontak dan Lokasi") {
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        addressError = null
                    },
                    label = { Text("Alamat toko") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = addressError != null,
                    colors = tinySpoonFieldColors(),
                    minLines = 2
                )
                if (addressError != null) {
                    Text(
                        text = addressError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it.filter { c -> c.isDigit() }.take(14)
                        phoneError = null
                    },
                    label = { Text("Nomor telepon") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = phoneError != null,
                    colors = tinySpoonFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                if (phoneError != null) {
                    Text(
                        text = phoneError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            ShopSection(title = "Jam Operasional") {
                OutlinedTextField(
                    value = openHours,
                    onValueChange = {
                        openHours = it.take(30)
                        hoursError = null
                    },
                    label = { Text("Jam buka") },
                    supportingText = { Text("Contoh: 07.00 - 17.00") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = hoursError != null,
                    colors = tinySpoonFieldColors(),
                    singleLine = true
                )
                if (hoursError != null) {
                    Text(
                        text = hoursError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
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
                    if (validate()) {
                        onSave(
                            shop.copy(
                                description = description.trim(),
                                address = address.trim(),
                                phone = phone,
                                openHours = openHours.trim()
                            )
                        )
                    }
                },
                enabled = hasChanges,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Simpan Perubahan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ShopSection(
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