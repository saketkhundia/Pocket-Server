package com.saketkhundia.pocketserver.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.presentation.glass.GlassBackground
import com.saketkhundia.pocketserver.presentation.glass.GlassLevel
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassActivityItem
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassButton
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassEmptyState
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassErrorState
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassIconCircle
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassLoadingRows
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassProgressBar
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassSecondaryButton
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassStatusPill
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassSurface
import com.saketkhundia.pocketserver.presentation.glass.LocalGlassColors
import com.saketkhundia.pocketserver.presentation.theme.FileKind
import com.saketkhundia.pocketserver.presentation.theme.LocalPsExtra
import com.saketkhundia.pocketserver.presentation.theme.PsRadius
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import com.saketkhundia.pocketserver.presentation.theme.eyebrow
import com.saketkhundia.pocketserver.presentation.theme.mono
import com.saketkhundia.pocketserver.presentation.theme.monoSmall

// ─── Glass surfaces (liquid-glass backed) ───────────────────────────

@Composable
fun glassColors(): Pair<Color, Color> {
    val g = LocalGlassColors.current
    return g.surfaceL2 to g.border
}

/** Liquid-glass card — L2 surface, specular highlight, hairline border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = PsRadius.xl,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    LiquidGlassSurface(
        modifier = modifier, level = GlassLevel.L2, radius = radius, onClick = onClick
    ) { content() }
}

// ─── Server status ──────────────────────────────────────────────────

@Composable
fun statusColor(status: ServerStatus): Color {
    val g = LocalGlassColors.current
    return when (status) {
        ServerStatus.RUNNING -> g.success
        ServerStatus.STARTING -> g.warning
        ServerStatus.ERROR -> g.error
        ServerStatus.STOPPED -> g.textTertiary
    }
}

fun statusLabel(status: ServerStatus): String = when (status) {
    ServerStatus.RUNNING -> "Running"
    ServerStatus.STARTING -> "Starting"
    ServerStatus.ERROR -> "Error"
    ServerStatus.STOPPED -> "Offline"
}

/** Delegates to liquid-glass status pill visuals (glow dot + L3 glass). */
@Composable
fun StatusDot(
    status: ServerStatus,
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp
) {
    val color = statusColor(status)
    LiquidGlassIconCircle(
        icon = if (status == ServerStatus.RUNNING) Icons.Filled.PlayArrow else Icons.Filled.Stop,
        tint = color, size = dotSize + 14.dp, modifier = modifier
    )
}

/** Translucent glass status pill — red/green/amber glowing dot. */
@Composable
fun StatusPill(status: ServerStatus, modifier: Modifier = Modifier) {
    LiquidGlassStatusPill(status = status, modifier = modifier)
}

// ─── Buttons (gold liquid-glass) ────────────────────────────────────

@Composable
fun GradientButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    // Stop maps to danger (red glow), everything else gold.
    val danger = label.contains("Stop", ignoreCase = true)
    LiquidGlassButton(
        label = label, onClick = onClick, modifier = modifier,
        icon = icon, enabled = enabled, loading = loading, danger = danger
    )
}

/** Quiet glass button for secondary actions (Share / Open). */
@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    LiquidGlassSecondaryButton(label = label, onClick = onClick, modifier = modifier, icon = icon)
}

// ─── Quick actions → feature tiles ──────────────────────────────────

@Composable
fun QuickAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val g = LocalGlassColors.current
    // Icon chip: white wash on dark; solid #F0F1F2 + #222 icon on light.
    // Titles #222 / subtitles #777 per light spec — never washed out.
    val chipBg = if (g.isDark) g.gold.copy(alpha = 0.13f) else Color(0xFFF0F1F2)
    val iconTint = if (g.isDark) g.goldSoft else Color(0xFF222222)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(PsRadius.md))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape)
                .background(chipBg)
                .border(1.dp, g.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = g.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = g.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─── Storage (gold progress) ────────────────────────────────────────

@Composable
fun StorageBar(progress: Float, modifier: Modifier = Modifier) {
    LiquidGlassProgressBar(progress = progress, modifier = modifier, fill = LocalGlassColors.current.gold)
}

/**
 * Dialog entrance wrapper: fade + 0.96 → 1.0 scale over 200ms.
 */
@Composable
fun DialogEntrance(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
            scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.96f),
        label = "dialogEntrance"
    ) { content() }
}

