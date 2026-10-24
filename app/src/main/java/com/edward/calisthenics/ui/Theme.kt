package com.edward.calisthenics.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFFF8A3D)
val Success = Color(0xFF4ADE80)
val Bg = Color(0xFF0F1115)
val Surface1 = Color(0xFF181B22)
val Surface2 = Color(0xFF232733)
val Muted = Color(0xFF9AA3B2)

private val colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF1A0E05),
    secondary = Success,
    onSecondary = Color(0xFF05160B),
    background = Bg,
    onBackground = Color(0xFFF2F3F5),
    surface = Surface1,
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    outline = Color(0xFF3A4050),
    error = Color(0xFFF87171),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
