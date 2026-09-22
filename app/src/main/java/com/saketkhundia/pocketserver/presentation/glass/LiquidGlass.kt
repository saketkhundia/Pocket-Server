package com.saketkhundia.pocketserver.presentation.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * POCKET GLASS — one subtle material, used everywhere.
 *
 * Flat dark frosted glass on AMOLED black. No strong gradients, no bevels,
 * no glows, no repeated layers. Depth comes from spacing, typography,
 * alignment and contrast — not from decoration.
 *
 * Fill (white alpha): L1 0.05 · L2 0.07 · L3 0.08 · L4 0.09.
 * Border: L1 0.10 white, else 0.12 white (explicit overrides allowed).
 * One subtle top highlight (0.06 white, fading over the top quarter).
 * One soft shadow. Radius comes from the caller (cards 24, rows 16…).
 *
 * No blur modifiers anywhere: blur is expensive and invisible on a pure
 * black page, so the material stays cheap to render while scrolling,
 * switching tabs, or transferring files.
 */

enum class GlassLevel { L1, L2, L3, L4 }

/** True when [level] maps to the recessed (lightest) material. */
fun GlassLevel.isRecessed(): Boolean = this == GlassLevel.L1

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.L2,
    radius: Dp = GlassShapes.card,
    glow: Color? = null,
    glowAlpha: Float = 0.10f,
    borderAlpha: Float? = null,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    frosted: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(radius)

    val fillAlpha = when (level) {
        GlassLevel.L1 -> 0.05f
        GlassLevel.L2 -> 0.07f
        GlassLevel.L3 -> 0.08f
        GlassLevel.L4 -> 0.09f
    }
    val defaultBorder = when (level) {
        GlassLevel.L1 -> 0.10f
        else -> 0.12f
    }
    val borderColor = borderAlpha?.let { Color.White.copy(alpha = it) }
        ?: Color.White.copy(alpha = defaultBorder)

    // One soft shadow — smaller for recessed surfaces.
    val elevation: Dp = if (level.isRecessed()) 6.dp else 10.dp

    Box(
        modifier = modifier
            .shadow(
                elevation, shape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(Color.White.copy(alpha = fillAlpha))
            .background(
                // Subtle inner highlight: a breath of white up top, nothing more.
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    endY = 120f
                )
            )
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

/**
 * Page background — true AMOLED black (#000000), kept mostly empty.
 * Only an extremely subtle tonal variation up top so glass has light
 * to catch. No visible gradient, no wallpaper.
 */
@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color.Black)) {
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.025f),
                        Color.Transparent
                    ),
                    endY = 700f
                )
            )
        )
    }
}
