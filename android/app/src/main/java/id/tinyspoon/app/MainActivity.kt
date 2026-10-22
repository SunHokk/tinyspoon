package id.tinyspoon.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Intent
import android.net.Uri
import id.tinyspoon.app.data.AdminRepository
import id.tinyspoon.app.ui.screens.product.SellerCertificate
import id.tinyspoon.app.data.AuthRepository
import id.tinyspoon.app.data.SessionUser
import kotlinx.coroutines.launch
import id.tinyspoon.app.ui.screens.admin.AdminDashboardScreen
import id.tinyspoon.app.ui.screens.auth.AuthScreen
import id.tinyspoon.app.ui.screens.auth.SellerStatus
import id.tinyspoon.app.ui.screens.auth.UserAccount
import id.tinyspoon.app.ui.screens.auth.UserRole
import id.tinyspoon.app.ui.screens.auth.dummyAccounts
import id.tinyspoon.app.ui.screens.cart.CartItem
import id.tinyspoon.app.ui.screens.cart.CartScreen
import id.tinyspoon.app.ui.screens.home.HomeScreen
import id.tinyspoon.app.ui.screens.home.Product
import id.tinyspoon.app.ui.screens.home.dummyProducts
import id.tinyspoon.app.ui.screens.onboarding.OnboardingScreen
import id.tinyspoon.app.ui.screens.order.CheckoutScreen
import id.tinyspoon.app.ui.screens.order.DELIVERY_FEE_PER_SELLER
import id.tinyspoon.app.ui.screens.order.OrderHistoryScreen
import id.tinyspoon.app.ui.screens.order.OrderTrackingScreen
import id.tinyspoon.app.ui.screens.order.dummyOrderHistory
import id.tinyspoon.app.ui.screens.product.ProductDetailScreen
import id.tinyspoon.app.ui.screens.product.Review
import id.tinyspoon.app.ui.screens.product.ReviewScreen
import id.tinyspoon.app.ui.screens.product.SellerCertificateScreen
import id.tinyspoon.app.ui.screens.product.seedReviewsFor
import id.tinyspoon.app.ui.screens.product.withRatingsFrom
import id.tinyspoon.app.ui.screens.profile.AboutScreen
import id.tinyspoon.app.ui.screens.profile.AddressScreen
import id.tinyspoon.app.ui.screens.profile.FavoritesScreen
import id.tinyspoon.app.ui.screens.profile.NotificationPrefs
import id.tinyspoon.app.ui.screens.profile.NotificationSettingsScreen
import id.tinyspoon.app.ui.screens.profile.ProfileScreen
import id.tinyspoon.app.ui.screens.profile.dummyAddresses
import id.tinyspoon.app.ui.screens.profile.withDefault
import id.tinyspoon.app.ui.screens.profile.withSaved
import id.tinyspoon.app.ui.screens.profile.withoutAddress
import id.tinyspoon.app.ui.screens.seller.OrderStatus
import id.tinyspoon.app.ui.screens.seller.SellerDashboardScreen
import id.tinyspoon.app.ui.screens.seller.SellerOrder
import id.tinyspoon.app.ui.screens.seller.SellerOrdersScreen
import id.tinyspoon.app.ui.screens.seller.SellerPendingScreen
import id.tinyspoon.app.ui.screens.seller.SellerProductsScreen
import id.tinyspoon.app.ui.screens.seller.SellerShopProfileScreen
import id.tinyspoon.app.ui.screens.seller.ShopProfile
import id.tinyspoon.app.ui.screens.seller.dummySellerOrders
import id.tinyspoon.app.ui.screens.seller.dummyShops
import id.tinyspoon.app.ui.theme.TinySpoonTheme
import id.tinyspoon.app.ui.screens.product.CertStatus
import id.tinyspoon.app.ui.screens.product.dummyCertificates
import id.tinyspoon.app.ui.screens.admin.AdminUsersScreen
import id.tinyspoon.app.ui.screens.admin.AdminVerificationScreen
import id.tinyspoon.app.data.CatalogRepository
import id.tinyspoon.app.data.ShopDetail
import id.tinyspoon.app.data.OrderRepository
import id.tinyspoon.app.data.SellerRepository
import id.tinyspoon.app.data.ProfileRepository
import id.tinyspoon.app.ui.screens.profile.SavedAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TinySpoonTheme {
                AppNavigation()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    private fun hideNavigationBar() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }
}

