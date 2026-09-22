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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.presentation.theme.PsIcons

// ─── Glass icon tile (one style for ALL icons) ─────────────────────
// Flat transparent glass: 0.07 white fill, 0.10 border, one soft highlight.
// Glyph #E5E5E5. No bevels, no graphite, no specular ellipses.
// Sizes: 52/24 hero, 52/22 actions, 36/16 rows. Radius 32% of tile.

@Composable
fun LiquidIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tileSize: Dp = 52.dp,
    glyphSize: Dp = 24.dp,
    contentDescription: String? = null
) {
    val tileShape = RoundedCornerShape(tileSize * 0.32f)
    Box(
        modifier = modifier
            .size(tileSize)
            .shadow(6.dp, tileShape, ambientColor = Color.Black.copy(alpha = 0.35f))
            .clip(tileShape)
            .background(Color.White.copy(alpha = 0.07f))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    endY = 80f
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), tileShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription,
            tint = Color(0xFFE5E5E5),
            modifier = Modifier.size(glyphSize)
        )
    }
}

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

// ─── Primary CTA ────────────────────────────────────────────────────
// One strong white control: flat #F5F5F5, 50dp, radius 16, #111111 bold
// label + icon, very subtle shadow. Press scales to 0.98.
// Destructive/off/loading: same shape in grey glass, white label.

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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.98f else 1f,
        tween(GlassMotion.press, easing = FastOutSlowInEasing), label = "btnPress"
    )
    val shape = RoundedCornerShape(GlassShapes.button) // 16px

    if (!danger && enabled && !loading) {
        Box(
            modifier = modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.35f))
                .clip(shape)
                .background(Color(0xFFF5F5F5))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = Color(0xFF111111), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, color = Color(0xFF111111)
                    )
                )
            }
        }
        return
    }
    // Destructive / off / loading: same shape in grey glass, white label.
    LiquidGlassSurface(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        level = GlassLevel.L2,
        radius = GlassShapes.button,
        glow = null,
        borderAlpha = 0.14f,
        onClick = if (enabled && !loading) onClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    strokeWidth = 2.dp
                )
            } else {
                if (icon != null) {
                    Icon(icon, null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                AnimatedContent(
                    targetState = label,
                    transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(140)) },
                    label = "glassCta"
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.95f)
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
    LiquidGlassSurface(
        modifier = modifier, level = GlassLevel.L1,
        radius = GlassShapes.button, onClick = onClick,
        borderAlpha = 0.10f
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.95f))
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
        radius = size * 0.32f,
        borderAlpha = 0.14f,
        onClick = onClick,
        contentAlignment = Alignment.Center
    ) { icon() }
}

// ─── Pills / status ─────────────────────────────────────────────────
// Compact glass status pill (28-30dp): 0.07 fill, 0.10 border, 6-7dp dot,
// 10-11sp letterspaced label. Status colors restrained to the dot only:
// green running, amber starting, red offline/error.

private val DotGreen = Color(0xFF6EE7B7)
private val DotAmber = Color(0xFFF5B84B)
private val DotRed = Color(0xFFFF6B6B)

@Composable
fun LiquidGlassPill(
    text: String,
    modifier: Modifier = Modifier,
    dot: Color? = null,
    accent: Color? = null
) {
    val ac = accent ?: Color.White.copy(alpha = 0.95f)
    LiquidGlassSurface(modifier = modifier, level = GlassLevel.L1, radius = GlassShapes.pill, glow = null) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(7.dp).background(dot, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.6.sp
                ),
                color = ac
            )
        }
    }
}

@Composable
fun LiquidGlassStatusPill(status: ServerStatus, modifier: Modifier = Modifier) {
    val (dot, label) = when (status) {
        ServerStatus.RUNNING -> DotGreen to "SERVER RUNNING"
        ServerStatus.STARTING -> DotAmber to "STARTING…"
        ServerStatus.ERROR -> DotRed to "SERVER ERROR"
        ServerStatus.STOPPED -> DotRed to "SERVER OFFLINE"
    }
    LiquidGlassSurface(modifier = modifier, level = GlassLevel.L1, radius = GlassShapes.pill) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(dot, CircleShape))
            Spacer(Modifier.width(8.dp))
            AnimatedContent(label, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "statusTxt") {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.6.sp
                    ),
                    color = if (status == ServerStatus.RUNNING) DotGreen else Color.White.copy(alpha = 0.60f)
                )
            }
        }
    }
}

