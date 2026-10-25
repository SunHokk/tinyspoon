package id.tinyspoon.app.ui.screens.seller

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinyspoon.app.ui.screens.auth.SellerStatus
import id.tinyspoon.app.ui.screens.home.CategoryChip
import id.tinyspoon.app.ui.screens.product.CertDetail
import id.tinyspoon.app.ui.screens.product.CertStatus
import id.tinyspoon.app.ui.screens.product.SellerCertificate
import id.tinyspoon.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale
import kotlinx.coroutines.launch

private val certTypeOptions = listOf(
    "SPP-IRT" to "Sertifikat Produksi Pangan Industri Rumah Tangga",
    "BPOM" to "Sertifikat Izin Edar BPOM",
    "Halal" to "Sertifikat Halal & Produksi Pangan"
)

private val allowedExtensions = listOf(".pdf", ".jpg", ".jpeg", ".png")

private val inputDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

private val displayDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))

private fun parseInputDate(text: String): LocalDate? =
    try {
        LocalDate.parse(text, inputDateFormat)
    } catch (e: DateTimeParseException) {
        null
    }

private fun displayToInput(display: String): String =
    try {
        LocalDate.parse(display, displayDateFormat).format(inputDateFormat)
    } catch (e: DateTimeParseException) {
        ""
    }

private fun fileNameOf(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index)
        }
    }
    return uri.lastPathSegment ?: "dokumen"
}

