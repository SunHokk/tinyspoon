package id.tinyspoon.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun tinySpoonFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    errorTextColor = TextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White,
    focusedBorderColor = OrangePrimary,
    unfocusedBorderColor = Color(0xFFE0E0E0),
    errorBorderColor = Color.Red,
    errorLabelColor = Color.Red,
    focusedLabelColor = OrangePrimary,
    cursorColor = OrangePrimary,
    focusedPlaceholderColor = Color(0xFF9E9E9E),
    unfocusedPlaceholderColor = Color(0xFF9E9E9E)
)