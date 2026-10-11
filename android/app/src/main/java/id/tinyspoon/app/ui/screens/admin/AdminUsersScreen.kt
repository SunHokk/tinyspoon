package id.tinyspoon.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.screens.auth.UserAccount
import id.tinyspoon.app.ui.screens.auth.UserRole
import id.tinyspoon.app.ui.screens.home.CategoryChip
import id.tinyspoon.app.ui.screens.seller.ActionButton
import id.tinyspoon.app.ui.screens.seller.SellerHeader
import id.tinyspoon.app.ui.theme.*

private val userFilters = listOf("Semua", "Pembeli", "Penjual", "Admin", "Nonaktif")

@Composable
fun AdminUsersScreen(
    accounts: List<UserAccount>,
    onToggleActive: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    var target by remember { mutableStateOf<UserAccount?>(null) }

    val filtered = accounts.filter { account ->
        val matchFilter = when (filter) {
            "Pembeli" -> account.role == UserRole.BUYER
            "Penjual" -> account.role == UserRole.SELLER
            "Admin" -> account.role == UserRole.ADMIN
            "Nonaktif" -> !account.isActive
            else -> true
        }
        val q = query.trim()
        val matchQuery = q.isEmpty() ||
                account.name.contains(q, ignoreCase = true) ||
                account.email.contains(q, ignoreCase = true) ||
                (account.shopName?.contains(q, ignoreCase = true) ?: false)
        matchFilter && matchQuery
    }

    target?.let { account ->
        val activating = !account.isActive
        AlertDialog(
            onDismissRequest = { target = null },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleActive(account.id, activating)
                        target = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activating) Color(0xFF4CAF50) else Color.Red
                    )
                ) {
                    Text(if (activating) "Aktifkan" else "Nonaktifkan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { target = null }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            title = {
                Text(
                    text = if (activating) "Aktifkan akun?" else "Nonaktifkan akun?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = when {
                        activating -> "${account.name} bisa masuk kembali ke aplikasi."
                        account.role == UserRole.SELLER ->
                            "${account.name} tidak bisa masuk, dan produk tokonya disembunyikan dari pembeli sampai akun diaktifkan lagi."
                        else -> "${account.name} tidak bisa masuk sampai akun diaktifkan lagi."
                    },
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
        SellerHeader(
            title = "Kelola Pengguna",
            subtitle = "${filtered.size} dari ${accounts.size} akun",
            onBack = onBack
        )

        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari nama, email, atau toko...", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = tinySpoonFieldColors(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(userFilters) { option ->
                    CategoryChip(
                        text = option,
                        isSelected = filter == option,
                        onClick = { filter = option }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "🔍", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tidak ada akun yang cocok",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Coba ubah kata kunci atau filter",
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
                items(filtered, key = { it.id }) { account ->
                    UserCard(
                        account = account,
                        onToggle = { target = account }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    account: UserAccount,
    onToggle: () -> Unit
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (account.isActive) OrangePrimary else Color(0xFFBDBDBD),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = account.name.take(1).uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = account.email,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    if (account.shopName != null) {
                        Text(
                            text = "Toko: ${account.shopName}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AccountBadge(
                        text = account.sellerStatus?.let { "${account.role.label} • ${it.label}" }
                            ?: account.role.label,
                        color = OrangePrimary
                    )
                    if (!account.isActive) {
                        AccountBadge(text = "Nonaktif", color = Color(0xFFE53935))
                    }
                }
            }

            if (account.role != UserRole.ADMIN) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = BorderOrange
                )
                ActionButton(
                    text = if (account.isActive) "Nonaktifkan" else "Aktifkan",
                    color = if (account.isActive) Color.Red else Color(0xFF4CAF50),
                    onClick = onToggle,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Akun admin tidak dapat diubah",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AccountBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}