// ─── Minimal server illustration (neutral line art) ───────────────────

@Composable
fun ServerGraphic(modifier: Modifier = Modifier, running: Boolean = false) {
    val g = LocalGlassColors.current
    // White line art on dark; neutral #555 gray on light (never tinted).
    val accent = if (g.isDark) g.goldSoft else Color(0xFF555555)
    val dim = g.textTertiary
    val slab = g.surfaceL3
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawArc(
            color = accent.copy(alpha = 0.22f),
            startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.09f, h * 0.06f), size = Size(w * 0.82f, h * 0.86f),
            style = Stroke(width = 1.5f)
        )
        drawArc(
            color = accent.copy(alpha = 0.12f),
            startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.17f, h * 0.13f), size = Size(w * 0.66f, h * 0.72f),
            style = Stroke(width = 1.5f)
        )
        drawOval(color = accent.copy(alpha = 0.14f), topLeft = Offset(w * 0.16f, h * 0.8f), size = Size(w * 0.68f, h * 0.14f))
        drawOval(color = accent.copy(alpha = 0.32f), topLeft = Offset(w * 0.16f, h * 0.8f), size = Size(w * 0.68f, h * 0.14f), style = Stroke(width = 1.5f))
        val slabW = w * 0.44f
        val slabH = h * 0.13f
        val left = (w - slabW) / 2f
        var top = h * 0.2f
        repeat(3) { i ->
            drawRoundRect(color = slab, topLeft = Offset(left, top), size = Size(slabW, slabH), cornerRadius = CornerRadius(6f, 6f))
            drawRoundRect(color = accent.copy(alpha = 0.35f), topLeft = Offset(left, top), size = Size(slabW, slabH), cornerRadius = CornerRadius(6f, 6f), style = Stroke(width = 1.5f))
            drawLine(color = dim.copy(alpha = 0.7f), start = Offset(left + slabW * 0.14f, top + slabH * 0.5f), end = Offset(left + slabW * 0.62f, top + slabH * 0.5f), strokeWidth = 2f)
            val ledOn = running || i == 2
            drawCircle(color = if (ledOn) accent else dim.copy(alpha = 0.5f), radius = 3f, center = Offset(left + slabW * 0.78f, top + slabH * 0.5f))
            top += slabH + h * 0.045f
        }
    }
}

// ─── Cinematic background (single shared layer) ─────────────────────

@Composable
fun AmoledBackground(modifier: Modifier = Modifier) {
    GlassBackground(modifier = modifier)
}

// ─── Typography helpers ─────────────────────────────────────────────

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.eyebrow, color = LocalGlassColors.current.textTertiary, modifier = modifier)
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    val g = LocalGlassColors.current
    Text(
        text,
        style = if (small) MaterialTheme.typography.monoSmall else MaterialTheme.typography.mono,
        color = if (color == Color.Unspecified) g.textPrimary else color,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier
    )
}

// ─── Sections ───────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val g = LocalGlassColors.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = g.textPrimary)
        if (actionLabel != null && onAction != null) {
            Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = g.gold, modifier = Modifier.clickable(onClick = onAction).padding(PsSpacing.xs))
        }
    }
}

// ─── File icons — ONE outlined family ───────────────────────────────

