package com.saketkhundia.pocketserver.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.presentation.theme.FileKind
import com.saketkhundia.pocketserver.presentation.theme.LocalPsExtra
import com.saketkhundia.pocketserver.presentation.theme.PsRadius
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import com.saketkhundia.pocketserver.presentation.theme.eyebrow
import com.saketkhundia.pocketserver.presentation.theme.mono
import com.saketkhundia.pocketserver.presentation.theme.monoSmall

// ─── Glass surfaces ───────────────────────────────────────────────

@Composable
fun glassColors(): Pair<Color, Color> {
    val extra = LocalPsExtra.current
    return extra.glass to extra.subtleBorder
}

/** Translucent card with hairline border — the base dashboard surface. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = PsRadius.xl,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val (bg, border) = glassColors()
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.TopStart
    ) { content() }
}

// ─── Server status ────────────────────────────────────────────────

@Composable
fun statusColor(status: ServerStatus): Color {
    val extra = LocalPsExtra.current
    return when (status) {
        ServerStatus.RUNNING -> extra.success
        ServerStatus.STARTING -> MaterialTheme.colorScheme.primary
        ServerStatus.ERROR -> MaterialTheme.colorScheme.error
        ServerStatus.STOPPED -> extra.tertiaryText
    }
}

fun statusLabel(status: ServerStatus): String = when (status) {
    ServerStatus.RUNNING -> "Running"
    ServerStatus.STARTING -> "Starting"
    ServerStatus.ERROR -> "Error"
    ServerStatus.STOPPED -> "Offline"
}

/**
 * Premium status dot: subtle glow + gentle pulse while running/starting.
 * Rests completely still when offline.
 */
@Composable
fun StatusDot(
    status: ServerStatus,
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp
) {
    val color = statusColor(status)
    if (status == ServerStatus.RUNNING || status == ServerStatus.STARTING) {
        val transition = rememberInfiniteTransition(label = "statusPulse")
        val pulse by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        Box(modifier = modifier.size(dotSize + 14.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dotSize + 14.dp)
                    .alpha(0.35f * pulse)
                    .background(color.copy(alpha = 0.25f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(color, CircleShape)
            )
        }
    } else {
        Box(modifier = modifier.size(dotSize + 14.dp), contentAlignment = Alignment.Center) {
            if (status == ServerStatus.STOPPED) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(
                            androidx.compose.foundation.BorderStroke(1.5.dp, color),
                            CircleShape
                        )
                )
            } else {
                Box(modifier = Modifier.size(dotSize).background(color, CircleShape))
            }
        }
    }
}

/** Status pill like the reference: tinted capsule, dot + uppercase label.
 * Color cross-fades (150ms) and label swaps via AnimatedContent — no popping. */
@Composable
fun StatusPill(status: ServerStatus, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    val animatedColor by androidx.compose.animation.animateColorAsState(
        targetValue = color,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "pillColor"
    )
    val text = when (status) {
        ServerStatus.RUNNING -> "SERVER RUNNING"
        ServerStatus.STARTING -> "STARTING…"
        ServerStatus.ERROR -> "SERVER ERROR"
        ServerStatus.STOPPED -> "SERVER OFFLINE"
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(animatedColor.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status == ServerStatus.STOPPED) {
            Box(
                Modifier
                    .size(8.dp)
                    .border(androidx.compose.foundation.BorderStroke(1.5.dp, animatedColor), CircleShape)
            )
        } else {
            Box(Modifier.size(8.dp).background(animatedColor, CircleShape))
        }
        Spacer(Modifier.width(8.dp))
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label = "pillText"
        ) { label ->
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = animatedColor
            )
        }
    }
}

// ─── Buttons ──────────────────────────────────────────────────────

/**
 * Premium CTA: blue → purple gradient, soft glow, icon + label.
 * Secondary variant is quiet glass.
 */
@Composable
fun GradientButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    // Monochrome CTA: solid primary (white on black / black on white).
    // Enabled color cross-fades; label morphs in place so Start ↔ Starting ↔
    // Stop never pops the whole hero. Same slot, same size.
    val targetBg = if (enabled) MaterialTheme.colorScheme.primary else LocalPsExtra.current.track
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "ctaBg"
    )
    val content = if (enabled) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = RoundedCornerShape(PsRadius.md)
            )
            .clip(RoundedCornerShape(PsRadius.md))
            .background(bg)
            .alpha(if (enabled) 1f else 0.7f)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                LoadingDots(color = content)
            } else {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                AnimatedContent(
                    targetState = label,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                    label = "ctaLabel"
                ) { text ->
                    Text(text, style = MaterialTheme.typography.titleMedium, color = content)
                }
            }
        }
    }
}

