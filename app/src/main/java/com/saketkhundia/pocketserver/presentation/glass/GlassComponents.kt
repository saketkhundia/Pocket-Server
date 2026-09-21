package com.saketkhundia.pocketserver.presentation.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saketkhundia.pocketserver.domain.model.ServerStatus

// ─── Card ───────────────────────────────────────────────────────────

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.L2,
    radius: Dp = GlassShapes.card,
    glow: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) = LiquidGlassSurface(
    modifier = modifier, level = level, radius = radius,
    glow = glow, onClick = onClick
) { content() }

// ─── Buttons ────────────────────────────────────────────────────────
// Primary: L4 glass + gold glow, white/gold text, 150-180ms press scale.
// No bounce.

@Composable
fun LiquidGlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    danger: Boolean = false
) {
    val c = LocalGlassColors.current
    val glow = if (danger) c.error else c.gold
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Production press: 0.98 scale, 110ms — subtle, no bounce.
    val scale by animateFloatAsState(
        if (pressed) 0.98f else 1f,
        tween(GlassMotion.press, easing = FastOutSlowInEasing), label = "btnPress"
    )
    // Hierarchy per theme: dark gets L4 glass + white text; light gets a
    // solid #111214 button + white text (glass-on-white has no presence
    // for a primary CTA; pressed deepens to #1A1A1A). Danger stays
    // red-on-glass in both.
    if (!c.isDark && !danger && enabled && !loading) {
        val shape = RoundedCornerShape(GlassShapes.button)
        val bgTarget = if (pressed) Color(0xFF1A1A1A) else Color(0xFF111214)
        val bg by androidx.compose.animation.animateColorAsState(
            bgTarget, tween(GlassMotion.press), label = "ctaPressBg"
        )
        Box(
            modifier = modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.22f))
                .clip(shape)
                .background(bg)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                        endY = 120f
                    )
                )
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(GlassDimens.ctaHeight),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
        return
    }
    LiquidGlassSurface(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        level = GlassLevel.L4,
        radius = GlassShapes.button,
        glow = glow,
        glowAlpha = if (danger) 0.12f else 0.12f,
        borderAlpha = 0.16f,
        onClick = if (enabled && !loading) onClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(GlassDimens.ctaHeight),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                GlassLoadingDots(color = if (danger) c.error else c.goldSoft)
            } else {
                if (icon != null) {
                    Icon(icon, null, tint = if (danger) c.error else c.goldSoft, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                }
                AnimatedContent(
                    targetState = label,
                    transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(140)) },
                    label = "glassCta"
                ) {
                    Text(
                        it,
                        // 15-16sp semibold primary CTA.
                        style = MaterialTheme.typography.titleMedium,
                        color = if (danger) c.error else c.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidGlassSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val c = LocalGlassColors.current
    LiquidGlassSurface(
        modifier = modifier, level = GlassLevel.L2,
        radius = GlassShapes.button, onClick = onClick
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = c.textPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
        }
    }
}

@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    icon: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        tween(GlassMotion.press, easing = FastOutSlowInEasing), label = "iconPress"
    )
    LiquidGlassSurface(
        modifier = modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale },
        level = GlassLevel.L2,
        radius = 100.dp,
        borderAlpha = 0.14f,
        onClick = onClick,
        contentAlignment = Alignment.Center
    ) { icon() }
}

@Composable
private fun GlassLoadingDots(color: Color) {
    val t = rememberInfiniteTransition(label = "dots")
    val p by t.animateFloat(0f, 2f, infiniteRepeatable(tween(900)), label = "p")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { i ->
            val a = 0.35f + 0.65f * ((p + i) % 3f / 2f)
            Box(Modifier.size(7.dp).alpha(a).background(color, CircleShape))
        }
    }
}

// ─── Pills / status ─────────────────────────────────────────────────

@Composable
fun LiquidGlassPill(
    text: String,
    modifier: Modifier = Modifier,
    dot: Color? = null,
    accent: Color? = null
) {
    val c = LocalGlassColors.current
    val ac = accent ?: c.gold
    LiquidGlassSurface(modifier = modifier, level = GlassLevel.L3, radius = GlassShapes.pill, glow = dot ?: ac, glowAlpha = 0.12f) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(8.dp).background(dot, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = ac)
        }
    }
}

