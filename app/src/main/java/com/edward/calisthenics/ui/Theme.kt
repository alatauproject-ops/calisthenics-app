package com.edward.calisthenics.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.data.Category
import com.edward.calisthenics.data.Kind

val Accent = Color(0xFFFF7A3D)
val AccentSoft = Color(0xFFFFB37A)
val Success = Color(0xFF3DDC97)
val Rest = Color(0xFF4FC3F7)
val Sea = Color(0xFF2EC4C6)
val Bg = Color(0xFF0D0F14)
val Surface1 = Color(0xFF171A22)
val Surface2 = Color(0xFF222632)
val Surface3 = Color(0xFF2C3140)
val TextMain = Color(0xFFF3F4F7)
val Muted = Color(0xFF98A0B3)
val Danger = Color(0xFFFF6B6B)

val HeroGradient = Brush.linearGradient(listOf(Color(0xFFFF8A3D), Color(0xFFE8456B)))
val SeaGradient = Brush.linearGradient(listOf(Color(0xFF1FA2C4), Color(0xFF2EC4A6)))
val CardGradient = Brush.verticalGradient(listOf(Color(0xFF1F2330), Color(0xFF171A22)))

fun kindColor(k: Kind): Color = when (k) {
    Kind.STRENGTH -> Color(0xFFFF7A3D)
    Kind.CIRCUIT -> Color(0xFFFFA94D)
    Kind.BEACH -> Sea
    Kind.HIIT -> Color(0xFFE8456B)
    Kind.CORE -> Color(0xFFB18CFF)
    Kind.SINGLE -> Accent
}

fun kindGradient(k: Kind): Brush = when (k) {
    Kind.BEACH -> SeaGradient
    Kind.CORE -> Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFFB18CFF)))
    Kind.HIIT -> Brush.linearGradient(listOf(Color(0xFFE8456B), Color(0xFFFF7A3D)))
    else -> HeroGradient
}

fun categoryColor(c: Category): Color = when (c) {
    Category.WARMUP -> Color(0xFFFFC857)
    Category.LEGS -> Color(0xFFFF7A3D)
    Category.PUSH -> Color(0xFFE8456B)
    Category.PULL -> Color(0xFF4FC3F7)
    Category.CORE -> Color(0xFFB18CFF)
    Category.CARDIO -> Sea
    Category.STRETCH -> Success
}

private val colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Success,
    onSecondary = Color(0xFF04150D),
    tertiary = Rest,
    background = Bg,
    onBackground = TextMain,
    surface = Surface1,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface3,
    surfaceContainerLow = Surface1,
    outline = Color(0xFF3A4152),
    outlineVariant = Color(0xFF2A2F3C),
    error = Danger,
)

private val base = Typography()
private val typography = Typography(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
)

val Caption = TextStyle(fontSize = 12.sp, color = Muted, letterSpacing = 0.4.sp)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
