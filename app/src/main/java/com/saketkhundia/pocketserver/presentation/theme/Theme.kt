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
import com.saketkhundia.pocketserver.presentation.glass.DarkGlassColors
import com.saketkhundia.pocketserver.presentation.glass.LightGlassColors
import com.saketkhundia.pocketserver.presentation.glass.LocalGlassColors

/**
 * Pocket Glass — PURE AMOLED: #000000 true black, white typography,
 * subtle grayscale glass. Status colors ONLY for server state dots.
 * No gold, no blue, no purple branding.
 */

// Status — state dots only
private val SuccessDark = Color(0xFF35D98B)
private val SuccessLight = Color(0xFF12805C)
private val WarningDark = Color(0xFFF5B84B)
private val WarningLight = Color(0xFFB45309)
private val ErrorDark = Color(0xFFFF5B61)
private val ErrorLight = Color(0xFFDC2626)
private val MonoPrimary = Color(0xFFFFFFFF)
private val MonoSecondary = Color(0xFFB8B8B8)

private val DarkScheme = darkColorScheme(
    primary = MonoPrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = MonoPrimary,
    secondary = SuccessDark,
    onSecondary = Color(0xFF04120B),
    background = Color(0xFF000000),
    onBackground = MonoPrimary,
    surface = Color(0xFF030303),
    onSurface = MonoPrimary,
    surfaceVariant = Color(0xFF050505),
    onSurfaceVariant = MonoSecondary,
    surfaceTint = Color.Transparent,
    outline = Color(0x1AFFFFFF),
    outlineVariant = Color(0x14FFFFFF),
    error = ErrorDark,
    onError = Color(0xFF1A0607),
    errorContainer = Color(0xFF1A0A0B),
    onErrorContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF777777)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF111418),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E5E9),
    onPrimaryContainer = Color(0xFF111418),
    secondary = SuccessLight,
    onSecondary = Color.White,
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF2F3F4),
    onSurfaceVariant = Color(0xFF555555),
    surfaceTint = Color.Transparent,
    outline = Color(0x12111214),
    outlineVariant = Color(0x0D111214),
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    tertiary = Color(0xFF7A7A7A)
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
    tertiaryText = Color(0xFF777777),
    subtleBorder = Color(0x1AFFFFFF),
    track = Color(0x14FFFFFF),
    glass = Color(0x09000000),
    isDark = true
)

private val LightExtra = PsExtraColors(
    success = SuccessLight,
    warning = WarningLight,
    tertiaryText = Color(0xFF7A7A7A),
    subtleBorder = Color(0x12111214),
    track = Color(0xFFE5E6E8),
    glass = Color(0xCCFFFFFF),
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
    val glass = if (useDark) DarkGlassColors else LightGlassColors

    CompositionLocalProvider(
        LocalPsExtra provides extra,
        LocalGlassColors provides glass
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
