package com.saketkhundia.pocketserver.presentation.glass

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * TAB BAR — sliding pill + hold-and-slide + pop. Exact spec.
 *
 * Bar: 4-equal-column grid, padding 8px, radius 28, elevated-glass 0.06.
 * Shared pill behind buttons: width (100%-16px)/4, translateX(i*100%),
 * glass 0.20->0.07, 0.16 border, radius 20 — glides, never blinks.
 * Motion: transform 0.38s cubic-bezier(0.32,0.72,0,1). Active icon 1.1x
 * same easing; active 95% white, inactive 35%.
 * Hold-and-slide: press-and-hold + pointer capture maps finger X to nearest
 * tab, switches live across segments. Vertical scroll unaffected (bar only
 * consumes its own gestures).
 * Dock pop: proximity pop=max(0,1-|fingerT-tabCenter|x4); icon
 * translateY(-7px x pop) scale(active?1.1:1+pop x 0.28), 0.1s tracking,
 * springs back on release; near-finger brightens to 95%. 8ms haptic tick
 * per crossing (guarded).
 */

private val TabPillEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

@Composable
fun LiquidGlassBottomNavigation(
    tabs: List<GlassTab>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    require(tabs.size == 4) { "Bottom nav requires exactly 4 tabs" }
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    var held by remember { mutableStateOf(false) }
    var fingerT by remember { mutableFloatStateOf(-1f) } // -1 = released
    var lastTickIndex by remember { mutableStateOf(selectedIndex) }

    fun tick(index: Int) {
        if (index == lastTickIndex) return
        lastTickIndex = index
        try {
            // ~8ms tick, guarded (some devices throw with no vibrator).
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {}
    }

    val barShape = RoundedCornerShape(GlassShapes.nav) // 24dp
    Box(
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 16.dp).padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // See-through bar: fully transparent fill so scrolling content shows
        // underneath. Only the hairline border defines the floating shape;
        // the tab pills keep their own glass backgrounds.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(barShape)
                .border(1.dp, Color.White.copy(alpha = 0.10f), barShape)
                .pointerInput(tabs, selectedIndex) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        held = true
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        var t = (down.position.x / w).coerceIn(0f, 1f)
                        fingerT = t
                        var index = ((t * 4).toInt()).coerceIn(0, 3)
                        if (index != selectedIndex) {
                            onSelect(tabs[index].route)
                            tick(index)
                        } else {
                            lastTickIndex = index
                        }
                        var pid = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pid }
                            if (change == null) {
                                // New pointer took over (multi-touch): track first pressed.
                                val next = event.changes.firstOrNull { it.pressed }
                                if (next == null) break
                                pid = next.id
                                continue
                            }
                            if (!change.pressed) break
                            t = (change.position.x / w).coerceIn(0f, 1f)
                            fingerT = t
                            val ni = ((t * 4).toInt()).coerceIn(0, 3)
                            if (ni != index) {
                                index = ni
                                onSelect(tabs[ni].route)
                                tick(ni)
                            }
                            change.consume()
                        }
                        held = false
                        fingerT = -1f
                    }
                }
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp)) {
                val pillW = (maxWidth - 0.dp) / 4 // (100% - 16px)/4 incl. 8px bar padding each side
                val pillPx = with(density) { pillW.toPx() }
                // Glide: translateX(activeIndex * 100%), 0.38s iOS spring.
                val pillX by animateFloatAsState(
                    targetValue = pillPx * selectedIndex,
                    animationSpec = tween(GlassMotion.tabPillMs, easing = TabPillEasing),
                    label = "navPillX"
                )
                val pillShape = RoundedCornerShape(GlassShapes.tabPill) // 18dp
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(width = pillW, height = maxHeight)
                        .graphicsLayer { translationX = pillX }
                        .clip(pillShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                                endY = 80f
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.14f), pillShape)
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { i, tab ->
                        key(tab.route) {
                            GlassNavCell(
                                label = tab.label,
                                icon = tab.icon,
                                selected = i == selectedIndex,
                                fingerT = if (held && fingerT >= 0f) fingerT else null,
                                tabIndex = i,
                                onClick = {
                                    if (i != selectedIndex) {
                                        onSelect(tab.route)
                                        tick(i)
                                    }
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

data class GlassTab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun GlassNavCell(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    fingerT: Float?,
    tabIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pxPerDp = with(density) { 1.dp.toPx() }
    val tabCenter = (tabIndex + 0.5f) / 4f
    val pop = if (fingerT != null) {
        (1f - kotlin.math.abs(fingerT - tabCenter) * 4f).coerceAtLeast(0f)
    } else 0f
    val targetScale = if (selected) 1.1f else 1f + pop * 0.28f
    val targetY = -7f * pop
    // Fast 0.1s tracking easing; springs back on release (pop -> 0).
    val scale by animateFloatAsState(targetScale, tween(GlassMotion.scrubMs, easing = TabPillEasing), label = "navPopS$tabIndex")
    val ty by animateFloatAsState(targetY, tween(GlassMotion.scrubMs, easing = TabPillEasing), label = "navPopY$tabIndex")
    val activeScale by animateFloatAsState(
        if (selected) 1.1f else 1f,
        tween(GlassMotion.tabPillMs, easing = TabPillEasing), label = "navActive$tabIndex"
    )
    val combined = if (fingerT != null) scale else activeScale
    val tint = when {
        selected -> Color.White.copy(alpha = 0.95f)
        pop > 0.35f -> Color.White.copy(alpha = 0.95f)
        else -> Color.White.copy(alpha = 0.35f)
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(GlassShapes.tabPill)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon, label,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = combined
                        scaleY = combined
                        translationY = if (fingerT != null) ty * pxPerDp else 0f
                    }
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall, // 10px semibold
                color = tint
            )
        }
    }
}

/**
 * Content swipe: horizontal swipe beyond 64px advances/retreats one tab
 * (clamped); vertical scroll unaffected (only horizontal drag consumed).
 */
@Composable
fun Modifier.tabSwipeToNavigate(
    currentIndex: Int,
    tabCount: Int,
    onSelect: (Int) -> Unit
): Modifier {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 64.dp.toPx() }
    return this.pointerInput(currentIndex, tabCount) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalX = 0f
            var totalY = 0f
            var fired = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val delta = change.position - change.previousPosition
                totalX += delta.x
                totalY += delta.y
                if (!fired && kotlin.math.abs(totalX) > thresholdPx && kotlin.math.abs(totalX) > kotlin.math.abs(totalY) * 1.4f) {
                    val next = if (totalX < 0) (currentIndex + 1).coerceAtMost(tabCount - 1)
                    else (currentIndex - 1).coerceAtLeast(0)
                    if (next != currentIndex) onSelect(next)
                    fired = true
                    change.consume()
                    break
                }
                if (kotlin.math.abs(totalY) > kotlin.math.abs(totalX) * 1.4f) {
                    // Vertical scroll — do not consume, let LazyColumn have it.
                    break
                }
            }
        }
    }
}

/**
 * Tab content crossfade: fadeSlideUp (opacity 0->1, translateY 12px->0,
 * 0.3s ease-out) keyed per tab.
 */
@Composable
fun TabContentCrossfade(
    tabKey: Any,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    key(tabKey) {
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = androidx.compose.animation.fadeIn(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                androidx.compose.animation.slideInVertically(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (it * 12f / 200f).toInt().coerceAtLeast(12) },
            label = "tabCrossfade$tabKey",
            modifier = modifier
        ) { content() }
    }
}