@Composable
fun FileKindIcon(
    kind: FileKind,
    modifier: Modifier = Modifier,
    containerSize: Dp = 40.dp
) {
    val g = LocalGlassColors.current
    // Chip: white wash on dark; #F0F1F2 + #222 icon on light.
    val chipBg = if (g.isDark) g.gold.copy(alpha = 0.10f) else Color(0xFFF0F1F2)
    val iconTint = if (g.isDark) g.goldSoft else Color(0xFF222222)
    val icon = when (kind) {
        FileKind.Folder -> Icons.Outlined.Folder
        FileKind.Image -> Icons.Outlined.Image
        FileKind.Video -> Icons.Outlined.Movie
        FileKind.Audio -> Icons.Outlined.AudioFile
        FileKind.Pdf -> Icons.Outlined.PictureAsPdf
        FileKind.Archive -> Icons.Outlined.Archive
        FileKind.Code, FileKind.Document, FileKind.Unknown -> Icons.Outlined.Description
    }
    Box(
        modifier = modifier.size(containerSize).clip(RoundedCornerShape(10.dp))
            .background(chipBg)
            .border(1.dp, g.border, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(containerSize * 0.55f))
    }
}

/** Glass list row — L2 surface, stable, no per-item blur. */
@Composable
fun FileRow(
    name: String,
    subtitle: String,
    isDir: Boolean,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val g = LocalGlassColors.current
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(PsRadius.md))
            .clickable(onClick = onClick)
            .padding(horizontal = PsSpacing.sm, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileKindIcon(kind = FileKind.of(name, isDir))
        Spacer(Modifier.width(PsSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = g.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = g.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Spacer(Modifier.width(PsSpacing.sm))
            trailing()
        } else {
            Icon(Icons.Filled.ChevronRight, null, tint = g.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── States (glass) ─────────────────────────────────────────────────

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    LiquidGlassEmptyState(title = title, subtitle = subtitle, modifier = modifier, actionLabel = actionLabel, onAction = onAction)
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    LiquidGlassErrorState(message = message, modifier = modifier, onRetry = onRetry)
}

/** Skeleton rows — L1 glass placeholders, gentle pulse. */
@Composable
fun LoadingRows(
    modifier: Modifier = Modifier,
    rows: Int = 4
) {
    LiquidGlassLoadingRows(modifier = modifier, rows = rows)
}

// ─── Activity ───────────────────────────────────────────────────────

enum class ActivityKind { Connection, Upload, Download, Start, Stop, Error, Photo, Other }

fun activityKindOf(method: String, status: Int): ActivityKind {
    if (method == "PHOTO" || method == "POST" && status in 200..299) {
        // Heuristic: photo uploads flagged by caller via path; keep generic here.
    }
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

fun activityDot(kind: ActivityKind): Color {
    // Resolved in composition below; helper for non-composable use returns fallback.
    return Color(0xFFF4C66A)
}

/** Circular glass icon — gold/green/red/purple by kind. */
@Composable
fun ActivityIcon(kind: ActivityKind, modifier: Modifier = Modifier) {
    val g = LocalGlassColors.current
    val (icon, tint) = when (kind) {
        ActivityKind.Upload -> Icons.Filled.ArrowUpward to g.success
        ActivityKind.Download -> Icons.Filled.ArrowDownward to g.success
        ActivityKind.Connection -> Icons.Filled.ChevronRight to g.textSecondary
        ActivityKind.Start -> Icons.Filled.PlayArrow to g.success
        ActivityKind.Stop -> Icons.Filled.Stop to g.error
        ActivityKind.Error -> Icons.Filled.ChevronRight to g.error
        ActivityKind.Photo -> Icons.Filled.Image to g.photo
        ActivityKind.Other -> Icons.Filled.ChevronRight to g.textSecondary
    }
    Box(
        modifier = modifier.size(44.dp).clip(CircleShape)
            .background(tint.copy(alpha = 0.13f))
            .border(1.dp, g.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Liquid-glass activity row. */
@Composable
fun ActivityRow(
    title: String,
    time: String,
    kind: ActivityKind,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val g = LocalGlassColors.current
    val dot = when (kind) {
        ActivityKind.Upload, ActivityKind.Download, ActivityKind.Start -> g.success
        ActivityKind.Stop, ActivityKind.Error -> g.error
        ActivityKind.Photo -> g.photo
        else -> g.gold
    }
    // Photo heuristic: title mentions photo → purple dot.
    val resolved = if (title.contains("photo", ignoreCase = true)) g.photo else dot
    LiquidGlassActivityItem(title = title, time = time, dot = resolved, modifier = modifier, onClick = onClick)
}