@Composable
fun LiquidGlassStatusPill(status: ServerStatus, modifier: Modifier = Modifier) {
    val c = LocalGlassColors.current
    // Light OFFLINE: neutral #F0F1F2 pill, #6B6B6B text, subtle solid red dot.
    // Never a dark-gray pill — that reads as disabled.
    if (!c.isDark && status == ServerStatus.STOPPED) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(GlassShapes.pill))
                .background(Color(0xFFF0F1F2))
                .border(1.dp, Color(0xFF111418).copy(alpha = 0.06f), RoundedCornerShape(GlassShapes.pill))
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(c.error, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    "SERVER OFFLINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B6B6B)
                )
            }
        }
        return
    }
    val (dot, label, accent) = when (status) {
        ServerStatus.RUNNING -> Triple(c.success, "SERVER RUNNING", c.success)
        ServerStatus.STARTING -> Triple(c.warning, "STARTING…", c.warning)
        ServerStatus.ERROR -> Triple(c.error, "SERVER ERROR", c.error)
        ServerStatus.STOPPED -> Triple(c.error, "SERVER OFFLINE", c.textSecondary)
    }
    val animated by androidx.compose.animation.animateColorAsState(
        dot, tween(150, easing = FastOutSlowInEasing), label = "statusDot"
    )
    LiquidGlassSurface(
        modifier = modifier, level = GlassLevel.L3, radius = GlassShapes.pill,
        glow = animated, glowAlpha = if (status == ServerStatus.STOPPED) 0.05f else 0.14f
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (status == ServerStatus.RUNNING || status == ServerStatus.STARTING) {
                val t = rememberInfiniteTransition(label = "glow")
                val pulse by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(22.dp).alpha(0.35f * pulse).background(animated.copy(alpha = 0.25f), CircleShape))
                    Box(Modifier.size(8.dp).background(animated, CircleShape))
                }
            } else if (status == ServerStatus.STOPPED) {
                Box(Modifier.size(8.dp).border(1.5.dp, c.textTertiary, CircleShape))
            } else {
                Box(Modifier.size(8.dp).background(animated, CircleShape))
            }
            Spacer(Modifier.width(8.dp))
            AnimatedContent(label, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "statusTxt") {
                Text(it, style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
    }
}

// ─── Feature tile ───────────────────────────────────────────────────

@Composable
fun LiquidGlassFeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null
) {
    val c = LocalGlassColors.current
    val tint = iconTint ?: c.goldSoft
    LiquidGlassSurface(modifier = modifier, level = GlassLevel.L2, radius = GlassShapes.card, glow = tint, glowAlpha = 0.07f, borderAlpha = 0.13f, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = GlassDimens.tileMin).padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(54.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f))
                    .border(1.dp, c.borderSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─── List item ──────────────────────────────────────────────────────

@Composable
fun LiquidGlassListItem(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val c = LocalGlassColors.current
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = if (selected) GlassLevel.L4 else GlassLevel.L2,
        radius = GlassShapes.small,
        glow = if (selected) c.gold else null,
        glowAlpha = 0.10f,
        borderAlpha = if (selected) 0.17f else 0.13f,
        onClick = onClick
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
fun LiquidGlassIconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 44.dp
) {
    val c = LocalGlassColors.current
    // Default (no explicit tint): white wash + white icon on dark;
    // flat light-gray + charcoal on light. Explicit tints keep their wash.
    val t = tint ?: if (c.isDark) c.goldSoft else Color(0xFF222222)
    if (c.isDark || tint != null) {
        Box(
            modifier = modifier.size(size).clip(CircleShape)
                .background(t.copy(alpha = 0.13f))
                .border(1.dp, c.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = t, modifier = Modifier.size(size * 0.48f))
        }
        return
    }
    Box(
        modifier = modifier.size(size).clip(CircleShape)
            .background(Color(0xFFF0F1F3))
            .border(1.dp, c.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = t, modifier = Modifier.size(size * 0.48f))
    }
}

// ─── Activity item ──────────────────────────────────────────────────

@Composable
fun LiquidGlassActivityItem(
    title: String,
    time: String,
    dot: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val c = LocalGlassColors.current
    LiquidGlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.L2, radius = GlassShapes.small, glow = dot, glowAlpha = 0.06f, borderAlpha = 0.12f, onClick = onClick) {
        Row(Modifier.fillMaxWidth().heightIn(min = GlassDimens.rowMin).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(dot.copy(alpha = 0.13f)).border(1.dp, c.borderSoft, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(9.dp).background(dot, CircleShape))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(time, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = c.textTertiary)
        }
    }
}

