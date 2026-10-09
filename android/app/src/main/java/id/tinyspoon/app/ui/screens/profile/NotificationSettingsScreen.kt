package id.tinyspoon.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.screens.seller.SellerHeader
import id.tinyspoon.app.ui.theme.*

data class NotificationPrefs(
    val orderStatus: Boolean = true,
    val promo: Boolean = true,
    val reviewReminder: Boolean = false,
    val newProducts: Boolean = false
)

@Composable
fun NotificationSettingsScreen(
    prefs: NotificationPrefs,
    onBack: () -> Unit,
    onPrefsChange: (NotificationPrefs) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
    ) {
        SellerHeader(
            title = "Notifikasi",
            subtitle = "Atur preferensi notifikasi",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingSwitchRow(
                        title = "Status pesanan",
                        subtitle = "Kabar saat pesananmu diterima, dimasak, dan dikirim",
                        checked = prefs.orderStatus,
                        onCheckedChange = { onPrefsChange(prefs.copy(orderStatus = it)) }
                    )
                    HorizontalDivider(color = BorderOrange)
                    SettingSwitchRow(
                        title = "Promo dan diskon",
                        subtitle = "Penawaran spesial untuk MPASI si kecil",
                        checked = prefs.promo,
                        onCheckedChange = { onPrefsChange(prefs.copy(promo = it)) }
                    )
                    HorizontalDivider(color = BorderOrange)
                    SettingSwitchRow(
                        title = "Pengingat ulasan",
                        subtitle = "Diingatkan menulis ulasan setelah pesanan selesai",
                        checked = prefs.reviewReminder,
                        onCheckedChange = { onPrefsChange(prefs.copy(reviewReminder = it)) }
                    )
                    HorizontalDivider(color = BorderOrange)
                    SettingSwitchRow(
                        title = "Produk baru",
                        subtitle = "Info saat seller favoritmu menambah menu",
                        checked = prefs.newProducts,
                        onCheckedChange = { onPrefsChange(prefs.copy(newProducts = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OrangePrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}