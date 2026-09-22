package com.saketkhundia.pocketserver.presentation.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Ambient keyframes — subtle, always monochrome (white/grey only).
 * float ±6px 3s · blob drifts 8/10/12s · shimmer-slide 2.5s ·
 * button shine 3.2s skewed white · pulse-glow · slow spin loaders.
 */

/** float: translateY ±6px, 3s ease-in-out infinite. */
@Composable
fun floatOffset(): Float {
    val t = rememberInfiniteTransition(label = "float")
    val y by t.animateFloat(
        -6f, 6f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )
    return y
}

@Composable
fun Modifier.floatAmbient(): Modifier {
    val y = floatOffset()
    return this.graphicsLayer { translationY = y }
}

/** Blob drift phase: 8/10/12s ease-in-out infinite, returns 0..1 progress. */
@Composable
fun blobPhase(durationMs: Int): Float {
    val t = rememberInfiniteTransition(label = "blob$durationMs")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "blobP"
    )
    return p
}

/** Shimmer-slide sweep overlay: 2.5s white-grey sweep. */
@Composable
fun ShimmerSweep(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                start = Offset(x * 600f - 300f, 0f),
                end = Offset(x * 600f + 100f, 200f)
            ),
            RectangleShape
        )
    )
}

/** Button shine sweep: 3.2s skewed white gradient, white-grey only. */
@Composable
fun buttonShineProgress(): Float {
    val t = rememberInfiniteTransition(label = "shine")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "shineP"
    )
    return p
}

/** Pulse-glow alpha: subtle white breathing for live surfaces. */
@Composable
fun pulseGlowAlpha(min: Float = 0.35f, max: Float = 1f, durationMs: Int = 1600): Float {
    val t = rememberInfiniteTransition(label = "pulseGlow")
    val a by t.animateFloat(
        min, max,
        infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseA"
    )
    return a
}

/** Slow spin angle for loaders (monochrome). */
@Composable
fun slowSpinAngle(durationMs: Int = 1200): Float {
    val t = rememberInfiniteTransition(label = "spin")
    val a by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "spinA"
    )
    return a
}

/** iOS-spring easing for the tab pill: cubic-bezier(0.32, 0.72, 0, 1). */
val IosSpring = androidx.compose.animation.core.CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
