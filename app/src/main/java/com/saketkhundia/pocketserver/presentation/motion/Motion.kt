package com.saketkhundia.pocketserver.presentation.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

/**
 * Central motion tokens — single source of truth for all animation.
 *
 * Philosophy: fast + subtle + purposeful. Every duration targets 150–250ms
 * so motion feels instantaneous, never presentational.
 *
 * - Navigation: fade + small slide (1/8 width), 200ms
 * - State change (server pill, CTA): fade + scale, 180ms
 * - Entrance (home sections): fade + 12dp rise, 220ms with ≤60ms stagger
 * - Dialogs/sheets: fade + 0.96→1.0 scale, 180ms
 * - No infinite motion except the server-running dot (1.6s, subtle).
 */
object PsMotion {
    /** Instant-feeling state fades (pill, text, CTA). */
    const val Fast = 150

    /** Default for navigation, dialogs, sheets, entrance. */
    const val Normal = 200

    /** Upper bound — nothing should exceed this except the running pulse. */
    const val Slow = 250

    /** Tiny stagger between coordinated entrance blocks (NOT per-item). */
    const val StaggerStep = 40

    /** Slide distance divisor: screen width / 8 ≈ subtle, not dramatic. */
    const val SlideDivisor = 8

    /** Entrance rise in dp — barely-there upward movement. */
    const val EntranceRise = 12

    /** Dialog/sheet scale: 0.96 → 1.0, never bouncy. */
    const val DialogScale = 0.96f

    fun <T> tweenFast(): androidx.compose.animation.core.FiniteAnimationSpec<T> =
        tween(Fast, easing = FastOutSlowInEasing)

    fun <T> tweenNormal(): androidx.compose.animation.core.FiniteAnimationSpec<T> =
        tween(Normal, easing = FastOutSlowInEasing)

    /** Spring for the bottom-dock pill — iOS spring 0.38s cubic-bezier(0.32,0.72,0,1). */
    val dockSpring: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        tween(380, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f))
}

// ─── Reduced motion ─────────────────────────────────────────────
// Respects the system animator setting: when the user disables animations
// (Accessibility → Remove animations, or animator duration scale == 0),
// all purposeful motion collapses to instant transitions. Essential state
// changes still apply — just without movement.

/** True when the user asked for reduced motion (animator scale == 0). */
@Composable
fun rememberReducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/** Duration that collapses to 0 when reduced motion is on. */
@Composable
@ReadOnlyComposable
fun motionMs(base: Int): Int {
    val ctx = LocalContext.current
    val reduced = try {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (_: Exception) {
        false
    }
    return if (reduced) 0 else base
}

// ─── Shared enter/exit specs ────────────────────────────────────

/** Subtle coordinated entrance: fade + slight rise. Used for home sections, QR, empty states. */
fun entranceIn(duration: Int = PsMotion.Normal): EnterTransition =
    fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
        slideInVertically(tween(duration, easing = FastOutSlowInEasing)) { it / 14 }

/** Dialog entrance: fade + 0.96 → 1.0 scale. 150–200ms, no bounce. */
fun dialogIn(duration: Int = 180): EnterTransition =
    fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
        scaleIn(tween(duration, easing = FastOutSlowInEasing), initialScale = PsMotion.DialogScale)

/** Snackbar/toast: slide up + fade in; exit mirrors downward. */
fun snackbarIn(duration: Int = PsMotion.Fast): EnterTransition =
    fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
        slideInVertically(tween(duration, easing = FastOutSlowInEasing)) { it / 2 }

fun snackbarOut(duration: Int = PsMotion.Fast): ExitTransition =
    fadeOut(tween(duration, easing = FastOutSlowInEasing)) +
        slideOutVertically(tween(duration, easing = FastOutSlowInEasing)) { it / 2 }

// ─── Directional navigation ─────────────────────────────────────
// Forward (drill-in): content slides slightly left. Back: slides right.
// Tab switches use index order so Home→Files slides left, Files→Home slides right.

fun AnimatedContentTransitionScope<*>.forwardIn(): EnterTransition =
    fadeIn(tween(PsMotion.Normal)) +
        slideInHorizontally(tween(PsMotion.Normal, easing = FastOutSlowInEasing)) { it / PsMotion.SlideDivisor }

fun AnimatedContentTransitionScope<*>.forwardOut(): ExitTransition =
    fadeOut(tween(PsMotion.Normal)) +
        slideOutHorizontally(tween(PsMotion.Normal, easing = FastOutSlowInEasing)) { -it / PsMotion.SlideDivisor }

fun AnimatedContentTransitionScope<*>.backIn(): EnterTransition =
    fadeIn(tween(PsMotion.Normal)) +
        slideInHorizontally(tween(PsMotion.Normal, easing = FastOutSlowInEasing)) { -it / PsMotion.SlideDivisor }

fun AnimatedContentTransitionScope<*>.backOut(): ExitTransition =
    fadeOut(tween(PsMotion.Normal)) +
        slideOutHorizontally(tween(PsMotion.Normal, easing = FastOutSlowInEasing)) { it / PsMotion.SlideDivisor }

/** Tab-bar transition: fadeSlideUp — opacity 0->1, translateY 12px->0, 0.3s ease-out. */
fun AnimatedContentTransitionScope<*>.tabIn(): EnterTransition =
    fadeIn(tween(300, easing = FastOutSlowInEasing)) +
        slideInVertically(tween(300, easing = FastOutSlowInEasing)) { (it * 12f / 200f).toInt().coerceAtLeast(12) }

fun AnimatedContentTransitionScope<*>.tabOut(): ExitTransition =
    fadeOut(tween(150, easing = FastOutSlowInEasing)) +
        slideOutVertically(tween(150, easing = FastOutSlowInEasing)) { -(it * 12f / 200f).toInt().coerceAtMost(-12) }
