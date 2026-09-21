package com.saketkhundia.pocketserver.presentation.glass

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * GLASS PERFORMANCE MODE — beauty without jank.
 *
 * HIGH:     full 7-layer glass (glow + highlight + shade + sheen + shadow).
 * BALANCED: glow + highlight + shade + shadow, no diagonal sheen. (default)
 * LOW:      flat translucent surface + hairline border + tiny shadow.
 *           No glow, no gradients — still looks intentional on AMOLED.
 *
 * No blur() modifiers exist anywhere in the item path by design; these modes
 * only toggle cheap gradient overlays, so switching is free.
 */
enum class GlassPerformanceMode { HIGH, BALANCED, LOW }

val LocalGlassPerformance = staticCompositionLocalOf { GlassPerformanceMode.BALANCED }
