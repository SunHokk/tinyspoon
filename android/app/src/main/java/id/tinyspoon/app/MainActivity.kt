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
import id.tinyspoon.app.ui.screens.onboarding.OnboardingScreen
import id.tinyspoon.app.ui.theme.TinySpoonTheme
import id.tinyspoon.app.ui.screens.auth.AuthScreen
import id.tinyspoon.app.ui.screens.home.HomeScreen
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
}

enum class Screen {
    SPLASH, ONBOARDING, AUTH, HOME
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }

    when (currentScreen) {
        Screen.SPLASH -> SplashScreen(
            onFinished = { currentScreen = Screen.ONBOARDING }
        )
        Screen.ONBOARDING -> OnboardingScreen(
            onFinish = { currentScreen = Screen.AUTH }
        )
        Screen.AUTH -> AuthScreen(
            onAuthSuccess = {currentScreen = Screen.HOME}
        )
        Screen.HOME -> HomeScreen(
            onProductClick = { product ->
            }
        )
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
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