/** Quiet glass button for secondary actions (Share / Open). */
@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val (bg, border) = glassColors()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(PsRadius.md))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(PsRadius.md))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LoadingDots(color: Color) {
    val transition = rememberInfiniteTransition(label = "dots")
    val p by transition.animateFloat(0f, 2f, infiniteRepeatable(tween(900)), label = "p")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val a = 0.35f + 0.65f * ((p + i) % 3f / 2f)
            Box(Modifier.size(7.dp).alpha(a).background(color, CircleShape))
        }
    }
}

// ─── Quick actions ────────────────────────────────────────────────

@Composable
fun QuickAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(PsRadius.md))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Storage ──────────────────────────────────────────────────────

@Composable
fun StorageBar(progress: Float, modifier: Modifier = Modifier) {
    val extra = LocalPsExtra.current
    // Smoothly interpolate jumps (20% → 45%) instead of snapping. Isolated to
    // this composable so progress updates never recompose the parent card.
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "storageProgress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(extra.track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/**
 * Dialog entrance wrapper: fade + 0.96 → 1.0 scale over 180ms.
 * Wrap AlertDialog *content* (title/text/buttons are already window-managed)
 * so every dialog in the app shares one subtle entrance. No bounce.
 */
@Composable
fun DialogEntrance(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
            scaleIn(tween(180, easing = FastOutSlowInEasing), initialScale = 0.96f),
        label = "dialogEntrance"
    ) { content() }
}

// ─── Minimal server illustration ──────────────────────────────────
// Professional line-art server stack: rounded slabs, LED dots, base ellipse.

@Composable
fun ServerGraphic(modifier: Modifier = Modifier, running: Boolean = false) {
    val accent = MaterialTheme.colorScheme.onSurface
    val dim = LocalPsExtra.current.tertiaryText
    val slab = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // faint orbit rings, inset so nothing touches the canvas edge
        drawArc(
            color = accent.copy(alpha = 0.18f),
            startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.09f, h * 0.06f), size = Size(w * 0.82f, h * 0.86f),
            style = Stroke(width = 1.5f)
        )
        drawArc(
            color = accent.copy(alpha = 0.1f),
            startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.17f, h * 0.13f), size = Size(w * 0.66f, h * 0.72f),
            style = Stroke(width = 1.5f)
        )
        // base ellipse
        drawOval(
            color = accent.copy(alpha = 0.14f),
            topLeft = Offset(w * 0.16f, h * 0.8f),
            size = Size(w * 0.68f, h * 0.14f)
        )
        drawOval(
            color = accent.copy(alpha = 0.3f),
            topLeft = Offset(w * 0.16f, h * 0.8f),
            size = Size(w * 0.68f, h * 0.14f),
            style = Stroke(width = 1.5f)
        )
        // three slabs
        val slabW = w * 0.44f
        val slabH = h * 0.13f
        val left = (w - slabW) / 2f
        var top = h * 0.2f
        repeat(3) { i ->
            drawRoundRect(
                color = slab,
                topLeft = Offset(left, top),
                size = Size(slabW, slabH),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.35f),
                topLeft = Offset(left, top),
                size = Size(slabW, slabH),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1.5f)
            )
            // slot line
            drawLine(
                color = dim.copy(alpha = 0.7f),
                start = Offset(left + slabW * 0.14f, top + slabH * 0.5f),
                end = Offset(left + slabW * 0.62f, top + slabH * 0.5f),
                strokeWidth = 2f
            )
            // LED
            val ledOn = running || i == 2
            drawCircle(
                color = if (ledOn) accent else dim.copy(alpha = 0.5f),
                radius = 3f,
                center = Offset(left + slabW * 0.78f, top + slabH * 0.5f)
            )
            top += slabH + h * 0.045f
        }
    }
}

// ─── Atmospheric AMOLED background ────────────────────────────────

@Composable
fun AmoledBackground(modifier: Modifier = Modifier) {
    val extra = LocalPsExtra.current
    val base = MaterialTheme.colorScheme.background
    val glow = MaterialTheme.colorScheme.onSurface
    val glowAlpha = if (extra.isDark) 0.06f else 0.10f
    Canvas(modifier = modifier) {
        drawRect(base)
        // whisper of neutral light, top — no color anywhere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow.copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(size.width * 0.5f, -size.height * 0.05f),
                radius = size.width * 0.9f
            ),
            radius = size.width * 0.9f,
            center = Offset(size.width * 0.5f, -size.height * 0.05f)
        )
    }
}

// ─── Typography helpers ───────────────────────────────────────────

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.eyebrow,
        color = LocalPsExtra.current.tertiaryText,
        modifier = modifier
    )
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = 1
) {
    Text(
        text,
        style = if (small) MaterialTheme.typography.monoSmall else MaterialTheme.typography.mono,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

// ─── Sections ─────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onAction).padding(PsSpacing.xs)
            )
        }
    }
}

