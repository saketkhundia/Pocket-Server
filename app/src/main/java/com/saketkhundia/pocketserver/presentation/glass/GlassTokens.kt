package com.saketkhundia.pocketserver.presentation.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * POCKET GLASS — centralized production tokens.
 * Single source of truth: if an accent changes, the whole app updates.
 *
 * Dark and Light share layout/components/typography/spacing/shapes/behavior,
 * but each has INTENTIONAL material tokens — light is designed independently,
 * never an inverted dark palette.
 *
 * Dark: AMOLED black, white-alpha overlays, subtle glass.
 * Light: off-white page, FROSTED WHITE cards (never gray overlays), dark
 * hairline borders, whisper shadows. Depth comes from surface tone steps +
 * soft shadows, never dark-gray washes.
 */

@Immutable
data class GlassColors(
    // Foundation — never pure black
    val bg0: Color, // #05090D
    val bg1: Color, // #071018
    val bg2: Color, // #09131B
    // Surfaces
    val surfaceL1: Color, // ~6%  background glass
    val surfaceL2: Color, // ~9%  normal cards
    val surfaceL2b: Color, // ~11% normal cards alt
    val surfaceL3: Color, // ~14% elevated
    val surfaceCta: Color, // ~17% primary CTA
    val surfaceNav: Color, // ~19% selected nav
    val border: Color, // ~14% subtle edge
    val borderSoft: Color, // ~10% faint edge
    val highlight: Color, // top specular
    val shade: Color, // darker lower edge
    // Text — high readability
    val textPrimary: Color, // #F5F7FA
    val textSecondary: Color, // #AAB5C0
    val textTertiary: Color, // #74808B
    // Accent — champagne gold, used selectively
    val gold: Color, // #E8B95E
    val goldMid: Color, // #F4CA76
    val goldSoft: Color, // #FFD98A
    val goldGlow: Color,
    // Status
    val success: Color, // #35D98B
    val error: Color, // #FF5B61
    val warning: Color, // #F5B84B
    val photo: Color, // #A98BFF secondary only
    // Tracks
    val track: Color,
    val isDark: Boolean
)

val DarkGlassColors = GlassColors(
    bg0 = Color(0xFF000000),
    bg1 = Color(0xFF030303),
    bg2 = Color(0xFF050505),
    surfaceL1 = Color(0x09FFFFFF), // 0.035
    surfaceL2 = Color(0x0EFFFFFF), // 0.055
    surfaceL2b = Color(0x13FFFFFF), // 0.075
    surfaceL3 = Color(0x17FFFFFF), // 0.09 elevated
    surfaceCta = Color(0x1FFFFFFF), // 0.12 strong
    surfaceNav = Color(0x1AFFFFFF), // 0.10 selected nav
    border = Color(0x1AFFFFFF), // 0.10
    borderSoft = Color(0x14FFFFFF),
    highlight = Color(0x29FFFFFF), // 0.16
    shade = Color(0x2B000000),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB8B8B8),
    textTertiary = Color(0xFF777777),
    // Monochrome: accent slots resolve to white/gray (no gold).
    gold = Color(0xFFFFFFFF),
    goldMid = Color(0xFFB8B8B8),
    goldSoft = Color(0xFFFFFFFF),
    goldGlow = Color(0xFFFFFFFF).copy(alpha = 0.06f),
    // Status ONLY — never for buttons/nav/cards/branding.
    success = Color(0xFF35D98B),
    error = Color(0xFFFF5B61),
    warning = Color(0xFFF5B84B),
    photo = Color(0xFFB8B8B8),
    track = Color(0x14FFFFFF),
    isDark = true
)

/**
 * LIGHT — frosted white glass on off-white. Spec values, not remapped dark:
 * base 0.72 / elevated 0.82 / strong 0.92 / border black 0.06 /
 * highlight white 0.75 / shadow black 0.06. Cards read WHITE floating over
 * #F4F5F7 — depth comes from tone steps + hairline borders + soft shadows,
 * never from dark washes.
 */
val LightGlassColors = GlassColors(
    bg0 = Color(0xFFF4F5F7),
    bg1 = Color(0xFFF8F9FA),
    bg2 = Color(0xFFFFFFFF),
    surfaceL1 = Color(0xA6FFFFFF), // 65% white, background glass
    surfaceL2 = Color(0xB8FFFFFF), // 72% white, cards
    surfaceL2b = Color(0xCCFFFFFF), // 80% white
    surfaceL3 = Color(0xD1FFFFFF), // 82% white, elevated / dialogs
    surfaceCta = Color(0xEBFFFFFF), // 92% white (danger path; primary CTA is solid dark)
    surfaceNav = Color(0xE0FFFFFF), // 88% white nav bar
    border = Color(0x0F111214), // 6% dark hairline
    borderSoft = Color(0x0D111214), // 5% faint edge
    highlight = Color(0xBFFFFFFF), // 75% white (dark-branch sheen only)
    shade = Color(0x0D000000), // ~5% bottom depth, never mud
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF555555),
    textTertiary = Color(0xFF7A7A7A),
    gold = Color(0xFF111111),
    goldMid = Color(0xFF555555),
    goldSoft = Color(0xFF111111),
    goldGlow = Color(0xFF111111).copy(alpha = 0.06f),
    success = Color(0xFF12805C),
    error = Color(0xFFDC2626),
    warning = Color(0xFFB45309),
    photo = Color(0xFF5B6472),
    track = Color(0xFFE5E6E8), // solid pale track (storage, skeletons)
    isDark = false
)

val LocalGlassColors = staticCompositionLocalOf { DarkGlassColors }

// ─── Dimensions — production rhythm ─────────────────────────────────
// Top 20 · header-bottom 22 · hero gap 18 · cards 14 · activity 22.

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
    val ctaHeight: Dp = 52.dp
    val tileMin: Dp = 88.dp
    val rowMin: Dp = 62.dp
    val navHeight: Dp = 68.dp
    const val touchMin: Int = 48
}

// ─── Shapes — spec radii ────────────────────────────────────────────
// Hero 24 · info cards 20 · activity 16–18 · buttons 16 · nav 26 · circles.

@Immutable
object GlassShapes {
    val small: Dp = 16.dp
    val button: Dp = 16.dp
    val card: Dp = 20.dp
    val large: Dp = 26.dp
    val nav: Dp = 26.dp
    val pill: Dp = 100.dp
}

// ─── Motion — Fast 120-160 · Normal 180-220 · Large 250-350 ──────────

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
}
