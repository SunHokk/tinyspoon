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
    val role: UserRole,
    val shopName: String? = null,
    val sellerStatus: SellerStatus? = null,
    val rejectionReason: String? = null,
    val isActive: Boolean = true
)