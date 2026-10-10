package id.tinyspoon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import id.tinyspoon.app.ui.screens.profile.FavoritesScreen
import id.tinyspoon.app.ui.screens.onboarding.OnboardingScreen
import id.tinyspoon.app.ui.theme.TinySpoonTheme
import id.tinyspoon.app.ui.screens.auth.AuthScreen
import id.tinyspoon.app.ui.screens.home.HomeScreen
import id.tinyspoon.app.ui.screens.product.ProductDetailScreen
import id.tinyspoon.app.ui.screens.home.Product
import id.tinyspoon.app.ui.screens.cart.CartItem
import id.tinyspoon.app.ui.screens.cart.CartScreen
import id.tinyspoon.app.ui.screens.order.CheckoutScreen
import id.tinyspoon.app.ui.screens.order.OrderTrackingScreen
import id.tinyspoon.app.ui.screens.profile.ProfileScreen
import id.tinyspoon.app.ui.screens.order.OrderHistoryScreen
import id.tinyspoon.app.ui.screens.product.SellerCertificateScreen
import id.tinyspoon.app.ui.screens.product.ReviewScreen
import id.tinyspoon.app.ui.screens.seller.SellerDashboardScreen
import id.tinyspoon.app.ui.screens.home.dummyProducts
import id.tinyspoon.app.ui.screens.seller.SellerProductsScreen
import id.tinyspoon.app.ui.screens.seller.SellerOrdersScreen
import id.tinyspoon.app.ui.screens.seller.dummySellerOrders
import id.tinyspoon.app.ui.screens.order.DELIVERY_FEE_PER_SELLER
import id.tinyspoon.app.ui.screens.seller.OrderStatus
import id.tinyspoon.app.ui.screens.seller.SellerOrder
import id.tinyspoon.app.ui.screens.order.dummyOrderHistory
import id.tinyspoon.app.ui.screens.product.Review
import id.tinyspoon.app.ui.screens.product.seedReviewsFor
import id.tinyspoon.app.ui.screens.product.withRatingsFrom
import id.tinyspoon.app.ui.screens.profile.AboutScreen
import id.tinyspoon.app.ui.screens.profile.AddressScreen
import id.tinyspoon.app.ui.screens.profile.NotificationPrefs
import id.tinyspoon.app.ui.screens.profile.NotificationSettingsScreen
import id.tinyspoon.app.ui.screens.profile.dummyAddresses
import id.tinyspoon.app.ui.screens.profile.withDefault
import id.tinyspoon.app.ui.screens.profile.withSaved
import id.tinyspoon.app.ui.screens.profile.withoutAddress
import id.tinyspoon.app.ui.screens.seller.SellerShopProfileScreen
import id.tinyspoon.app.ui.screens.seller.dummyShops
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
    SPLASH, ONBOARDING, AUTH, HOME, PRODUCT_DETAIL, CART, CHECKOUT, SELLER_CERTIFICATE, ORDER_TRACKING, PROFILE, ORDER_HISTORY, REVIEW, SELLER_DASHBOARD, SELLER_PRODUCTS, SELLER_ORDERS, FAVORITES, ADDRESSES, NOTIFICATION_SETTINGS, ABOUT, SELLER_SHOP
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    val seedReviews = remember { seedReviewsFor(dummyProducts) }
    var reviews by remember { mutableStateOf(seedReviews) }
    var products by remember { mutableStateOf(withRatingsFrom(dummyProducts, seedReviews)) }
    var sellerOrders by remember { mutableStateOf(dummySellerOrders) }
    val buyerName = "Gilbert"
    val currentSellerName = "Dapur Bunda"
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var trackingBackTo by remember { mutableStateOf(Screen.HOME) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }
    var detailBackTo by remember { mutableStateOf(Screen.HOME) }
    var cartBackTo by remember { mutableStateOf(Screen.HOME) }
    var addresses by remember { mutableStateOf(dummyAddresses) }
    var notifPrefs by remember { mutableStateOf(NotificationPrefs()) }
    var appRating by remember { mutableStateOf(0) }
    var shops by remember { mutableStateOf(dummyShops) }
    val context = LocalContext.current
    val favoriteProducts = products.filter { it.id in favoriteIds }
    val backTarget: Screen? = when (currentScreen) {
        Screen.SPLASH, Screen.ONBOARDING, Screen.AUTH, Screen.HOME -> null
        Screen.PRODUCT_DETAIL -> detailBackTo
        Screen.CART -> cartBackTo
        Screen.CHECKOUT -> Screen.CART
        Screen.SELLER_CERTIFICATE, Screen.REVIEW -> Screen.PRODUCT_DETAIL
        Screen.ORDER_TRACKING -> trackingBackTo
        Screen.PROFILE -> Screen.HOME
        Screen.ORDER_HISTORY, Screen.FAVORITES, Screen.ADDRESSES,
        Screen.NOTIFICATION_SETTINGS, Screen.ABOUT, Screen.SELLER_DASHBOARD -> Screen.PROFILE
        Screen.SELLER_PRODUCTS, Screen.SELLER_ORDERS, Screen.SELLER_SHOP -> Screen.SELLER_DASHBOARD
    }

    BackHandler(enabled = backTarget != null) {
        backTarget?.let { currentScreen = it }
    }

    when (currentScreen) {
        Screen.SPLASH -> SplashScreen(
            onFinished = { currentScreen = Screen.ONBOARDING }
        )

        Screen.ONBOARDING -> OnboardingScreen(
            onFinish = { currentScreen = Screen.AUTH }
        )

        Screen.AUTH -> AuthScreen(
            onAuthSuccess = { currentScreen = Screen.HOME }
        )

        Screen.HOME -> HomeScreen(
            products = products,
            onProductClick = { product ->
                selectedProduct = product
                detailBackTo = Screen.HOME
                currentScreen = Screen.PRODUCT_DETAIL
            },
            onProfileClick = { currentScreen = Screen.PROFILE }
        )

        Screen.PROFILE -> ProfileScreen(
            orderCount = sellerOrders.count { it.customerName == buyerName } + dummyOrderHistory.size,
            reviewCount = reviews.count { it.userName == buyerName },
            favoriteCount = favoriteProducts.size,
            appRating = appRating,
            onBack = { currentScreen = Screen.HOME },
            onLogout = { currentScreen = Screen.AUTH },
            onOrderHistoryClick = { currentScreen = Screen.ORDER_HISTORY },
            onFavoritesClick = { currentScreen = Screen.FAVORITES },
            onAddressesClick = { currentScreen = Screen.ADDRESSES },
            onNotificationsClick = { currentScreen = Screen.NOTIFICATION_SETTINGS },
            onAboutClick = { currentScreen = Screen.ABOUT },
            onSubmitAppRating = { rating, _ ->
                appRating = rating
                Toast.makeText(context, "Terima kasih atas penilaianmu!", Toast.LENGTH_SHORT).show()
            },
            onSellerDashboardClick = { currentScreen = Screen.SELLER_DASHBOARD }
        )

        Screen.ADDRESSES -> AddressScreen(
            addresses = addresses,
            onBack = { currentScreen = Screen.PROFILE },
            onSaveAddress = { saved -> addresses = addresses.withSaved(saved) },
            onDeleteAddress = { id -> addresses = addresses.withoutAddress(id) },
            onSetDefault = { id -> addresses = addresses.withDefault(id) }
        )

        Screen.NOTIFICATION_SETTINGS -> NotificationSettingsScreen(
            prefs = notifPrefs,
            onBack = { currentScreen = Screen.PROFILE },
            onPrefsChange = { notifPrefs = it }
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
            onRemoveFavorite = { product ->
                favoriteIds = favoriteIds - product.id
            }
        )

        Screen.PRODUCT_DETAIL -> selectedProduct?.let { product ->
            ProductDetailScreen(
                product = product,
                isFavorite = product.id in favoriteIds,
                onToggleFavorite = {
                    favoriteIds = if (product.id in favoriteIds) favoriteIds - product.id
                    else favoriteIds + product.id
                },
                onBack = { currentScreen = detailBackTo },
                onAddToCart = {
                    val existingItem = cartItems.find { it.product.id == product.id }
                    cartItems = if (existingItem != null) {
                        cartItems.map {
                            if (it.product.id == product.id) it.copy(quantity = it.quantity + 1)
                            else it
                        }
                    } else {
                        cartItems + CartItem(product, 1)
                    }
                    cartBackTo = Screen.PRODUCT_DETAIL
                    currentScreen = Screen.CART
                },
                onSellerClick = { currentScreen = Screen.SELLER_CERTIFICATE },
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
            onOrderSuccess = {
                val newOrders = createOrdersFromCart(cartItems, buyerName, sellerOrders.size)
                sellerOrders = newOrders + sellerOrders
                selectedOrderId = newOrders.firstOrNull()?.id
                trackingBackTo = Screen.HOME
                cartItems = emptyList()
                currentScreen = Screen.ORDER_TRACKING
            }
        )

        Screen.ORDER_TRACKING -> {
            val order = sellerOrders.find {
                it.id == selectedOrderId && it.customerName == buyerName
            }
            OrderTrackingScreen(
                orderId = selectedOrderId ?: "-",
                sellerName = order?.sellerName ?: currentSellerName,
                status = order?.status ?: OrderStatus.DONE,
                onBack = { currentScreen = trackingBackTo }
            )
        }

        Screen.ORDER_HISTORY -> OrderHistoryScreen(
            orders = sellerOrders.filter { it.customerName == buyerName },
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
                    val newReview = Review(
                        id = UUID.randomUUID().toString(),
                        productId = product.id,
                        userName = buyerName,
                        rating = rating,
                        comment = comment,
                        date = todayLabel("d MMM yyyy"),
                        babyAge = ""
                    )
                    val updatedReviews = listOf(newReview) + reviews.filterNot {
                        it.productId == product.id && it.userName == buyerName
                    }
                    val updatedProducts = withRatingsFrom(products, updatedReviews)
                    reviews = updatedReviews
                    products = updatedProducts
                    selectedProduct = updatedProducts.find { it.id == product.id } ?: product
                }
            )
        }

        Screen.SELLER_CERTIFICATE -> selectedProduct?.let { product ->
            SellerCertificateScreen(
                sellerName = product.sellerName,
                products = products,
                shop = shops[product.sellerName],
                onBack = { currentScreen = Screen.PRODUCT_DETAIL }
            )
        }

        Screen.SELLER_DASHBOARD -> SellerDashboardScreen(
            orders = sellerOrders.filter { it.sellerName == currentSellerName },
            onBack = { currentScreen = Screen.PROFILE },
            onManageProducts = { currentScreen = Screen.SELLER_PRODUCTS },
            onManageOrders = { currentScreen = Screen.SELLER_ORDERS },
            onSellerProfile = { currentScreen = Screen.SELLER_SHOP }
        )

        Screen.SELLER_ORDERS -> SellerOrdersScreen(
            orders = sellerOrders.filter { it.sellerName == currentSellerName },
            onBack = { currentScreen = Screen.SELLER_DASHBOARD },
            onUpdateStatus = { id, newStatus ->
                sellerOrders = sellerOrders.map {
                    if (it.id == id) it.copy(status = newStatus) else it
                }
            }
        )

        Screen.SELLER_PRODUCTS -> SellerProductsScreen(
            sellerName = "Dapur Bunda",
            products = products,
            onBack = { currentScreen = Screen.SELLER_DASHBOARD },
            onSaveProduct = { saved ->
                products = if (products.any { it.id == saved.id }) {
                    products.map { if (it.id == saved.id) saved else it }
                } else {
                    products + saved
                }
            },
            onDeleteProduct = { id ->
                products = products.filterNot { it.id == id }
            }
        )

        Screen.SELLER_SHOP -> {
            val shop = shops[currentSellerName]
            if (shop != null) {
                SellerShopProfileScreen(
                    shop = shop,
                    onBack = { currentScreen = Screen.SELLER_DASHBOARD },
                    onSave = { updated ->
                        shops = shops + (currentSellerName to updated)
                        Toast.makeText(context, "Profil toko diperbarui", Toast.LENGTH_SHORT).show()
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
    onFinished: () -> Unit) {
    val brandColor = Color(0xFFFF6B35)

    LaunchedEffect(Unit) {
        delay(2000)
        onFinished()
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