// ─── Search / text field ────────────────────────────────────────────

@Composable
fun LiquidGlassSearchBar(
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…",
    leading: ImageVector? = null,
    onClear: (() -> Unit)? = null
) {
    val c = LocalGlassColors.current
    LiquidGlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.L2, radius = GlassShapes.button) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Icon(leading, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            BasicTextField(
                value = value, onValueChange = onValue, singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.gold),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = c.textTertiary)
                    inner()
                }
            )
            if (value.isNotEmpty() && onClear != null) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(CircleShape).clickable(onClick = onClear).padding(4.dp)) {
                    Text("✕", color = c.textTertiary)
                }
            }
        }
    }
}

@Composable
fun LiquidGlassTextField(
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter text…",
    leading: ImageVector? = null,
    singleLine: Boolean = true
) {
    val c = LocalGlassColors.current
    LiquidGlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.L2, radius = GlassShapes.small) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Icon(leading, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            BasicTextField(
                value = value, onValueChange = onValue, singleLine = singleLine,
                textStyle = LocalTextStyle.current.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.gold),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = c.textTertiary)
                    inner()
                }
            )
        }
    }
}

// ─── Chips / segmented ──────────────────────────────────────────────

@Composable
fun LiquidGlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalGlassColors.current
    // Selected chip must look interactive: dark L4 glow on AMOLED,
    // solid #111111 + white text in light (a white-on-white chip vanishes).
    if (!c.isDark && selected) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(GlassShapes.pill))
                .background(Color(0xFF111111))
                .clickable(onClick = onClick)
        ) {
            Text(
                label, style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
        return
    }
    LiquidGlassSurface(
        modifier = modifier, level = if (selected) GlassLevel.L4 else GlassLevel.L2,
        radius = GlassShapes.pill, glow = if (selected) c.gold else null, glowAlpha = 0.16f,
        onClick = onClick
    ) {
        Text(
            label, style = MaterialTheme.typography.labelLarge,
            color = if (selected) c.goldSoft else c.textSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun LiquidGlassSegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.L1, radius = GlassShapes.button) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            options.forEachIndexed { i, o ->
                val sel = i == selected
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(GlassShapes.button))
                        .then(
                            if (sel) Modifier.background(
                                Brush.verticalGradient(
                                    listOf(
                                        LocalGlassColors.current.gold.copy(alpha = 0.28f),
                                        LocalGlassColors.current.gold.copy(alpha = 0.14f)
                                    )
                                )
                            ) else Modifier
                        )
                        .clickable { onSelect(i) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(o, style = MaterialTheme.typography.labelLarge, color = if (sel) LocalGlassColors.current.goldSoft else LocalGlassColors.current.textSecondary)
                }
            }
        }
    }
}

// ─── Switch (custom, 200ms) ─────────────────────────────────────────

@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalGlassColors.current
    // OFF track: elevated glass on dark; solid pale gray in light so the
    // white thumb reads (white-on-near-white is invisible).
    val offTrack = if (c.isDark) c.surfaceL3 else Color(0xFFE5E6E8)
    val trackTarget = if (checked) c.success.copy(alpha = 0.35f) else offTrack
    val track by androidx.compose.animation.animateColorAsState(trackTarget, tween(GlassMotion.switchMs), label = "swTrack")
    val thumbX by animateFloatAsState(if (checked) 1f else 0f, tween(GlassMotion.switchMs, easing = FastOutSlowInEasing), label = "swX")
    Box(
        modifier = modifier.width(52.dp).height(32.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(track)
            .border(1.dp, if (checked) c.success.copy(alpha = 0.5f) else c.border, RoundedCornerShape(100.dp))
            .clickable { onChange(!checked) }
            .padding(3.dp)
    ) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.5f)
                .graphicsLayer { translationX = thumbX * 20.dp.toPx() }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .border(1.dp, c.borderSoft, CircleShape)
        )
    }
}

