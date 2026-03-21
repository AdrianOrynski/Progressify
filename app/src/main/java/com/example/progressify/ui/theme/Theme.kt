package com.example.progressify.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FantasyGold        = Color(0xFFFFD700)
val FantasyGoldDim     = Color(0xFFB8960C)
val DeepDragonRed      = Color(0xFF8B0000)
val DragonRedLight     = Color(0xFFBF2222)
val Parchment          = Color(0xFFF5E6BE)
val ParchmentDim       = Color(0xFFBFAD88)
val DarkWood           = Color(0xFF120D07)
val AncientBrown       = Color(0xFF2B1B11)
val AncientBrownLight  = Color(0xFF3E2A1A)
val ShadowBlack        = Color(0xFF0A0704)
val IronGray           = Color(0xFF3C3C3C)

private val ColorScheme = darkColorScheme(
    primary          = FantasyGold,
    onPrimary        = DarkWood,
    primaryContainer = AncientBrown,
    secondary        = DeepDragonRed,
    onSecondary      = Parchment,
    background       = DarkWood,
    onBackground     = Parchment,
    surface          = AncientBrown,
    onSurface        = Parchment,
    surfaceVariant   = AncientBrownLight,
    onSurfaceVariant = ParchmentDim,
    error            = DragonRedLight,
    outline          = FantasyGoldDim
)

private val AppTypography = Typography(
    displaySmall  = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = 2.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.8.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.5.sp)
)

@Composable
fun ProgressifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography  = AppTypography,
        content     = content
    )
}