// ─── Inset address bar ──────────────────────────────────────────────
// Dark inset rgba(0,0,0,0.28), 0.09 border, radius 16, link glyph + COPY.

@Composable
fun LiquidGlassAddressBar(
    text: String,
    modifier: Modifier = Modifier,
    onCopy: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(PsIcons.Link, null, tint = Color.White.copy(alpha = 0.60f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.5.sp),
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onCopy != null) {
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "COPY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp),
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }
}

// ─── Stat rows: 3-col grid, 1px dividers 8% white ────────────────────

@Composable
fun LiquidGlassStatRow(
    stats: List<Triple<String, String, String>>,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        stats.forEachIndexed { i, (value, label, _) ->
            Column(
                Modifier.weight(1f).padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        color = Color.White.copy(alpha = 0.95f)
                    )
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.4.sp
                    ),
                    color = Color.White.copy(alpha = 0.40f)
                )
            }
            if (i < stats.lastIndex) {
                Box(Modifier.width(1.dp).height(44.dp).background(Color.White.copy(alpha = 0.08f)))
            }
        }
    }
}

// ─── Section header: 10px bold uppercase micro-label ────────────────

@Composable
fun LiquidGlassSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.6.sp
            ),
            color = Color.White.copy(alpha = 0.30f),
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.60f),
                modifier = Modifier.clickable(onClick = onAction).padding(4.dp)
            )
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
    LiquidGlassSurface(modifier = modifier, level = GlassLevel.L2, radius = GlassShapes.card, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = GlassDimens.tileMin).padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LiquidIconTile(icon = icon, tileSize = 52.dp, glyphSize = 22.dp)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.95f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.60f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = if (selected) GlassLevel.L2 else GlassLevel.L1,
        radius = GlassShapes.small,
        borderAlpha = if (selected) 0.16f else 0.10f,
        onClick = onClick
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.95f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.60f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    LiquidIconTile(
        icon = icon,
        modifier = modifier,
        tileSize = size,
        glyphSize = size * 0.46f
    )
}

// ─── Activity item ────────────────────────────────────────────────
// 36px flat glass icon + 12.5 semibold title + 11 muted time.
// [flat]=true renders a transparent row for use inside one continuous
// glass container with hairline dividers (no card-in-card).

@Composable
fun LiquidGlassActivityItem(
    title: String,
    time: String,
    dot: Color,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val row = @Composable {
        Row(
            Modifier.fillMaxWidth().heightIn(min = GlassDimens.rowMin).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidIconTile(icon = PsIcons.Activity, tileSize = 36.dp, glyphSize = 16.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.95f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    time,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.40f)
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.30f)
            )
        }
    }
    if (flat) {
        Box(
            modifier = modifier.fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.CenterStart
        ) { row() }
    } else {
        LiquidGlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.L1, radius = GlassShapes.small, onClick = onClick) {
            row()
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
    LiquidGlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.L1, radius = GlassShapes.button) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Icon(leading, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            BasicTextField(
                value = value, onValueChange = onValue, singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.95f)),
                cursorBrush = SolidColor(Color.White.copy(alpha = 0.9f)),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.35f))
                    inner()
                }
            )
            if (value.isNotEmpty() && onClear != null) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(CircleShape).clickable(onClick = onClear).padding(4.dp)) {
                    Text("✕", color = Color.White.copy(alpha = 0.35f))
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
    LiquidGlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.L1, radius = GlassShapes.small) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Icon(leading, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            BasicTextField(
                value = value, onValueChange = onValue, singleLine = singleLine,
                textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.95f)),
                cursorBrush = SolidColor(Color.White.copy(alpha = 0.9f)),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.35f))
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
    LiquidGlassSurface(
        modifier = modifier, level = if (selected) GlassLevel.L2 else GlassLevel.L1,
        radius = GlassShapes.pill, borderAlpha = if (selected) 0.16f else 0.10f,
        onClick = onClick
    ) {
        Text(
            label, style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.60f),
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
                                        Color.White.copy(alpha = 0.20f),
                                        Color.White.copy(alpha = 0.07f)
                                    )
                                )
                            ) else Modifier
                        )
                        .clickable { onSelect(i) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(o, style = MaterialTheme.typography.labelLarge, color = if (sel) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.60f))
                }
            }
        }
    }
}