// ─── Progress ───────────────────────────────────────────────────────

@Composable
fun LiquidGlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    fill: Color? = null
) {
    val c = LocalGlassColors.current
    val f = fill ?: c.gold
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(GlassMotion.progress, easing = FastOutSlowInEasing), label = "glassProg")
    Box(
        modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(100.dp))
            .background(c.track).border(1.dp, c.border, RoundedCornerShape(100.dp))
    ) {
        Box(
            Modifier.fillMaxWidth(animated).fillMaxHeight()
                .clip(RoundedCornerShape(100.dp))
                .background(Brush.horizontalGradient(listOf(f, f.copy(alpha = 0.75f))))
        )
    }
}

// ─── Storage / folder cards ─────────────────────────────────────────

@Composable
fun LiquidGlassStorageCard(
    freeLabel: String,
    usedLabel: String?,
    totalLabel: String?,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val c = LocalGlassColors.current
    LiquidGlassCard(modifier = modifier, glow = c.gold, level = GlassLevel.L2) {
        Column(Modifier.padding(GlassDimens.s16)) {
            Text("Storage", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Spacer(Modifier.height(GlassDimens.s12))
            Text(freeLabel, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Spacer(Modifier.height(GlassDimens.s12))
            LiquidGlassProgressBar(progress = fraction, fill = c.gold)
            Spacer(Modifier.height(GlassDimens.s8))
            if (usedLabel != null) Text(usedLabel, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            if (totalLabel != null) Text(totalLabel, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
        }
    }
}

@Composable
fun LiquidGlassFolderCard(
    count: Int,
    names: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalGlassColors.current
    LiquidGlassCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(GlassDimens.s16)) {
            Text("Shared Folders  ›", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Spacer(Modifier.height(GlassDimens.s12))
            Text("$count", style = MaterialTheme.typography.displayMedium, color = c.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                if (count == 0) "No folders yet" else "$count folder${if (count == 1) "" else "s"} · $names",
                style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Dialog / sheet ─────────────────────────────────────────────────

@Composable
fun LiquidGlassDialog(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = "Cancel",
    onDismiss: (() -> Unit)? = null
) {
    val c = LocalGlassColors.current
    // Dialogs are large surfaces → frosted in light, layered in dark.
    LiquidGlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.L3, radius = GlassShapes.large, glow = c.gold, glowAlpha = 0.10f, frosted = true) {
        Column(Modifier.fillMaxWidth().padding(GlassDimens.s20)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            Spacer(Modifier.height(GlassDimens.s20))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (secondaryLabel != null) {
                    LiquidGlassSecondaryButton(secondaryLabel, { onDismiss?.invoke() }, Modifier.weight(1f))
                }
                LiquidGlassButton(primaryLabel, onPrimary, Modifier.weight(1f))
            }
        }
    }
}

// ─── States ─────────────────────────────────────────────────────────

@Composable
fun LiquidGlassEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = LocalGlassColors.current
    Column(modifier.fillMaxWidth().padding(vertical = GlassDimens.s32), horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            LiquidGlassIconCircle(icon, tint = c.goldSoft, size = 56.dp)
            Spacer(Modifier.height(GlassDimens.s16))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(GlassDimens.s16))
            LiquidGlassButton(actionLabel, onAction, Modifier.fillMaxWidth(0.7f))
        }
    }
}

@Composable
fun LiquidGlassErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    LiquidGlassEmptyState(title = "Something went wrong", subtitle = message, modifier = modifier, actionLabel = if (onRetry != null) "Try again" else null, onAction = onRetry)
}

@Composable
fun LiquidGlassLoadingRows(modifier: Modifier = Modifier, rows: Int = 4) {
    val c = LocalGlassColors.current
    val t = rememberInfiniteTransition(label = "skel")
    val alpha by t.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(rows) {
            LiquidGlassSurface(level = GlassLevel.L1, radius = GlassShapes.small, modifier = Modifier.fillMaxWidth().alpha(alpha)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(c.track))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(c.track))
                        Box(Modifier.fillMaxWidth(0.35f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(c.track))
                    }
                }
            }
        }
    }
}
