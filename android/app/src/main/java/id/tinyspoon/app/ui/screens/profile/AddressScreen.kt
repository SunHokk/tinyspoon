package id.tinyspoon.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.screens.home.CategoryChip
import id.tinyspoon.app.ui.screens.seller.ActionButton
import id.tinyspoon.app.ui.screens.seller.SellerHeader
import id.tinyspoon.app.ui.theme.*
import java.util.UUID

data class SavedAddress(
    val id: String,
    val label: String,
    val recipient: String,
    val fullAddress: String,
    val isDefault: Boolean = false
)

fun SavedAddress.asDeliveryText(): String = "$recipient\n$fullAddress"

val dummyAddresses = listOf(
    SavedAddress("addr-1", "Rumah", "Gilbert", "Jl. Kemang Raya No. 10, Jakarta Selatan", true)
)

val addressLabels = listOf("Rumah", "Kantor", "Lainnya")

private fun List<SavedAddress>.normalizeDefault(preferredId: String?): List<SavedAddress> {
    if (isEmpty()) return this
    val defaultId = preferredId ?: firstOrNull { it.isDefault }?.id ?: first().id
    return map { it.copy(isDefault = it.id == defaultId) }
}

fun List<SavedAddress>.withSaved(saved: SavedAddress): List<SavedAddress> {
    val base = if (any { it.id == saved.id }) {
        map { if (it.id == saved.id) saved else it }
    } else {
        this + saved
    }
    return base.normalizeDefault(if (saved.isDefault) saved.id else null)
}

fun List<SavedAddress>.withoutAddress(id: String): List<SavedAddress> =
    filterNot { it.id == id }.normalizeDefault(null)

fun List<SavedAddress>.withDefault(id: String): List<SavedAddress> =
    normalizeDefault(id)

@Composable
fun AddressScreen(
    addresses: List<SavedAddress>,
    onBack: () -> Unit,
    onSaveAddress: (SavedAddress) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onSetDefault: (String) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SavedAddress?>(null) }
    var toDelete by remember { mutableStateOf<SavedAddress?>(null) }

    toDelete?.let { address ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAddress(address.id)
                        toDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            title = { Text("Hapus alamat?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    text = "Alamat \"${address.label}\" akan dihapus dari daftarmu.",
                    color = TextSecondary
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showForm) {
        AddressForm(
            initial = editing,
            forceDefault = addresses.isEmpty() || (editing != null && addresses.size == 1),
            onBack = {
                showForm = false
                editing = null
            },
            onSubmit = { saved ->
                onSaveAddress(saved)
                showForm = false
                editing = null
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCream)
        ) {
            SellerHeader(
                title = "Alamat Tersimpan",
                subtitle = "${addresses.size} alamat",
                onBack = onBack
            )

            if (addresses.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "📍", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada alamat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Simpan alamat supaya checkout lebih cepat",
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
                    items(addresses, key = { it.id }) { address ->
                        AddressCard(
                            address = address,
                            onEdit = {
                                editing = address
                                showForm = true
                            },
                            onSetDefault = { onSetDefault(address.id) },
                            onDelete = { toDelete = address }
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
                        editing = null
                        showForm = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "+ Tambah Alamat",
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
private fun AddressCard(
    address: SavedAddress,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📍", fontSize = 16.sp)
                Text(
                    text = address.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (address.isDefault) {
                    Box(
                        modifier = Modifier
                            .background(OrangePrimaryLight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Utama",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = address.recipient,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = address.fullAddress,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )

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
                if (!address.isDefault) {
                    ActionButton(
                        text = "Jadikan Utama",
                        color = OrangePrimary,
                        onClick = onSetDefault,
                        modifier = Modifier.weight(1.4f)
                    )
                }
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
private fun AddressForm(
    initial: SavedAddress?,
    forceDefault: Boolean,
    onBack: () -> Unit,
    onSubmit: (SavedAddress) -> Unit
) {
    var label by remember { mutableStateOf(initial?.label ?: addressLabels[0]) }
    var recipient by remember { mutableStateOf(initial?.recipient ?: "") }
    var fullAddress by remember { mutableStateOf(initial?.fullAddress ?: "") }
    var makeDefault by remember { mutableStateOf(initial?.isDefault ?: false) }

    var recipientError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true

        if (recipient.isBlank()) {
            recipientError = "Nama penerima tidak boleh kosong"
            isValid = false
        } else if (recipient.trim().length < 3) {
            recipientError = "Nama minimal 3 karakter"
            isValid = false
        }

        if (fullAddress.isBlank()) {
            addressError = "Alamat tidak boleh kosong"
            isValid = false
        } else if (fullAddress.trim().length < 10) {
            addressError = "Tulis alamat selengkap mungkin"
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
            title = if (initial == null) "Tambah Alamat" else "Edit Alamat",
            subtitle = null,
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Label Alamat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        addressLabels.forEach { option ->
                            CategoryChip(
                                text = option,
                                isSelected = label == option,
                                onClick = { label = option }
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Detail Alamat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = recipient,
                        onValueChange = {
                            recipient = it
                            recipientError = null
                        },
                        label = { Text("Nama penerima") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = recipientError != null,
                        colors = tinySpoonFieldColors(),
                        singleLine = true
                    )
                    if (recipientError != null) {
                        Text(
                            text = recipientError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fullAddress,
                        onValueChange = {
                            fullAddress = it
                            addressError = null
                        },
                        label = { Text("Alamat lengkap") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = addressError != null,
                        colors = tinySpoonFieldColors(),
                        minLines = 3
                    )
                    if (addressError != null) {
                        Text(
                            text = addressError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    if (!forceDefault) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { makeDefault = !makeDefault },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = makeDefault,
                                onCheckedChange = { makeDefault = it },
                                colors = CheckboxDefaults.colors(checkedColor = OrangePrimary)
                            )
                            Text(
                                text = "Jadikan alamat utama",
                                fontSize = 14.sp,
                                color = TextPrimary
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
                onClick = {
                    if (validate()) {
                        onSubmit(
                            SavedAddress(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                label = label,
                                recipient = recipient.trim(),
                                fullAddress = fullAddress.trim(),
                                isDefault = forceDefault || makeDefault
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
                    text = if (initial == null) "Simpan Alamat" else "Simpan Perubahan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}