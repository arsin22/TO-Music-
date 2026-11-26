package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Zinc950 = Color(0xFF09090B)
val Zinc900 = Color(0xFF18181B)
val Zinc800 = Color(0xFF27272A)
val Zinc500 = Color(0xFF71717A)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc100 = Color(0xFFF4F4F5)

val Indigo600 = Color(0xFF4F46E5)
val Indigo500 = Color(0xFF6366F1)
val Indigo400 = Color(0xFF818CF8)
val Indigo200 = Color(0xFFC7D2FE)
val Indigo100 = Color(0xFFE0E7FF)

val Sky400 = Color(0xFF38BDF8)
val Sky300 = Color(0xFF7DD3FC)
val Sky200 = Color(0xFFBAE6FD)

val Emerald600 = Color(0xFF059669)
val Emerald400 = Color(0xFF34D399)
val Emerald300 = Color(0xFF6EE7B7)
val Emerald200 = Color(0xFFA7F3D0)

val Rose500 = Color(0xFFF43F5E)
val Rose400 = Color(0xFFFB7185)
val Rose300 = Color(0xFFFDA4AF)

val Amber400 = Color(0xFFFBBF24)
val Amber300 = Color(0xFFFCD34D)
val Amber200 = Color(0xFFFDE68A)

val Red600 = Color(0xFFDC2626)
val Red400 = Color(0xFFF87171)
val Red300 = Color(0xFFFCA5A5)
val Red200 = Color(0xFFFECACA)

val GlassSurface = Color(0xFFFFFFFF).copy(alpha = 0.045f)
val GlassSurfaceElevated = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val GlassSurfaceStrong = Color(0xFF121216).copy(alpha = 0.94f)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.10f)
val GlassBorderStrong = Color(0xFFFFFFFF).copy(alpha = 0.16f)

@Immutable
data class ToMusicColors(
    val isDark: Boolean,
    val background: Color,
    val headerBackground: Color,
    val modalBackground: Color,
    val sheetBackground: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassSurfaceSelected: Color,
    val glassInput: Color,
    val glassBorder: Color,
    val glassBorderStrong: Color,
    val textStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val primaryBtnBg: Color,
    val primaryBtnFg: Color,
    val accentSky: Color,
    val accentSkyText: Color,
    val accentSkySoft: Color,
    val accentIndigo: Color,
    val accentIndigoText: Color,
    val accentRose: Color,
    val accentRoseText: Color,
    val accentEmerald: Color,
    val accentEmeraldText: Color,
    val accentAmber: Color,
    val accentAmberText: Color,
    val accentRed: Color,
    val accentRedText: Color,
    val sliderInactive: Color,
    val handleBar: Color,
    val lyricDim: Color,
    val lyricPeak: Color,
    val ambientOrbAlpha: Float,
    val secondaryOrbAlpha: Float
)

val DarkToMusicColors = ToMusicColors(
    isDark = true,
    background = Zinc950,
    headerBackground = Zinc950.copy(alpha = 0.85f),
    modalBackground = Zinc900.copy(alpha = 0.96f),
    sheetBackground = Zinc950,
    glassSurface = Color.White.copy(alpha = 0.055f),
    glassSurfaceElevated = Color.White.copy(alpha = 0.095f),
    glassSurfaceSelected = Color.White.copy(alpha = 0.14f),
    glassInput = Color.Black.copy(alpha = 0.34f),
    glassBorder = Color.White.copy(alpha = 0.11f),
    glassBorderStrong = Color.White.copy(alpha = 0.18f),
    textStrong = Color.White,
    textPrimary = Zinc100,
    textSecondary = Zinc200,
    textMuted = Zinc400,
    textSubtle = Zinc500,
    primaryBtnBg = Color.White,
    primaryBtnFg = Zinc950,
    accentSky = Sky400,
    accentSkyText = Sky300,
    accentSkySoft = Sky200,
    accentIndigo = Indigo500,
    accentIndigoText = Indigo100,
    accentRose = Rose400,
    accentRoseText = Rose300,
    accentEmerald = Emerald400,
    accentEmeraldText = Emerald200,
    accentAmber = Amber400,
    accentAmberText = Amber200,
    accentRed = Red400,
    accentRedText = Red200,
    sliderInactive = Color.White.copy(alpha = 0.14f),
    handleBar = Color.White.copy(alpha = 0.25f),
    lyricDim = Color.White.copy(alpha = 0.28f),
    lyricPeak = Color.White,
    ambientOrbAlpha = 0.22f,
    secondaryOrbAlpha = 0.11f
)

val LightToMusicColors = ToMusicColors(
    isDark = false,
    background = Color(0xFFE9EFF8),
    headerBackground = Color(0xFFF6F9FF).copy(alpha = 0.78f),
    modalBackground = Color(0xFFF8FAFF).copy(alpha = 0.94f),
    sheetBackground = Color(0xFFEDF2FA),
    glassSurface = Color.White.copy(alpha = 0.58f),
    glassSurfaceElevated = Color.White.copy(alpha = 0.80f),
    glassSurfaceSelected = Color(0xFF4F46E5).copy(alpha = 0.12f),
    glassInput = Color.White.copy(alpha = 0.74f),
    glassBorder = Color(0xFF0F172A).copy(alpha = 0.09f),
    glassBorderStrong = Color.White.copy(alpha = 0.92f),
    textStrong = Color(0xFF020617),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textMuted = Color(0xFF475569),
    textSubtle = Color(0xFF64748B),
    primaryBtnBg = Color(0xFF0F172A),
    primaryBtnFg = Color.White,
    accentSky = Color(0xFF0284C7),
    accentSkyText = Color(0xFF0369A1),
    accentSkySoft = Color(0xFF0C4A6E),
    accentIndigo = Color(0xFF4F46E5),
    accentIndigoText = Color(0xFF3730A3),
    accentRose = Color(0xFFE11D48),
    accentRoseText = Color(0xFFBE123C),
    accentEmerald = Color(0xFF059669),
    accentEmeraldText = Color(0xFF065F46),
    accentAmber = Color(0xFFD97706),
    accentAmberText = Color(0xFF92400E),
    accentRed = Color(0xFFDC2626),
    accentRedText = Color(0xFF991B1B),
    sliderInactive = Color(0xFF0F172A).copy(alpha = 0.14f),
    handleBar = Color(0xFF0F172A).copy(alpha = 0.22f),
    lyricDim = Color(0xFF0F172A).copy(alpha = 0.26f),
    lyricPeak = Color(0xFF020617),
    ambientOrbAlpha = 0.26f,
    secondaryOrbAlpha = 0.18f
)

val LocalAppColors = staticCompositionLocalOf { DarkToMusicColors }

fun accentColorFromHue(hue: Int, saturation: Float = 0.85f, lightness: Float = 0.62f): Color {
    return Color.hsl(hue = hue.toFloat().coerceIn(0f, 360f), saturation = saturation, lightness = lightness)
}
