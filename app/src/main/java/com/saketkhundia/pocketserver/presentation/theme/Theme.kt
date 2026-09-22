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
 * MONOCHROME ON AMOLED BLACK — visuals only, functionality untouched.
 * Page/body/shell: pure #000. One faint light source: single ultra-subtle
 * white radial sheen (7% white) top-center (see GlassBackground).
 * All surfaces/text: white at calibrated opacities (85-95 / 60 / 40 /
 * 30-35 / dividers 6-8). Single semantic exception: online/live dot mint
 * #6EE7B7 with glow. Everything else grey. No colored blobs, no hue
 * gradients anywhere.
 */

// Live/online ONLY — mint with glow 0 0 8px rgba(52,211,153,0.9).
private val LiveMint = Color(0xFF6EE7B7)
// Monochrome text ladder: primary 85-95%, secondary 60%, tertiary 40%,
// muted 30-35%, dividers 6-8% white.
private val White95 = Color(0xF2FFFFFF) // 95%
private val White85 = Color(0xD9FFFFFF) // 85%
private val White60 = Color(0x99FFFFFF) // 60%
private val White40 = Color(0x66FFFFFF) // 40%
private val White35 = Color(0x59FFFFFF) // 35%
private val White30 = Color(0x4DFFFFFF) // 30%
private val Divider08 = Color(0x14FFFFFF) // 8%
private val Divider06 = Color(0x0FFFFFFF) // 6%
private val MonoPrimary = White95
private val MonoSecondary = White60

private val DarkScheme = darkColorScheme(
    primary = MonoPrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF141414),
    onPrimaryContainer = MonoPrimary,
    secondary = White60,
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = MonoPrimary,
    surface = Color(0xFF000000),
    onSurface = MonoPrimary,
    surfaceVariant = Color(0xFF000000),
    onSurfaceVariant = MonoSecondary,
    surfaceTint = Color.Transparent,
    outline = Divider08,
    outlineVariant = Divider06,
    error = White60,
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF141414),
    onErrorContainer = MonoPrimary,
    tertiary = White40
)

// Light scheme resolves to the same AMOLED-black monochrome so the visual
// layer stays strictly monochrome even if the stored theme preference is
// light — the setting itself keeps working (functionality untouched).
private val LightScheme = darkColorScheme(
    primary = MonoPrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF141414),
    onPrimaryContainer = MonoPrimary,
    secondary = White60,
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = MonoPrimary,
    surface = Color(0xFF000000),
    onSurface = MonoPrimary,
    surfaceVariant = Color(0xFF000000),
    onSurfaceVariant = MonoSecondary,
    surfaceTint = Color.Transparent,
    outline = Divider08,
    outlineVariant = Divider06,
    error = White60,
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF141414),
    onErrorContainer = MonoPrimary,
    tertiary = White40
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
    success = LiveMint,
    warning = White60,
    tertiaryText = White40,
    subtleBorder = Divider08,
    track = Divider08,
    glass = Color(0x0FFFFFFF),
    isDark = true
)

private val LightExtra = PsExtraColors(
    success = LiveMint,
    warning = White60,
    tertiaryText = White40,
    subtleBorder = Divider08,
    track = Divider08,
    glass = Color(0x0FFFFFFF),
    isDark = true
)

val LocalPsExtra = staticCompositionLocalOf { DarkExtra }

/** Mint live-dot tokens (single semantic exception). */
val LiveDot = LiveMint
val LiveDotGlow = Color(0xE634D399) // rgba(52,211,153,0.9) glow



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
