package com.saketkhundia.pocketserver.presentation.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MONOCHROME LIQUID-GLASS TOKENS — visuals only.
 * AMOLED black foundation (#000), white-at-opacity surfaces/text only.
 * Live mint #6EE7B7 reserved for the online/live dot.
 *
 * Elevated card: gradient 165deg 0.14 -> 0.07 -> 0.03 white, border 0.14,
 * shadow 0 16/40 0.35 + 0 2/8 0.20, inset top 0.22 / bottom 0.18, radius 24.
 * Recessed card: same gradient at half strength (0.08 -> 0.03), border 0.10.
 * Overlays: 1px top specular streak + top-gloss wash (top 42%).
 * Radii: cards 24, tab bar 28, pill 20, buttons 16, icon tiles 32%.
 */

@Immutable
data class GlassColors(
    val bg0: Color,
    val bg1: Color,
    val bg2: Color,
    val surfaceL1: Color,
    val surfaceL2: Color,
    val surfaceL2b: Color,
    val surfaceL3: Color,
    val surfaceCta: Color,
    val surfaceNav: Color,
    val border: Color,
    val borderSoft: Color,
    val highlight: Color,
    val shade: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val gold: Color,
    val goldMid: Color,
    val goldSoft: Color,
    val goldGlow: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val photo: Color,
    val track: Color,
    val isDark: Boolean
)

val DarkGlassColors = GlassColors(
    bg0 = Color(0xFF000000),
    bg1 = Color(0xFF000000),
    bg2 = Color(0xFF000000),
    surfaceL1 = Color(0x0FFFFFFF),
    surfaceL2 = Color(0x0FFFFFFF),
    surfaceL2b = Color(0x0FFFFFFF),
    surfaceL3 = Color(0x0FFFFFFF),
    surfaceCta = Color(0x0FFFFFFF),
    surfaceNav = Color(0x0FFFFFFF),
    border = Color.White.copy(alpha = 0.14f),
    borderSoft = Color.White.copy(alpha = 0.10f),
    highlight = Color.White.copy(alpha = 0.22f),
    shade = Color.Black.copy(alpha = 0.18f),
    textPrimary = Color.White.copy(alpha = 0.95f),
    textSecondary = Color.White.copy(alpha = 0.60f),
    textTertiary = Color.White.copy(alpha = 0.40f),
    // Monochrome: accent slots resolve to white/grey — no hues.
    gold = Color.White.copy(alpha = 0.95f),
    goldMid = Color.White.copy(alpha = 0.60f),
    goldSoft = Color.White.copy(alpha = 0.95f),
    goldGlow = Color.White.copy(alpha = 0.06f),
    // Status: live mint only; all other states resolve to grey.
    success = Color(0xFF6EE7B7),
    error = Color.White.copy(alpha = 0.60f),
    warning = Color.White.copy(alpha = 0.60f),
    photo = Color.White.copy(alpha = 0.60f),
    track = Color.White.copy(alpha = 0.08f),
    isDark = true
)

// Light resolves to the same black monochrome (visuals stay strict;
// the stored theme preference keeps working).
val LightGlassColors = DarkGlassColors

val LocalGlassColors = staticCompositionLocalOf { DarkGlassColors }

// ─── Dimensions ─────────────────────────────────────────────────────

@Immutable
object GlassDimens {
    val s4: Dp = 4.dp
    val s8: Dp = 8.dp
    val s12: Dp = 12.dp
    val s14: Dp = 14.dp
    val s16: Dp = 16.dp
    val s18: Dp = 18.dp
    val s20: Dp = 20.dp
    val s22: Dp = 22.dp
    val s24: Dp = 24.dp
    val s32: Dp = 32.dp
    val pagePadding: Dp = 20.dp
    val pagePaddingWide: Dp = 22.dp
    val cardPadding: Dp = 18.dp
    val cardSpacing: Dp = 14.dp
    val sectionSpacing: Dp = 22.dp
    val bottomInset: Dp = 124.dp
    val headerSize: Dp = 44.dp
    val ctaHeight: Dp = 50.dp
    val tileMin: Dp = 88.dp
    val rowMin: Dp = 62.dp
    val navHeight: Dp = 68.dp
    const val touchMin: Int = 48
}

// ─── Shapes — spec radii ──────────────────────────────────────
// Cards 24 · info cards 20 · tab bar 24 · pill 20 · buttons 16,
// icon tiles 32%.

@Immutable
object GlassShapes {
    val small: Dp = 16.dp
    val button: Dp = 16.dp
    val card: Dp = 24.dp
    val large: Dp = 24.dp
    val info: Dp = 20.dp
    val nav: Dp = 24.dp
    val pill: Dp = 20.dp
    val tabPill: Dp = 18.dp
}

// ─── Motion ─────────────────────────────────────────────────────────
// Tab pill: transform 0.38s cubic-bezier(0.32,0.72,0,1).
// Tab content: fadeSlideUp 0.3s ease-out. Scrub tracking: 0.1s.

@Immutable
object GlassMotion {
    const val fast: Int = 140
    const val nav: Int = 200
    const val button: Int = 120
    const val press: Int = 110
    const val chip: Int = 160
    const val dialog: Int = 220
    const val sheet: Int = 300
    const val switchMs: Int = 200
    const val progress: Int = 400
    const val tabPillMs: Int = 380
    const val tabContentMs: Int = 300
    const val scrubMs: Int = 100
}