@Composable
fun SellerPendingScreen(
    shopName: String,
    ownerName: String,
    status: SellerStatus,
    rejectionReason: String?,
    certificate: SellerCertificate?,
    onSubmitCertificate: suspend (SellerCertificate, Uri?) -> String?,
    onLogout: () -> Unit
) {
    val isRejected = status == SellerStatus.REJECTED
    val hasSubmitted = certificate != null && !isRejected

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
            Column {
                Text(
                    text = "Akun Penjual",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = shopName,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val emoji = when {
                        isRejected -> "❌"
                        hasSubmitted -> "⏳"
                        else -> "📄"
                    }
                    val title = when {
                        isRejected -> "Pendaftaran Ditolak"
                        hasSubmitted -> "Menunggu Verifikasi"
                        else -> "Lengkapi Sertifikat"
                    }
                    val message = when {
                        isRejected -> "Admin menolak pengajuan toko \"$shopName\". Perbaiki data sertifikat di bawah lalu kirim ulang."
                        hasSubmitted -> "Sertifikat tokomu sudah dikirim. Admin akan memeriksanya, dan kamu bisa mulai berjualan setelah disetujui. Masuk lagi nanti untuk melihat statusnya."
                        else -> "Halo, $ownerName! Satu langkah lagi: kirim sertifikat kelayakan pangan toko \"$shopName\" agar admin bisa memverifikasinya."
                    }

                    Text(text = emoji, fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRejected) Color(0xFFE53935) else OrangePrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    if (isRejected && !rejectionReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE53935).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Alasan: $rejectionReason",
                                fontSize = 13.sp,
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (hasSubmitted && certificate != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Data yang Dikirim",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CertDetail(label = "Jenis Sertifikat", value = certificate.certType)
                        CertDetail(label = "Nomor Sertifikat", value = certificate.certNumber)
                        CertDetail(label = "Diterbitkan Oleh", value = certificate.issuedBy)
                        CertDetail(label = "Tanggal Terbit", value = certificate.issuedDate)
                        CertDetail(label = "Berlaku Hingga", value = certificate.expiredDate)
                        CertDetail(label = "Dokumen", value = "📎 ${certificate.documentName}")
                    }
                }
            } else {
                CertificateForm(
                    shopName = shopName,
                    initial = certificate,
                    submitLabel = if (isRejected) "Kirim Ulang" else "Kirim Sertifikat",
                    onSubmit = onSubmitCertificate
                )
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
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Keluar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
private fun CertificateForm(
    shopName: String,
    initial: SellerCertificate?,
    submitLabel: String,
    onSubmit: suspend (SellerCertificate, Uri?) -> String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var typeKey by remember {
        mutableStateOf(
            certTypeOptions.firstOrNull { it.second == initial?.certType }?.first
                ?: certTypeOptions[0].first
        )
    }
    var certNumber by remember { mutableStateOf(initial?.certNumber ?: "") }
    var issuedBy by remember { mutableStateOf(initial?.issuedBy ?: "") }
    var issuedDate by remember { mutableStateOf(initial?.let { displayToInput(it.issuedDate) } ?: "") }
    var expiredDate by remember { mutableStateOf(initial?.let { displayToInput(it.expiredDate) } ?: "") }
    var fileName by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    var numberError by remember { mutableStateOf<String?>(null) }
    var issuerError by remember { mutableStateOf<String?>(null) }
    var issuedError by remember { mutableStateOf<String?>(null) }
    var expiredError by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = fileNameOf(context, uri)
            if (allowedExtensions.any { name.lowercase().endsWith(it) }) {
                fileName = name
                pickedUri = uri
                fileError = null
                formError = null
            } else {
                fileError = "Format file harus PDF, JPG, atau PNG"
            }
        }
    }

    fun validate(): Boolean {
        var ok = true

        if (certNumber.trim().length < 8) {
            numberError = "Nomor sertifikat minimal 8 karakter"
            ok = false
        }

        if (issuedBy.trim().length < 3) {
            issuerError = "Isi nama lembaga penerbit"
            ok = false
        }

        val issued = parseInputDate(issuedDate)
        val expired = parseInputDate(expiredDate)

        if (issued == null) {
            issuedError = "Format dd/MM/yyyy, mis. 01/01/2025"
            ok = false
        } else if (issued.isAfter(LocalDate.now())) {
            issuedError = "Tanggal terbit tidak boleh di masa depan"
            ok = false
        }

        if (expired == null) {
            expiredError = "Format dd/MM/yyyy, mis. 31/12/2028"
            ok = false
        } else if (issued != null && !expired.isAfter(issued)) {
            expiredError = "Harus setelah tanggal terbit"
            ok = false
        } else if (!expired.isAfter(LocalDate.now())) {
            expiredError = "Sertifikat sudah kedaluwarsa"
            ok = false
        }

        if (pickedUri == null) {
            fileError = "Unggah dokumen sertifikat"
            ok = false
        }

        return ok
    }

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
                text = "Data Sertifikat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(text = "Jenis sertifikat", fontSize = 13.sp, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                certTypeOptions.forEach { (key, _) ->
                    CategoryChip(
                        text = key,
                        isSelected = typeKey == key,
                        onClick = { typeKey = key }
                    )
                }
            }

            CertField(
                label = "Nomor sertifikat",
                value = certNumber,
                onValueChange = {
                    certNumber = it
                    numberError = null
                },
                error = numberError
            )

            CertField(
                label = "Diterbitkan oleh",
                value = issuedBy,
                onValueChange = {
                    issuedBy = it
                    issuerError = null
                },
                error = issuerError,
                supportingText = "Mis. Dinas Kesehatan Kota Jakarta Selatan"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CertField(
                    label = "Tanggal terbit",
                    value = issuedDate,
                    onValueChange = {
                        issuedDate = it.filter { c -> c.isDigit() || c == '/' }.take(10)
                        issuedError = null
                    },
                    error = issuedError,
                    keyboardType = KeyboardType.Number,
                    supportingText = "dd/MM/yyyy",
                    modifier = Modifier.weight(1f)
                )
                CertField(
                    label = "Berlaku hingga",
                    value = expiredDate,
                    onValueChange = {
                        expiredDate = it.filter { c -> c.isDigit() || c == '/' }.take(10)
                        expiredError = null
                    },
                    error = expiredError,
                    keyboardType = KeyboardType.Number,
                    supportingText = "dd/MM/yyyy",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Dokumen sertifikat (PDF/JPG/PNG, maks. 5 MB)",
                fontSize = 13.sp,
                color = TextSecondary
            )

            if (fileName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OrangePrimaryLight, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "📎 $fileName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }
            }

            OutlinedButton(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary)
            ) {
                Text(
                    text = if (fileName.isBlank()) "Pilih File" else "Ganti File",
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (fileError != null) {
                Text(
                    text = fileError!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (formError != null) {
                Text(
                    text = formError!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Button(
                onClick = {
                    if (validate()) {
                        val issued = parseInputDate(issuedDate)!!
                        val expired = parseInputDate(expiredDate)!!
                        val cert = SellerCertificate(
                            sellerName = shopName,
                            certNumber = certNumber.trim(),
                            issuedBy = issuedBy.trim(),
                            issuedDate = issued.format(displayDateFormat),
                            expiredDate = expired.format(displayDateFormat),
                            certType = certTypeOptions.first { it.first == typeKey }.second,
                            status = CertStatus.PENDING,
                            documentName = fileName
                        )
                        scope.launch {
                            isSubmitting = true
                            formError = onSubmit(cert, pickedUri)
                            isSubmitting = false
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isSubmitting) "Mengunggah..." else submitLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CertField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            supportingText = if (supportingText != null) {
                { Text(supportingText, fontSize = 11.sp) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = error != null,
            colors = tinySpoonFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true
        )
        if (error != null) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}