// ─── File icons ───────────────────────────────────────────────────

@Composable
fun FileKindIcon(
    kind: FileKind,
    modifier: Modifier = Modifier,
    containerSize: androidx.compose.ui.unit.Dp = 40.dp
) {
    // Fully monochrome file system; type is carried by the glyph itself.
    val icon = when (kind) {
        FileKind.Folder -> Icons.Filled.Folder
        FileKind.Image -> Icons.Filled.Image
        FileKind.Video -> Icons.Filled.VideoFile
        FileKind.Audio -> Icons.Filled.AudioFile
        FileKind.Pdf -> Icons.Filled.PictureAsPdf
        FileKind.Archive -> Icons.Filled.Archive
        FileKind.Code -> Icons.Filled.Code
        FileKind.Document, FileKind.Unknown -> Icons.Filled.Description
    }
    val tint = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .size(containerSize)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(containerSize * 0.55f))
    }
}

/** Compact file row — rows, not huge cards. */
@Composable
fun FileRow(
    name: String,
    subtitle: String,
    isDir: Boolean,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PsRadius.md))
            .clickable(onClick = onClick)
            .padding(horizontal = PsSpacing.sm, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileKindIcon(kind = FileKind.of(name, isDir))
        Spacer(Modifier.width(PsSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(PsSpacing.sm))
            trailing()
        } else {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = LocalPsExtra.current.tertiaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── States ───────────────────────────────────────────────────────

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = PsSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(PsSpacing.lg))
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onAction).padding(PsSpacing.sm)
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = PsSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRetry != null) {
            Spacer(Modifier.height(PsSpacing.lg))
            Text(
                "Try again",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onRetry).padding(PsSpacing.sm)
            )
        }
    }
}

/** Skeleton rows for loading — shimmer-free gentle placeholders. */
@Composable
fun LoadingRows(
    modifier: Modifier = Modifier,
    rows: Int = 4
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val track = LocalPsExtra.current.track
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(rows) { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .alpha(alpha)
                        .background(track)
                )
                Spacer(Modifier.width(PsSpacing.md))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.fillMaxWidth(if (i % 2 == 0) 0.6f else 0.4f).height(12.dp)
                            .clip(RoundedCornerShape(6.dp)).alpha(alpha)
                            .background(track)
                    )
                    Box(
                        Modifier.fillMaxWidth(0.35f).height(10.dp)
                            .clip(RoundedCornerShape(6.dp)).alpha(alpha)
                            .background(track)
                    )
                }
            }
        }
    }
}

// ─── Activity ─────────────────────────────────────────────────────

enum class ActivityKind { Connection, Upload, Download, Start, Stop, Error, Other }

fun activityKindOf(method: String, status: Int): ActivityKind {
    if (status >= 500) return ActivityKind.Error
    return when {
        method == "START" -> ActivityKind.Start
        method == "STOP" || method == "FTP_STOP" -> ActivityKind.Stop
        method == "POST" || method.startsWith("FTP") && method.contains("STOR") -> ActivityKind.Upload
        method == "GET" || method == "HEAD" -> ActivityKind.Download
        method.startsWith("FTP") -> ActivityKind.Connection
        status in 200..299 -> ActivityKind.Connection
        status in 400..499 -> ActivityKind.Error
        else -> ActivityKind.Other
    }
}

/** Circular tinted icon like the reference timeline. */
@Composable
fun ActivityIcon(kind: ActivityKind, modifier: Modifier = Modifier) {
    val extra = LocalPsExtra.current
    val iconAndTint: Pair<ImageVector, Color> = when (kind) {
        ActivityKind.Upload -> Icons.Filled.ArrowUpward to MaterialTheme.colorScheme.primary
        ActivityKind.Download -> Icons.Filled.ArrowDownward to extra.success
        ActivityKind.Connection -> Icons.Filled.ChevronRight to MaterialTheme.colorScheme.onSurfaceVariant
        ActivityKind.Start -> Icons.Filled.PlayArrow to extra.success
        ActivityKind.Stop -> Icons.Filled.Stop to MaterialTheme.colorScheme.error
        ActivityKind.Error -> Icons.Filled.ChevronRight to MaterialTheme.colorScheme.error
        ActivityKind.Other -> Icons.Filled.ChevronRight to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = iconAndTint.first
    val tint = iconAndTint.second
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Reference-style activity row: circular icon, title, time, chevron. */
@Composable
fun ActivityRow(
    title: String,
    time: String,
    kind: ActivityKind,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActivityIcon(kind)
            Spacer(Modifier.width(PsSpacing.lg))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = LocalPsExtra.current.tertiaryText,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp + PsSpacing.lg)
                    .height(1.dp)
                    .background(LocalPsExtra.current.subtleBorder)
            )
        }
    }
}
