package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import java.util.Locale

private val ToMusicDarkColorScheme = darkColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo500.copy(alpha = 0.25f),
    onPrimaryContainer = Indigo100,
    secondary = Sky400,
    onSecondary = Zinc950,
    secondaryContainer = Sky400.copy(alpha = 0.18f),
    onSecondaryContainer = Sky200,
    tertiary = Emerald400,
    onTertiary = Zinc950,
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc900,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc400,
    error = Red600,
    onError = Color.White,
    outline = GlassBorder
)

private val ToMusicLightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo500.copy(alpha = 0.14f),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0284C7).copy(alpha = 0.14f),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Emerald600,
    onTertiary = Color.White,
    background = Color(0xFFE9EFF8),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = Red600,
    onError = Color.White,
    outline = Color(0xFF0F172A).copy(alpha = 0.12f)
)

fun resolveIsPersian(language: AppLanguage): Boolean {
    return when (language) {
        AppLanguage.FA -> true
        AppLanguage.EN -> false
        AppLanguage.SYSTEM -> Locale.getDefault().language.equals("fa", ignoreCase = true)
    }
}

@Composable
fun ToMusicTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    language: AppLanguage = AppLanguage.SYSTEM,
    content: @Composable () -> Unit
) {
    val isFa = resolveIsPersian(language)
    val isDark = themeMode == AppThemeMode.DARK
    val colors = if (isDark) DarkToMusicColors else LightToMusicColors
    val strings = if (isFa) FaStrings else EnStrings
    val layoutDirection = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppColors provides colors,
        LocalStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = if (isDark) ToMusicDarkColorScheme else ToMusicLightColorScheme,
            typography = ToMusicTypography,
            content = content
        )
    }
}
