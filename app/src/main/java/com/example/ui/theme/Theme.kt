package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class Win10Tokens(
    val isDark: Boolean,
    val accent: Color,
    val accentPressed: Color,
    val accentHighlight: Color,
    val chromeBackground: Color,
    val cardBackground: Color,
    val elevatedSurface: Color,
    val border: Color,
    val borderStrong: Color,
    val transferGreen: Color,
    val transferTrack: Color,
    val pausedAmber: Color,
    val errorRed: Color,
    val urgentColor: Color,
    val quickColor: Color,
    val casualColor: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

private val LightWin10Tokens = Win10Tokens(
    isDark = false,
    accent = Win10Blue,
    accentPressed = Win10BlueDark,
    accentHighlight = Win10SkyBlue,
    chromeBackground = Win10LightChrome,
    cardBackground = Win10LightCard,
    elevatedSurface = Win10LightElevated,
    border = Win10LightBorder,
    borderStrong = Color(0xFF8C8C8C),
    transferGreen = Win10TransferGreen,
    transferTrack = Color(0xFFE1E1E1),
    pausedAmber = Win10PausedAmber,
    errorRed = Win10ErrorRed,
    urgentColor = Win10ErrorRed,
    quickColor = Win10Blue,
    casualColor = Color(0xFF5C6B7A),
    textPrimary = Win10LightTextPrimary,
    textSecondary = Win10LightTextSecondary
)

private val DarkWin10Tokens = Win10Tokens(
    isDark = true,
    accent = Win10BlueLight,
    accentPressed = Win10Blue,
    accentHighlight = Win10SkyBlue,
    chromeBackground = Win10DarkChrome,
    cardBackground = Win10DarkCard,
    elevatedSurface = Win10DarkElevated,
    border = Win10DarkBorder,
    borderStrong = Color(0xFF666666),
    transferGreen = Win10TransferGreenLight,
    transferTrack = Color(0xFF3A3A3A),
    pausedAmber = Win10PausedAmber,
    errorRed = Color(0xFFFF4B5C),
    urgentColor = Color(0xFFFF5252),
    quickColor = Win10SkyBlue,
    casualColor = Color(0xFF8C9BA8),
    textPrimary = Win10DarkTextPrimary,
    textSecondary = Win10DarkTextSecondary
)

val LocalWin10Tokens = staticCompositionLocalOf { LightWin10Tokens }

// Sharp Windows 10 rectangular geometry (0dp-2dp)
val Win10Shapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp)
)

private val WinLightColorScheme = lightColorScheme(
    primary = Win10Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6ECFC),
    onPrimaryContainer = Win10BlueDeep,
    secondary = Win10BlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6F2FB),
    onSecondaryContainer = Win10BlueDeep,
    tertiary = Win10TransferGreen,
    onTertiary = Color.White,
    background = Win10LightBackground,
    onBackground = Win10LightTextPrimary,
    surface = Win10LightBackground,
    onSurface = Win10LightTextPrimary,
    surfaceVariant = Win10LightChrome,
    onSurfaceVariant = Win10LightTextSecondary,
    outline = Win10LightBorder,
    error = Win10ErrorRed,
    onError = Color.White
)

private val WinDarkColorScheme = darkColorScheme(
    primary = Win10BlueLight,
    onPrimary = Color(0xFF001E38),
    primaryContainer = Win10BlueDeep,
    onPrimaryContainer = Color(0xFFD6ECFC),
    secondary = Win10SkyBlue,
    onSecondary = Color(0xFF00263D),
    secondaryContainer = Color(0xFF1B3A54),
    onSecondaryContainer = Color(0xFFCDE9FF),
    tertiary = Win10TransferGreenLight,
    onTertiary = Color(0xFF002B07),
    background = Win10DarkBackground,
    onBackground = Win10DarkTextPrimary,
    surface = Win10DarkBackground,
    onSurface = Win10DarkTextPrimary,
    surfaceVariant = Win10DarkChrome,
    onSurfaceVariant = Win10DarkTextSecondary,
    outline = Win10DarkBorder,
    error = Color(0xFFFF4B5C),
    onError = Color.White
)

enum class WinThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun WinFetchTheme(
    themeMode: WinThemeMode = WinThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        WinThemeMode.SYSTEM -> isSystemInDarkTheme()
        WinThemeMode.LIGHT -> false
        WinThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) WinDarkColorScheme else WinLightColorScheme
    val tokens = if (isDark) DarkWin10Tokens else LightWin10Tokens

    CompositionLocalProvider(LocalWin10Tokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WinTypography,
            shapes = Win10Shapes,
            content = content
        )
    }
}