enum class Screen {
    SPLASH, ONBOARDING, AUTH, HOME, PRODUCT_DETAIL, CART, CHECKOUT, SELLER_CERTIFICATE,
    ORDER_TRACKING, PROFILE, ORDER_HISTORY, REVIEW, SELLER_DASHBOARD, SELLER_PRODUCTS,
    SELLER_ORDERS, FAVORITES, ADDRESSES, NOTIFICATION_SETTINGS, ABOUT, SELLER_SHOP,
    SELLER_PENDING, ADMIN_DASHBOARD, ADMIN_VERIFICATION, ADMIN_USERS
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
    val scope = rememberCoroutineScope()
    var restoredUser by remember { mutableStateOf<SessionUser?>(null) }
    var sessionChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        restoredUser = AuthRepository.restoreSession()
        sessionChecked = true
    }
    var accounts by remember { mutableStateOf(dummyAccounts) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var certificates by remember { mutableStateOf(dummyCertificates) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var catalogLoading by remember { mutableStateOf(false) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var sellerDetail by remember { mutableStateOf<ShopDetail?>(null) }
    var sellerDetailLoading by remember { mutableStateOf(false) }
    var sellerOrders by remember { mutableStateOf(dummySellerOrders) }
    var shopOrders by remember { mutableStateOf<List<SellerOrder>>(emptyList()) }
    var buyerOrders by remember { mutableStateOf<List<SellerOrder>>(emptyList()) }
    var ordersLoading by remember { mutableStateOf(false) }
    var ordersError by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var trackingBackTo by remember { mutableStateOf(Screen.HOME) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }
    var detailBackTo by remember { mutableStateOf(Screen.HOME) }
    var cartBackTo by remember { mutableStateOf(Screen.HOME) }
    var addresses by remember { mutableStateOf<List<SavedAddress>>(emptyList()) }
    var myReviewCount by remember { mutableStateOf(0) }
    var adminAccounts by remember { mutableStateOf<List<UserAccount>>(emptyList()) }
    var adminCertificates by remember { mutableStateOf<Map<String, SellerCertificate>>(emptyMap()) }
    var adminDocPaths by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var adminOrders by remember { mutableStateOf<List<SellerOrder>>(emptyList()) }
    var notifPrefs by remember { mutableStateOf(NotificationPrefs()) }
    var appRating by remember { mutableStateOf(0) }
    var shops by remember { mutableStateOf(dummyShops) }
    val context = LocalContext.current
    val currentUser = accounts.find { it.id == currentUserId }
    val buyerName = currentUser?.takeIf { it.role == UserRole.BUYER }?.name ?: ""
    val currentSellerName = currentUser?.takeIf { it.role == UserRole.SELLER }?.shopName ?: ""

    fun isSellerVisible(shopName: String): Boolean {
        val owner = accounts.find { it.shopName == shopName } ?: return true
        return owner.isActive && owner.sellerStatus == SellerStatus.APPROVED
    }

    val visibleProducts = products.filter { isSellerVisible(it.sellerName) }
    val favoriteProducts = visibleProducts.filter { it.id in favoriteIds }

    suspend fun loadCatalogNow() {
        catalogLoading = true
        catalogError = null
        val result = CatalogRepository.loadCatalog()
        result.data?.let {
            products = it.products
            reviews = it.reviews
            favoriteIds = it.favoriteIds
        }
        catalogError = result.error
        catalogLoading = false
    }

    fun reloadCatalog() {
        scope.launch { loadCatalogNow() }
    }

    suspend fun loadOrdersNow() {
        ordersLoading = true
        ordersError = null
        val result = OrderRepository.loadMyOrders()
        result.orders?.let { buyerOrders = it }
        ordersError = result.error
        ordersLoading = false
    }

    fun reloadOrders() {
        scope.launch { loadOrdersNow() }
    }

    suspend fun loadShopOrdersNow() {
        val result = OrderRepository.loadMyOrders()
        result.orders?.let { shopOrders = it }
    }

    fun reloadShopOrders() {
        scope.launch { loadShopOrdersNow() }
    }

    suspend fun loadProfileDataNow() {
        ProfileRepository.loadProfileData()?.let {
            addresses = it.addresses
            notifPrefs = it.prefs
            appRating = it.appRating
            myReviewCount = it.reviewCount
        }
    }

    fun reloadProfileData() {
        scope.launch { loadProfileDataNow() }
    }

    suspend fun loadAdminDataNow() {
        AdminRepository.loadAdminData()?.let {
            adminAccounts = it.accounts
            adminCertificates = it.certificates
            adminDocPaths = it.documentPaths
        }
        OrderRepository.loadMyOrders().orders?.let { adminOrders = it }
    }

    fun reloadAdminData() {
        scope.launch { loadAdminDataNow() }
    }

    fun toggleFavorite(productId: String) {
        val wasFavorite = productId in favoriteIds
        favoriteIds = if (wasFavorite) favoriteIds - productId else favoriteIds + productId
        scope.launch {
            val ok = if (wasFavorite) CatalogRepository.removeFavorite(productId)
            else CatalogRepository.addFavorite(productId)
            if (!ok) {
                favoriteIds = if (wasFavorite) favoriteIds + productId else favoriteIds - productId
                Toast.makeText(context, "Gagal memperbarui favorit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun enterSession(session: SessionUser) {
        val user = session.account
        accounts = accounts.filterNot { it.id == user.id } + user
        session.shop?.let { shop -> shops = shops + (shop.name to shop) }
        session.certificate?.let { cert ->
            user.shopName?.let { certificates = certificates + (it to cert) }
        }
        currentUserId = user.id
        cartItems = emptyList()
        favoriteIds = emptySet()
        notifPrefs = NotificationPrefs()
        appRating = 0
        addresses = emptyList()
        myReviewCount = 0
        shopOrders = emptyList()
        currentScreen = when (user.role) {
            UserRole.BUYER -> Screen.HOME
            UserRole.SELLER ->
                if (user.sellerStatus == SellerStatus.APPROVED) Screen.SELLER_DASHBOARD
                else Screen.SELLER_PENDING
            UserRole.ADMIN -> Screen.ADMIN_DASHBOARD
        }
        reloadCatalog()
        if (user.role == UserRole.BUYER) {
            reloadOrders()
            reloadProfileData()
        }
        if (user.role == UserRole.SELLER && user.sellerStatus == SellerStatus.APPROVED) reloadShopOrders()
        if (user.role == UserRole.ADMIN) reloadAdminData()
    }

    fun logout() {
        scope.launch { AuthRepository.signOut() }
        currentUserId = null
        products = emptyList()
        reviews = emptyList()
        buyerOrders = emptyList()
        shopOrders = emptyList()
        adminAccounts = emptyList()
        adminCertificates = emptyMap()
        adminDocPaths = emptyMap()
        adminOrders = emptyList()
        addresses = emptyList()
        favoriteIds = emptySet()
        currentScreen = Screen.AUTH
    }

    val backTarget: Screen? = when (currentScreen) {
        Screen.SPLASH, Screen.ONBOARDING, Screen.AUTH, Screen.HOME,
        Screen.SELLER_DASHBOARD, Screen.SELLER_PENDING, Screen.ADMIN_DASHBOARD -> null
        Screen.PRODUCT_DETAIL -> detailBackTo
        Screen.CART -> cartBackTo
        Screen.CHECKOUT -> Screen.CART
        Screen.SELLER_CERTIFICATE, Screen.REVIEW -> Screen.PRODUCT_DETAIL
        Screen.ORDER_TRACKING -> trackingBackTo
        Screen.PROFILE -> Screen.HOME
        Screen.ORDER_HISTORY, Screen.FAVORITES, Screen.ADDRESSES,
        Screen.NOTIFICATION_SETTINGS, Screen.ABOUT -> Screen.PROFILE
        Screen.ADMIN_VERIFICATION, Screen.ADMIN_USERS -> Screen.ADMIN_DASHBOARD
        Screen.SELLER_PRODUCTS, Screen.SELLER_ORDERS, Screen.SELLER_SHOP -> Screen.SELLER_DASHBOARD
    }

    BackHandler(enabled = backTarget != null) {
        backTarget?.let { currentScreen = it }
    }

    when (currentScreen) {
        Screen.SPLASH -> SplashScreen(
            ready = sessionChecked,
            onFinished = {
                val saved = restoredUser
                if (saved != null) enterSession(saved) else currentScreen = Screen.ONBOARDING
            }
        )

        Screen.ONBOARDING -> OnboardingScreen(
            onFinish = { currentScreen = Screen.AUTH }
        )

        Screen.AUTH -> AuthScreen(
            onLogin = { email, password ->
                val result = AuthRepository.signIn(email, password)
                result.user?.let { enterSession(it) }
                result.error
            },
            onRegister = { name, email, password, role, shopName ->
                val result = AuthRepository.signUp(name, email, password, role, shopName)
                result.user?.let { enterSession(it) }
                result.error
            }
        )

        Screen.HOME -> HomeScreen(
            products = visibleProducts,
            isLoading = catalogLoading,
            errorMessage = catalogError,
            onRetry = { reloadCatalog() },
            onProductClick = { product ->
                selectedProduct = product
                detailBackTo = Screen.HOME
                currentScreen = Screen.PRODUCT_DETAIL
            },
            onProfileClick = {
                reloadProfileData()
                currentScreen = Screen.PROFILE
            }
        )

        Screen.PROFILE -> ProfileScreen(
            userName = currentUser?.name ?: "",
            userEmail = currentUser?.email ?: "",
            orderCount = buyerOrders.size,
            reviewCount = myReviewCount,
            favoriteCount = favoriteProducts.size,
            appRating = appRating,
            onBack = { currentScreen = Screen.HOME },
            onLogout = { logout() },
            onOrderHistoryClick = {
                reloadOrders()
                currentScreen = Screen.ORDER_HISTORY
            },
            onFavoritesClick = { currentScreen = Screen.FAVORITES },
            onAddressesClick = { currentScreen = Screen.ADDRESSES },
            onNotificationsClick = { currentScreen = Screen.NOTIFICATION_SETTINGS },
            onAboutClick = { currentScreen = Screen.ABOUT },
            onSubmitAppRating = { rating, feedback ->
                scope.launch {
                    val error = ProfileRepository.submitAppRating(rating, feedback)
                    if (error == null) {
                        appRating = rating
                        Toast.makeText(context, "Terima kasih atas penilaianmu!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal mengirim: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )

        Screen.ADDRESSES -> AddressScreen(
            addresses = addresses,
            onBack = { currentScreen = Screen.PROFILE },
            onSaveAddress = { saved ->
                val isNew = addresses.none { it.id == saved.id }
                addresses = addresses.withSaved(saved)
                scope.launch {
                    val error = ProfileRepository.saveAddress(saved, isNew)
                    if (error != null) {
                        Toast.makeText(context, "Gagal menyimpan: $error", Toast.LENGTH_LONG).show()
                    }
                    loadProfileDataNow()
                }
            },
            onDeleteAddress = { id ->
                addresses = addresses.withoutAddress(id)
                scope.launch {
                    val error = ProfileRepository.deleteAddress(id)
                    if (error != null) {
                        Toast.makeText(context, "Gagal menghapus: $error", Toast.LENGTH_LONG).show()
                    }
                    loadProfileDataNow()
                }
            },
            onSetDefault = { id ->
                addresses = addresses.withDefault(id)
                scope.launch {
                    val error = ProfileRepository.setDefaultAddress(id)
                    if (error != null) {
                        Toast.makeText(context, "Gagal mengubah alamat utama: $error", Toast.LENGTH_LONG).show()
                    }
                    loadProfileDataNow()
                }
            }
        )

        Screen.NOTIFICATION_SETTINGS -> NotificationSettingsScreen(
            prefs = notifPrefs,
            onBack = { currentScreen = Screen.PROFILE },
            onPrefsChange = { newPrefs ->
                val old = notifPrefs
                notifPrefs = newPrefs
                scope.launch {
                    val error = ProfileRepository.updatePrefs(newPrefs)
                    if (error != null) {
                        notifPrefs = old
                        Toast.makeText(context, "Gagal menyimpan: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )

        Screen.ABOUT -> AboutScreen(
            onBack = { currentScreen = Screen.PROFILE }
        )

        Screen.FAVORITES -> FavoritesScreen(
            favorites = favoriteProducts,
            onBack = { currentScreen = Screen.PROFILE },
            onBrowseProducts = { currentScreen = Screen.HOME },
            onProductClick = { product ->
                selectedProduct = product
                detailBackTo = Screen.FAVORITES
                currentScreen = Screen.PRODUCT_DETAIL
            },
            onRemoveFavorite = { product -> toggleFavorite(product.id) }
        )

        Screen.PRODUCT_DETAIL -> selectedProduct?.let { product ->
            ProductDetailScreen(
                product = product,
                isFavorite = product.id in favoriteIds,
                onToggleFavorite = { toggleFavorite(product.id) },
                onBack = { currentScreen = detailBackTo },
                onAddToCart = {
                    val existingItem = cartItems.find { it.product.id == product.id }
                    cartItems = if (existingItem != null) {
                        cartItems.map {
                            if (it.product.id == product.id) it.copy(quantity = (it.quantity + 1).coerceAtMost(50))
                            else it
                        }
                    } else {
                        cartItems + CartItem(product, 1)
                    }
                    cartBackTo = Screen.PRODUCT_DETAIL
                    currentScreen = Screen.CART
                },
                onSellerClick = {
                    sellerDetail = null
                    sellerDetailLoading = true
                    currentScreen = Screen.SELLER_CERTIFICATE
                    scope.launch {
                        sellerDetail = CatalogRepository.loadShopDetail(product.sellerName)
                        sellerDetailLoading = false
                    }
                },
                onReviewClick = { currentScreen = Screen.REVIEW }
            )
        }

        Screen.CART -> CartScreen(
            cartItems = cartItems,
            onBack = { currentScreen = cartBackTo },
            onBrowseProducts = { currentScreen = Screen.HOME },
            onRemoveItem = { item ->
                cartItems = cartItems - item
            },
            onQuantityChange = { item, newQty ->
                cartItems = cartItems.map {
                    if (it.product.id == item.product.id) it.copy(quantity = newQty)
                    else it
                }
            },
            onCheckout = {
                currentScreen = Screen.CHECKOUT
            }
        )

        Screen.CHECKOUT -> CheckoutScreen(
            cartItems = cartItems,
            savedAddresses = addresses,
            onBack = { currentScreen = Screen.CART },
            onPlaceOrder = { address, notes, payment ->
                val result = OrderRepository.placeOrder(cartItems, address, notes, payment)
                if (result.error != null) {
                    result.error
                } else {
                    loadOrdersNow()
                    selectedOrderId = result.codes.firstOrNull()
                    null
                }
            },
            onOrderSuccess = {
                trackingBackTo = Screen.HOME
                cartItems = emptyList()
                currentScreen = Screen.ORDER_TRACKING
            }
        )

        Screen.ORDER_TRACKING -> {
            LaunchedEffect(selectedOrderId) { loadOrdersNow() }
            val order = buyerOrders.find { it.id == selectedOrderId }
            OrderTrackingScreen(
                orderId = selectedOrderId ?: "-",
                sellerName = order?.sellerName ?: "-",
                status = order?.status ?: OrderStatus.NEW,
                onBack = { currentScreen = trackingBackTo }
            )
        }

        Screen.ORDER_HISTORY -> OrderHistoryScreen(
            orders = buyerOrders,
            isLoading = ordersLoading,
            errorMessage = ordersError,
            onRetry = { reloadOrders() },
            onBack = { currentScreen = Screen.PROFILE },
            onTrackOrder = { orderId ->
                selectedOrderId = orderId
                trackingBackTo = Screen.ORDER_HISTORY
                currentScreen = Screen.ORDER_TRACKING
            },
            onReorder = { itemLines ->
                val (updatedCart, skipped) = reorderItems(itemLines, products, cartItems)
                if (skipped < itemLines.size) {
                    cartItems = updatedCart
                    cartBackTo = Screen.ORDER_HISTORY
                    currentScreen = Screen.CART
                }
                if (skipped > 0) {
                    val message = if (skipped == itemLines.size) {
                        "Produk pada pesanan ini sudah tidak tersedia"
                    } else {
                        "$skipped produk sudah tidak tersedia dan dilewati"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        )

        Screen.REVIEW -> selectedProduct?.let { product ->
            ReviewScreen(
                productName = product.name,
                reviews = reviews.filter { it.productId == product.id },
                onBack = { currentScreen = Screen.PRODUCT_DETAIL },
                onSubmitReview = { rating, comment ->
                    scope.launch {
                        val error = CatalogRepository.submitReview(product.id, rating, comment)
                        if (error != null) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        } else {
                            loadCatalogNow()
                            selectedProduct = products.find { it.id == product.id } ?: product
                            Toast.makeText(context, "Ulasan terkirim", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        Screen.SELLER_CERTIFICATE -> selectedProduct?.let { product ->
            SellerCertificateScreen(
                sellerName = product.sellerName,
                products = products,
                shop = sellerDetail?.shop,
                certificate = sellerDetail?.certificate,
                isLoading = sellerDetailLoading,
                onBack = { currentScreen = Screen.PRODUCT_DETAIL }
            )
        }

        Screen.SELLER_PENDING -> SellerPendingScreen(
            shopName = currentSellerName,
            ownerName = currentUser?.name ?: "",
            status = currentUser?.sellerStatus ?: SellerStatus.PENDING,
            rejectionReason = currentUser?.rejectionReason,
            certificate = certificates[currentSellerName],
            onSubmitCertificate = { cert, uri ->
                if (uri == null) {
                    "Pilih file dokumen sertifikat"
                } else {
                    val bytes = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bytes == null) {
                        "Tidak dapat membaca file. Pilih ulang file"
                    } else {
                        val error = SellerRepository.submitCertificate(cert, bytes)
                        if (error == null) {
                            SellerRepository.loadMyCertificate()?.let { saved ->
                                certificates = certificates + (currentSellerName to saved)
                            }
                            accounts = accounts.map {
                                if (it.id == currentUserId) {
                                    it.copy(sellerStatus = SellerStatus.PENDING, rejectionReason = null)
                                } else it
                            }
                            Toast.makeText(context, "Sertifikat terkirim, menunggu verifikasi admin", Toast.LENGTH_SHORT).show()
                        }
                        error
                    }
                }
            },
            onLogout = { logout() }
        )

        Screen.ADMIN_DASHBOARD -> AdminDashboardScreen(
            accounts = adminAccounts,
            products = products,
            orders = adminOrders,
            reviews = reviews,
            onOpenVerification = {
                reloadAdminData()
                currentScreen = Screen.ADMIN_VERIFICATION
            },
            onOpenUsers = {
                reloadAdminData()
                currentScreen = Screen.ADMIN_USERS
            },
            onLogout = { logout() }
        )

        Screen.ADMIN_USERS -> AdminUsersScreen(
            accounts = adminAccounts,
            onToggleActive = { accountId, active ->
                scope.launch {
                    val error = AdminRepository.setUserActive(accountId, active)
                    if (error == null) {
                        Toast.makeText(
                            context,
                            if (active) "Akun diaktifkan" else "Akun dinonaktifkan",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context, "Gagal: $error", Toast.LENGTH_LONG).show()
                    }
                    loadAdminDataNow()
                }
            },
            onBack = { currentScreen = Screen.ADMIN_DASHBOARD }
        )

        Screen.ADMIN_VERIFICATION -> AdminVerificationScreen(
            accounts = adminAccounts,
            certificates = adminCertificates,
            onApprove = { accountId ->
                scope.launch {
                    val error = AdminRepository.reviewShop(accountId, true, null)
                    if (error == null) {
                        Toast.makeText(context, "Penjual disetujui", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal: $error", Toast.LENGTH_LONG).show()
                    }
                    loadAdminDataNow()
                }
            },
            onReject = { accountId, reason ->
                scope.launch {
                    val error = AdminRepository.reviewShop(accountId, false, reason)
                    if (error == null) {
                        Toast.makeText(context, "Pengajuan ditolak", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal: $error", Toast.LENGTH_LONG).show()
                    }
                    loadAdminDataNow()
                }
            },
            onViewDocument = { shopName ->
                val path = adminDocPaths[shopName]
                if (path == null) {
                    Toast.makeText(context, "Dokumen tidak ditemukan", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        val url = AdminRepository.createDocumentUrl(path)
                        if (url == null) {
                            Toast.makeText(context, "Gagal membuka dokumen", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Tidak ada aplikasi untuk membuka dokumen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            onBack = { currentScreen = Screen.ADMIN_DASHBOARD }
        )

        Screen.SELLER_DASHBOARD -> SellerDashboardScreen(
            orders = shopOrders,
            shopName = currentSellerName,
            certificate = certificates[currentSellerName]?.takeIf { it.status == CertStatus.ACTIVE },
            onLogout = { logout() },
            onManageProducts = { currentScreen = Screen.SELLER_PRODUCTS },
            onManageOrders = {
                reloadShopOrders()
                currentScreen = Screen.SELLER_ORDERS
            },
            onSellerProfile = { currentScreen = Screen.SELLER_SHOP }
        )

        Screen.SELLER_ORDERS -> SellerOrdersScreen(
            orders = shopOrders,
            onBack = { currentScreen = Screen.SELLER_DASHBOARD },
            onUpdateStatus = { id, newStatus ->
                scope.launch {
                    val error = SellerRepository.updateOrderStatus(id, newStatus)
                    if (error != null) {
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                    loadShopOrdersNow()
                }
            }
        )

        Screen.SELLER_PRODUCTS -> SellerProductsScreen(
            sellerName = currentSellerName,
            products = products,
            onBack = { currentScreen = Screen.SELLER_DASHBOARD },
            onSaveProduct = { saved ->
                scope.launch {
                    val isNew = products.none { it.id == saved.id }
                    val error = SellerRepository.saveProduct(saved, isNew)
                    if (error != null) {
                        Toast.makeText(context, "Gagal menyimpan: $error", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Produk disimpan", Toast.LENGTH_SHORT).show()
                    }
                    loadCatalogNow()
                }
            },
            onDeleteProduct = { id ->
                scope.launch {
                    val error = SellerRepository.deleteProduct(id)
                    if (error != null) {
                        Toast.makeText(context, "Gagal menghapus: $error", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Produk dihapus", Toast.LENGTH_SHORT).show()
                    }
                    loadCatalogNow()
                }
            }
        )

        Screen.SELLER_SHOP -> {
            val shop = shops[currentSellerName]
            if (shop != null) {
                SellerShopProfileScreen(
                    shop = shop,
                    onBack = { currentScreen = Screen.SELLER_DASHBOARD },
                    onSave = { updated ->
                        scope.launch {
                            val error = SellerRepository.updateShop(updated)
                            if (error == null) {
                                shops = shops + (currentSellerName to updated)
                                Toast.makeText(context, "Profil toko diperbarui", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal menyimpan: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun createOrdersFromCart(
    cartItems: List<CartItem>,
    buyerName: String,
    existingCount: Int
): List<SellerOrder> {
    val today = LocalDate.now()
    val datePart = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val dateLabel = today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID")))

    return cartItems
        .groupBy { it.product.sellerName }
        .entries
        .mapIndexed { index, (sellerName, items) ->
            SellerOrder(
                id = "TS-$datePart-${(existingCount + index + 1).toString().padStart(3, '0')}",
                customerName = buyerName,
                items = items.joinToString(", ") { "${it.product.name} x${it.quantity}" },
                total = items.sumOf { it.product.price * it.quantity },
                status = OrderStatus.NEW,
                sellerName = sellerName,
                date = dateLabel,
                deliveryFee = DELIVERY_FEE_PER_SELLER
            )
        }
}

private fun todayLabel(pattern: String): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern(pattern, Locale("id", "ID")))

private fun reorderItems(
    itemLines: List<String>,
    products: List<Product>,
    cartItems: List<CartItem>
): Pair<List<CartItem>, Int> {
    var updated = cartItems
    var skipped = 0

    itemLines.forEach { line ->
        val match = Regex("^(.*) x(\\d+)$").find(line.trim())
        val name = match?.groupValues?.get(1) ?: line.trim()
        val quantity = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
        val product = products.find { it.name.equals(name, ignoreCase = true) }

        if (product == null) {
            skipped++
        } else {
            val existing = updated.find { it.product.id == product.id }
            updated = if (existing != null) {
                updated.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + quantity)
                    else it
                }
            } else {
                updated + CartItem(product, quantity)
            }
        }
    }

    return updated to skipped
}

@Composable
fun SplashScreen(
    ready: Boolean,
    onFinished: () -> Unit
) {
    val brandColor = Color(0xFFFF6B35)
    var minTimePassed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000)
        minTimePassed = true
    }

    LaunchedEffect(minTimePassed, ready) {
        if (minTimePassed && ready) onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brandColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🥄",
            fontSize = 80.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TinySpoon",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Makanan bergizi untuk si kecil",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}