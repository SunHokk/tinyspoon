package id.tinyspoon.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = White,
    background = TextPrimary,
    surface = Color(0xFF1E1E1E),
    onBackground = White,
    onSurface = White,
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = White,
    background = BackgroundCream,
    surface = White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun TinySpoonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}