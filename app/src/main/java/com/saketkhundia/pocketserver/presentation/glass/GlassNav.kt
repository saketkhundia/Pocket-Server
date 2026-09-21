package com.saketkhundia.pocketserver.presentation.glass

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * AMOLED BOTTOM NAVIGATION — 4 EXACTLY-EQUAL CELLS.
 *
 * Layout (the bug fix):
 * - Bar: FULL WIDTH × FIXED 68dp height, 16dp horizontal / 12dp bottom margin.
 * - Inside: ONE Row(fillMaxSize) with EXACTLY 4 children, each
 *   Box(Modifier.weight(1f).fillMaxHeight()) → exactly 25% each.
 *   No wrapContent, no IntrinsicSize, no content-based widths, identical
 *   padding in every cell.
 * - The selected background is sized from the SAME cell grid
 *   (cellW = maxWidth / 4 → pill = cellW − 6dp × bar − 8dp) and drawn as a
 *   single shared pill BEHIND the Row. It never uses fillMaxSize() at the
 *   bar level, so it can never become a full-bar/full-height rectangle.
 *
 * Animation: POSITION ONLY (GPU translationX, 175ms). Size is fixed —
 * never animated, never derived from text.
 *
 * SINGLE SOURCE OF TRUTH: [currentRoute] from NavController only.
 * Tap → onSelect immediately (no IO first); content loads async.
 *
 * Paint (dark): 0.035 white bar, 0.09 selected, 0.10 border.
 * Paint (light): white-0.88 bar, #E8E9EB pill, 0.07 border, 0 4/20 0.08 shadow.
 * Selected #FFFFFF/#111111, unselected #666666/#888888. No gold, no blue.
 * No blur modifiers, no animated shadows — cheapest component to render.
 */
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
    val dark = LocalGlassColors.current.isDark
    // Bar material is explicit per theme (not the generic surface): dark is a
    // near-black film, light is flat white-0.88 + 0.07 hairline + one soft
    // static shadow. No layered effects — this stays the cheapest component.
    val barShape = RoundedCornerShape(GlassShapes.nav)
    Box(
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 16.dp).padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    12.dp, barShape,
                    ambientColor = Color.Black.copy(alpha = if (dark) 0.50f else 0.08f),
                    spotColor = Color.Black.copy(alpha = if (dark) 0.40f else 0.05f)
                )
                .clip(barShape)
                .background(if (dark) Color.White.copy(alpha = 0.035f) else LocalGlassColors.current.surfaceNav)
                .border(
                    1.dp,
                    if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFF111418).copy(alpha = 0.07f),
                    barShape
                )
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Cell grid measured on the EXACT area the Row below fills:
                // no padding between this scope and the Row, so cell i owns
                // [i*cellW,(i+1)*cellW] in both.
                val cellW: Dp = maxWidth / 4
                val cellPx = with(density) { cellW.toPx() }
                val insetPx = with(density) { 3.dp.toPx() }
                // Position-only animation: same size every frame.
                val pillX by animateFloatAsState(
                    targetValue = cellPx * selectedIndex,
                    animationSpec = tween(175, easing = FastOutSlowInEasing),
                    label = "navPillX"
                )
                // Theme-adaptive pill: white glass on AMOLED black;
                // solid #E8E9EB capsule + 0.07 border in light. Same size always.
                val pillBg = if (dark) {
                    Color.White.copy(alpha = 0.09f)
                } else {
                    Color(0xFFE8E9EB)
                }
                val pillBorder = if (dark) {
                    Color.White.copy(alpha = 0.10f)
                } else {
                    Color(0xFF111418).copy(alpha = 0.07f)
                }
                // Shared pill BEHIND the Row: fixed (cellW−6dp × bar−8dp).
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 4.dp)
                        .width(cellW - 6.dp)
                        .height(maxHeight - 8.dp)
                        .graphicsLayer { translationX = pillX + insetPx }
                        .clip(RoundedCornerShape(18.dp))
                        .background(pillBg)
                        .border(1.dp, pillBorder, RoundedCornerShape(18.dp))
                )
                // Content Row ON TOP: 4 identical 25% cells.
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { i, tab ->
                        GlassNavCell(
                            label = tab.label,
                            icon = tab.icon,
                            selected = i == selectedIndex,
                            onClick = { if (i != selectedIndex) onSelect(tab.route) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // One 25% cell: full-cell touch area, centered column, identical metrics.
    // Selected #111 / unselected #8A8A8A in light; white / tertiary in dark.
    // Colors resolve from tokens + theme — never hardcoded per screen.
    val c = LocalGlassColors.current
    val unselected = if (c.isDark) c.textTertiary else Color(0xFF8A8A8A)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        tween(110, easing = FastOutSlowInEasing), label = "navPress"
    )
    val content: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, label,
                tint = if (selected) c.textPrimary else unselected,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) c.textPrimary else unselected
            )
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .graphicsLayer { scaleX = press; scaleY = press },
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}
