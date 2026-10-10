package com.example.reto2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AurexColorScheme = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = CardWhite,
    secondary = BrandMint,
    onSecondary = BrandInk,
    tertiary = BrandInk,
    background = PageBackground,
    onBackground = BrandInk,
    surface = CardWhite,
    onSurface = BrandInk,
    surfaceVariant = PageBackground,
    onSurfaceVariant = MutedText,
    error = androidx.compose.ui.graphics.Color(0xFFB04444),
    onError = CardWhite
)

@Composable
fun Reto2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AurexColorScheme,
        typography = Typography,
        content = content
    )
}
