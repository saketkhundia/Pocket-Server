package com.saketkhundia.pocketserver.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.domain.model.AppThemeMode

/**
 * Pocket Server design system — AMOLED black + white, Apple-like type.
 * One restrained blue→purple accent, used only for: primary CTA, active
 * states, server-running glow, links, progress and key highlights.
 */

// Monochrome identity: no blue anywhere. Primary is white-on-black
// in dark theme, black-on-white in light theme.

// Status — Apple system tones, muted for dark
private val SuccessDark = Color(0xFF30D158)
private val SuccessLight = Color(0xFF1A7F37)
private val WarningDark = Color(0xFFFF9F0A)
private val WarningLight = Color(0xFFB45309)
private val ErrorDark = Color(0xFFFF6961)
private val ErrorLight = Color(0xFFDC2626)

private val DarkScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF232326),
    onPrimaryContainer = Color(0xFFF2F2F3),
    secondary = SuccessDark,
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0B0B0D),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF131316),
    onSurfaceVariant = Color(0xFFA1A1A6),
    surfaceTint = Color.Transparent,
    outline = Color(0x14FFFFFF),
    outlineVariant = Color(0x0DFFFFFF),
    error = ErrorDark,
    onError = Color.Black,
    errorContainer = Color(0xFF2A1210),
    onErrorContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF6E6E73)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF111214),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E4E6),
    onPrimaryContainer = Color(0xFF1A1A1C),
    secondary = SuccessLight,
    onSecondary = Color.White,
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF111214),
    surface = Color.White,
    onSurface = Color(0xFF111214),
    surfaceVariant = Color(0xFFEDEDEF),
    onSurfaceVariant = Color(0xFF5B5F68),
    surfaceTint = Color.Transparent,
    outline = Color(0xFFE2E2E4),
    outlineVariant = Color(0xFFEDEDEF),
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    tertiary = Color(0xFF9CA3AF)
)

/** Extra semantic tokens not covered by Material3. */
data class PsExtraColors(
    val success: Color,
    val warning: Color,
    val tertiaryText: Color,
    val subtleBorder: Color,
    val track: Color,
    val glass: Color,
    val isDark: Boolean
)

private val DarkExtra = PsExtraColors(
    success = SuccessDark,
    warning = WarningDark,
    tertiaryText = Color(0xFF6E6E73),
    subtleBorder = Color(0x14FFFFFF),
    track = Color(0xFF1C1C1F),
    glass = Color(0xB30B0B0D),
    isDark = true
)

private val LightExtra = PsExtraColors(
    success = SuccessLight,
    warning = WarningLight,
    tertiaryText = Color(0xFF8E8E93),
    subtleBorder = Color(0xFFE2E2E4),
    track = Color(0xFFE9E9EB),
    glass = Color(0xE6FFFFFF),
    isDark = false
)

val LocalPsExtra = staticCompositionLocalOf { DarkExtra }



@Composable
fun PocketServerTheme(
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PocketServerApp
    val settings by app.container.settingsRepository.settings.collectAsState(
        initial = com.saketkhundia.pocketserver.domain.model.AppSettings()
    )
    val useDark = when (settings.themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme = if (useDark) DarkScheme else LightScheme
    val extra = if (useDark) DarkExtra else LightExtra

    CompositionLocalProvider(LocalPsExtra provides extra) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
