package com.saketkhundia.pocketserver.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.saketkhundia.pocketserver.R

/**
 * Outfit 300-800, applied globally as the sans font with antialiased rendering.
 * (Compose renders text antialiased by default; tight tracking below keeps
 * hero titles crisp on AMOLED.)
 *
 * Type scale (spec):
 * - hero titles 22px/bold/tight tracking
 * - card numerals 19-30px/bold/tabular-nums (tnum)
 * - body 13px/medium at 40% white
 * - section labels 10px/bold/uppercase/tracking 0.14-0.18em at 30% white
 * - tab labels 10px/semibold
 * - mono for address/URL text 12.5px
 */
val Outfit = FontFamily(
    Font(R.font.outfit_300, FontWeight.Light),
    Font(R.font.outfit_400, FontWeight.Normal),
    Font(R.font.outfit_500, FontWeight.Medium),
    Font(R.font.outfit_600, FontWeight.SemiBold),
    Font(R.font.outfit_700, FontWeight.Bold),
    Font(R.font.outfit_800, FontWeight.ExtraBold)
)

/** Legacy alias — everything resolves to Outfit now. */
val Inter: FontFamily = Outfit

val Mono = FontFamily.Monospace

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    displayMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = "tnum"
    ),
    // Hero title: 22px/bold/tight tracking.
    headlineLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum"
    ),
    titleLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 17.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    // Body: 13px/medium.
    bodyMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Tab labels: 10px/semibold.
    labelSmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

/** Monospace style for address/URL text: 12.5px. */
val Typography.mono: TextStyle
    get() = bodyMedium.copy(fontFamily = Mono, fontSize = 12.5.sp)

val Typography.monoSmall: TextStyle
    get() = bodySmall.copy(fontFamily = Mono, fontSize = 12.5.sp)

/** Section label: 10px/bold/uppercase/tracking 0.14-0.18em at 30% white. */
val Typography.eyebrow: TextStyle
    get() = labelSmall.copy(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp // 0.16em at 10sp
    )

/** Card numeral: 19-30px/bold/tabular-nums. */
val Typography.cardNumeral: TextStyle
    get() = displayMedium.copy(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum"
    )
