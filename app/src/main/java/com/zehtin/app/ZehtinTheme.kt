package com.zehtin.app

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Zehtin Colors
val ZehtinBg = Color(0xFFF5F0E8)
val ZehtinSurface = Color(0xFFEDE8DF)
val ZehtinDeep = Color(0xFF1A1A14)
val ZehtinOlive = Color(0xFF4A5240)
val ZehtinAccent = Color(0xFFC8693A)
val ZehtinGreen = Color(0xFF8FAE6B)
val ZehtinMuted = Color(0xFF7A7568)
val ZehtinBorder = Color(0xFFD8D2C6)

private val ZehtinColorScheme = lightColorScheme(
    primary = ZehtinAccent,
    secondary = ZehtinOlive,
    tertiary = ZehtinGreen,
    background = ZehtinBg,
    surface = ZehtinSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = ZehtinDeep,
    onSurface = ZehtinDeep,
)

@Composable
fun ZehtinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZehtinColorScheme,
        content = content
    )
}