// ─── Switch (custom, 200ms, monochrome) ─────────────────────────────

@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackTarget = if (checked) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.10f)
    val track by androidx.compose.animation.animateColorAsState(trackTarget, tween(GlassMotion.switchMs), label = "swTrack")
    val thumbX by animateFloatAsState(if (checked) 1f else 0f, tween(GlassMotion.switchMs, easing = FastOutSlowInEasing), label = "swX")
    Box(
        modifier = modifier.width(52.dp).height(32.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(track)
            .border(1.dp, Color.White.copy(alpha = if (checked) 0.22f else 0.14f), RoundedCornerShape(100.dp))
            .clickable { onChange(!checked) }
            .padding(3.dp)
    ) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.5f)
                .graphicsLayer { translationX = thumbX * 20.dp.toPx() }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
        )
    }
}

// ─── Progress (monochrome) ──────────────────────────────────────────
// Track 0.12 white, fill #E5E5E5. No shimmer, no animation beyond the
// value itself.

@Composable
fun LiquidGlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    fill: Color? = null
) {
    val f = fill ?: Color(0xFFE5E5E5)
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(GlassMotion.progress, easing = FastOutSlowInEasing), label = "glassProg")
    Box(
        modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(100.dp))
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            Modifier.fillMaxWidth(animated).fillMaxHeight()
                .clip(RoundedCornerShape(100.dp))
                .background(f)
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
    LiquidGlassCard(modifier = modifier, level = GlassLevel.L2) {
        Column(Modifier.padding(GlassDimens.s16)) {
            Text("Storage", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.95f))
            Spacer(Modifier.height(GlassDimens.s12))
            Text(
                freeLabel,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = Color.White.copy(alpha = 0.95f)
            )
            Spacer(Modifier.height(GlassDimens.s12))
            LiquidGlassProgressBar(progress = fraction)
            Spacer(Modifier.height(GlassDimens.s8))
            if (usedLabel != null) Text(usedLabel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.60f))
            if (totalLabel != null) Text(totalLabel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.40f))
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
    LiquidGlassCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(GlassDimens.s16)) {
            Text("Shared Folders  ›", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.95f))
            Spacer(Modifier.height(GlassDimens.s12))
            Text(
                "$count",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = Color.White.copy(alpha = 0.95f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (count == 0) "No folders yet" else "$count folder${if (count == 1) "" else "s"} · $names",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.60f), maxLines = 1, overflow = TextOverflow.Ellipsis
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
    LiquidGlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.L2, radius = GlassShapes.large, frosted = true) {
        Column(Modifier.fillMaxWidth().padding(GlassDimens.s20)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.95f))
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.60f))
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
    Column(modifier.fillMaxWidth().padding(vertical = GlassDimens.s32), horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            LiquidIconTile(icon = icon, tileSize = 56.dp, glyphSize = 24.dp)
            Spacer(Modifier.height(GlassDimens.s16))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.95f))
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.60f))
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
    val t = rememberInfiniteTransition(label = "skel")
    val alpha by t.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(rows) {
            LiquidGlassSurface(level = GlassLevel.L1, radius = GlassShapes.small, modifier = Modifier.fillMaxWidth().alpha(alpha)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.08f)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)))
                        Box(Modifier.fillMaxWidth(0.35f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)))
                    }
                }
            }
        }
    }
}

// ─── Slow monochrome spinner (loaders) ──────────────────────────────

@Composable
fun LiquidGlassSpinner(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val angle = com.saketkhundia.pocketserver.presentation.motion.slowSpinAngle()
    CircularProgressIndicator(
        modifier = modifier.size(size).graphicsLayer { rotationZ = angle },
        color = Color.White.copy(alpha = 0.9f),
        strokeWidth = 2.dp
    )
}
