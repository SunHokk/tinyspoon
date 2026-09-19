package id.tinyspoon.app.ui.screens.order

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.screens.cart.CartItem
import id.tinyspoon.app.ui.theme.*

@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    onBack: () -> Unit,
    onOrderSuccess: () -> Unit
) {
    var address by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var selectedPayment by rememberSaveable { mutableStateOf("Transfer Bank") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf<String?>(null) }

    val totalPrice = cartItems.sumOf { it.product.price * it.quantity }
    val deliveryFee = 10000
    val grandTotal = totalPrice + deliveryFee

    val paymentMethods = listOf("Transfer Bank", "QRIS", "COD (Bayar di Tempat)")

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(
                    onClick = onOrderSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Lihat Pesanan", color = Color.White)
                }
            },
            title = {
                Text(
                    text = "🎉 Pesanan Berhasil!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Pesanan kamu sedang diproses. MPASI untuk si kecil akan segera dikirim!",
                    textAlign = TextAlign.Center,
                    color = TextSecondary
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrangePrimary)
                .padding(16.dp)
                .padding(top = 24.dp)
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
                    text = "Checkout",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CheckoutSection(title = "Ringkasan Pesanan") {
                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.product.name} x${item.quantity}",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Rp ${item.product.price * item.quantity}",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            CheckoutSection(title = "Alamat Pengiriman") {
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        addressError = null
                    },
                    placeholder = { Text("Masukkan alamat lengkap...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = addressError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary,
                        cursorColor = OrangePrimary,
                        errorBorderColor = Color.Red
                    ),
                    minLines = 3
                )
                if (addressError != null) {
                    Text(
                        text = addressError!!,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Catatan tambahan (opsional)...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        cursorColor = OrangePrimary
                    ),
                    minLines = 2
                )
            }

            CheckoutSection(title = "Metode Pembayaran") {
                paymentMethods.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPayment = method }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedPayment == method,
                            onClick = { selectedPayment = method },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = OrangePrimary
                            )
                        )
                        Text(
                            text = method,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            CheckoutSection(title = "Rincian Biaya") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", fontSize = 14.sp, color = TextSecondary)
                    Text("Rp $totalPrice", fontSize = 14.sp, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ongkos kirim", fontSize = 14.sp, color = TextSecondary)
                    Text("Rp $deliveryFee", fontSize = 14.sp, color = TextPrimary)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = BorderOrange
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Rp $grandTotal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    if (address.isBlank()) {
                        addressError = "Alamat tidak boleh kosong"
                    } else {
                        showSuccessDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Buat Pesanan • Rp $grandTotal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CheckoutSection(
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