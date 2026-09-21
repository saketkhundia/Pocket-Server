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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LIQUID GLASS MATERIAL — one API, two intentional material systems.
 *
 * DARK (7-layer): translucent base + accent glow wash + top specular +
 * darker lower edge + inner highlight + diagonal sheen (HIGH only) +
 * deep static shadow. Depth through light play on black.
 *
 * LIGHT (flat frosted white — deliberately NOT layered): white base +
 * 1dp black-0.06 hairline + whisper shadow. No glow washes, no highlight
 * or shade gradients, no sheen. Stacking translucent gradients over white
 * is exactly what baked the old "gray gradient card" look — depth here
 * comes from surface-tone steps + hairline + shadow, never from washes.
 *
 * Performance: zero blur modifiers anywhere; all overlays are static
 * single-draws. One background per screen, never per item.
 */

enum class GlassLevel { L1, L2, L3, L4 }

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
    content: @Composable () -> Unit
) {
    val c = LocalGlassColors.current
    val perf = LocalGlassPerformance.current
    val base = when (level) {
        GlassLevel.L1 -> c.surfaceL1
        GlassLevel.L2 -> c.surfaceL2
        GlassLevel.L3 -> c.surfaceL3
        GlassLevel.L4 -> c.surfaceCta
    }
    val shape = RoundedCornerShape(radius)
    // Dark keeps per-level border lift; light always uses the 0.06 hairline
    // (explicit overrides above it would print gray rims on white).
    val borderColor = if (!c.isDark) {
        c.border
    } else {
        borderAlpha?.let { c.border.copy(alpha = it) }
            ?: when (level) {
                GlassLevel.L1 -> c.borderSoft
                GlassLevel.L2 -> c.border
                GlassLevel.L3 -> c.border.copy(alpha = 0.12f)
                GlassLevel.L4 -> c.border.copy(alpha = 0.13f)
            }
    }
    val elevation = when {
        perf == GlassPerformanceMode.LOW -> 2.dp
        level == GlassLevel.L1 -> 0.dp
        level == GlassLevel.L2 -> 8.dp
        level == GlassLevel.L3 -> 14.dp
        else -> 16.dp
    }
    // Shadows: deep black halo on AMOLED; barely-perceived 0.08/0.05 depth
    // on light. A strong shadow under a white card reads as dirt, not depth.
    val ambient = if (c.isDark) 0.50f else 0.08f
    val spot = if (c.isDark) 0.40f else 0.05f

    if (!c.isDark) {
        // LIGHT: flat frosted white. Base + hairline + shadow. Nothing else.
        Box(
            modifier = modifier
                .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = ambient), spotColor = Color.Black.copy(alpha = spot))
                .clip(shape)
                .background(base)
                .border(1.dp, borderColor, shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = contentAlignment
        ) { content() }
        return
    }

    // DARK: full layered treatment.
    val showGlow = perf != GlassPerformanceMode.LOW
    val showDepth = perf == GlassPerformanceMode.HIGH || perf == GlassPerformanceMode.BALANCED
    val showSheen = perf == GlassPerformanceMode.HIGH

    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = ambient), spotColor = Color.Black.copy(alpha = spot))
            .clip(shape)
            .background(base)
            .then(
                if (showGlow && glow != null) Modifier.background(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(0.5f, 0.0f),
                        radius = 520f
                    )
                ) else Modifier
            )
            // Inner top highlight: bright → transparent over first third.
            .then(
                if (showDepth) Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            c.highlight.copy(alpha = if (level == GlassLevel.L1) 0.07f else 0.10f),
                            Color.Transparent
                        ),
                        endY = 260f
                    )
                ) else Modifier
            )
            // Darker lower edge: transparent → depth shade at bottom.
            .then(
                if (showDepth) Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, c.shade.copy(alpha = 0.55f)),
                        startY = 420f,
                        endY = Float.POSITIVE_INFINITY
                    )
                ) else Modifier
            )
            // Faint diagonal reflection — HIGH mode only.
            .then(
                if (showSheen) Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.03f), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(420f, 420f)
                    )
                ) else Modifier
            )
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = contentAlignment
    ) { content() }
}

/**
 * Page background — theme-owned gradient, one shared layer per screen.
 * Dark: true black so AMOLED pixels stay off. Light: #F4F5F7 off-white
 * with a breath of white light up top. No animation, no color tint.
 */
@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
    val c = LocalGlassColors.current
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(c.bg0, c.bg1, c.bg2))
        )
    ) {
        Box(
            Modifier.matchParentSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (c.isDark) 0.035f else 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(0.5f, -0.08f),
                    radius = 980f
                )
            )
        )
    }
}
