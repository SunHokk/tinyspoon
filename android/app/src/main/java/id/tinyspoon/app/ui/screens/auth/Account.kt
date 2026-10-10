package id.tinyspoon.app.ui.screens.auth

enum class UserRole(val label: String) {
    BUYER("Pembeli"),
    SELLER("Penjual"),
    ADMIN("Admin")
}

enum class SellerStatus(val label: String) {
    PENDING("Menunggu Verifikasi"),
    APPROVED("Terverifikasi"),
    REJECTED("Ditolak")
}

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val shopName: String? = null,
    val sellerStatus: SellerStatus? = null
)

// Data demo. Password tersimpan polos hanya sementara; backend nanti menyimpan hash.
val dummyAccounts = listOf(
    UserAccount("acc-buyer-1", "Gilbert", "gilbert@email.com", "gilbert123", UserRole.BUYER),
    UserAccount("acc-seller-1", "Bunda Lia", "dapurbunda@email.com", "bunda123", UserRole.SELLER, "Dapur Bunda", SellerStatus.APPROVED),
    UserAccount("acc-seller-2", "Pak Dimas", "mpasisehat@email.com", "sehat123", UserRole.SELLER, "MPASI Sehat", SellerStatus.APPROVED),
    UserAccount("acc-seller-3", "Bu Rani", "nutrisibayi@email.com", "nutrisi123", UserRole.SELLER, "Nutrisi Bayi", SellerStatus.APPROVED),
    UserAccount("acc-admin-1", "Admin TinySpoon", "admin@tinyspoon.id", "admin123", UserRole.ADMIN)
)