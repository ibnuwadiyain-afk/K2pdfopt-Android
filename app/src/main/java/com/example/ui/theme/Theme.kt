package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent400,
    onPrimary = SlateNavy950,
    primaryContainer = CyanAccent700,
    onPrimaryContainer = Color.White,
    secondary = AmberHighlight400,
    onSecondary = SlateNavy950,
    secondaryContainer = AmberHighlight700,
    onSecondaryContainer = Color.White,
    tertiary = EmeraldSuccess,
    background = SlateNavy950,
    onBackground = Color(0xFFF1F5F9),
    surface = SlateNavy900,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SlateNavy800,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = RoseWarning
)

private val LightColorScheme = lightColorScheme(
    primary = CyanAccent700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = SlateNavy900,
    secondary = AmberHighlight700,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = SlateNavy900,
    tertiary = EmeraldSuccess,
    background = PaperLightBg,
    onBackground = InkDarkText,
    surface = PaperLightSurface,
    onSurface = InkDarkText,
    surfaceVariant = PaperLightCard,
    onSurfaceVariant = InkMutedText,
    error = RoseWarning
)

private val EinkColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF222222),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDDDDD),
    onSecondaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF111111),
    error = Color.Black
)

@Composable
fun K2PdfOptTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    einkHighContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        einkHighContrast -> EinkColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
