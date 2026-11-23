package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadPriority
import com.example.data.model.DownloadStatus
import com.example.engine.FormatUtils
import com.example.ui.theme.LocalWin10Tokens
import com.example.ui.theme.WinMonoFamily

/**
 * Authentic Windows 10 File Copy / Transfer Progress Bar.
 * Sharp 0.dp corners, 1.dp border, Windows 10 Transfer Green (#06B025) with animated glossy pulse,
 * Amber on Paused, Red on Failed, and marquee sweep when indeterminate.
 */
@Composable
fun Win10ProgressBar(
    progress: Float,
    status: DownloadStatus,
    indeterminate: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWin10Tokens.current
    val barColor = when (status) {
        DownloadStatus.PAUSED -> tokens.pausedAmber
        DownloadStatus.FAILED, DownloadStatus.CANCELED -> tokens.errorRed
        DownloadStatus.PACKAGING, DownloadStatus.EXTRACTING -> tokens.accent
        else -> tokens.transferGreen
    }

    val infiniteTransition = rememberInfiniteTransition(label = "win10_progress_shimmer")
    val sweepFraction by infiniteTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_x"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(tokens.transferTrack, RectangleShape)
            .border(1.dp, tokens.border, RectangleShape)
            .clipToBounds()
    ) {
        if (indeterminate && status.isActive) {
            // Windows 10 Marquee Indeterminate Sweep
            Canvas(modifier = Modifier.fillMaxSize()) {
                val segWidth = size.width * 0.34f
                val left = (sweepFraction * size.width) - segWidth * 0.5f
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            barColor.copy(alpha = 0.85f),
                            barColor,
                            barColor.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startX = left,
                        endX = left + segWidth
                    ),
                    topLeft = Offset(left, 0f),
                    size = androidx.compose.ui.geometry.Size(segWidth, size.height)
                )
            }
        } else {
            val clamped = progress.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .background(barColor, RectangleShape)
            ) {
                if (status.isActive && clamped > 0.01f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pulseWidth = size.width * 0.45f.coerceAtLeast(48f)
                        val startX = (sweepFraction * (size.width + pulseWidth)) - pulseWidth
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.36f),
                                    Color.Transparent
                                ),
                                startX = startX,
                                endX = startX + pulseWidth
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Windows 10 "More details" Live Transfer Speed Area Graph.
 */
@Composable
fun Win10SpeedGraph(
    samples: List<Long>,
    currentSpeedBytes: Long,
    status: DownloadStatus,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWin10Tokens.current
    val strokeColor = when (status) {
        DownloadStatus.PAUSED -> tokens.pausedAmber
        DownloadStatus.FAILED -> tokens.errorRed
        else -> tokens.transferGreen
    }
    val peak = (samples.maxOrNull() ?: currentSpeedBytes).coerceAtLeast(1024L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.chromeBackground, RectangleShape)
            .border(1.dp, tokens.border, RectangleShape)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speed: ${FormatUtils.formatSpeed(currentSpeedBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Peak: ${FormatUtils.formatSpeed(peak)}",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    if (tokens.isDark) Color(0xFF141414) else Color.White,
                    RectangleShape
                )
                .border(1.dp, tokens.border, RectangleShape)
        ) {
            val w = size.width
            val h = size.height
            val gridColor = tokens.border.copy(alpha = 0.5f)
            // Horizontal grid lines
            for (i in 1..3) {
                val y = h * (i / 4f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            // Vertical grid lines
            for (i in 1..7) {
                val x = w * (i / 8f)
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            }

            val chartSamples = if (samples.isEmpty()) listOf(0L, 0L) else if (samples.size == 1) listOf(0L, samples.first()) else samples
            val maxVal = peak.toFloat().coerceAtLeast(1f)
            val stepX = w / (chartSamples.size - 1).coerceAtLeast(1)

            val linePath = Path()
            val fillPath = Path()
            chartSamples.forEachIndexed { idx, value ->
                val x = idx * stepX
                val ratio = (value.toFloat() / maxVal).coerceIn(0f, 1f)
                val y = h - (ratio * (h - 6f)) - 2f
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(w, h)
            fillPath.close()

            drawPath(path = fillPath, color = strokeColor.copy(alpha = 0.24f))
            drawPath(path = linePath, color = strokeColor, style = Stroke(width = 2.5f))
        }
    }
}

@Composable
fun Win10PriorityBadge(
    priority: DownloadPriority,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWin10Tokens.current
    val bg = when (priority) {
        DownloadPriority.URGENT -> tokens.urgentColor
        DownloadPriority.QUICK -> tokens.quickColor
        DownloadPriority.CASUAL -> tokens.casualColor
    }
    Surface(
        shape = RectangleShape,
        color = bg,
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onClick)
            } else Modifier
        )
    ) {
        Text(
            text = priority.label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun Win10TagBadge(
    text: String,
    accent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWin10Tokens.current
    Surface(
        shape = RectangleShape,
        color = if (accent) tokens.accent.copy(alpha = 0.14f) else tokens.elevatedSurface,
        border = BorderStroke(1.dp, if (accent) tokens.accent else tokens.border),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = WinMonoFamily,
            color = if (accent) tokens.accent else tokens.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun Win10ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val tokens = LocalWin10Tokens.current
    val bg = when {
        !enabled -> tokens.elevatedSurface.copy(alpha = 0.5f)
        primary -> tokens.accent
        else -> tokens.elevatedSurface
    }
    val fg = when {
        !enabled -> tokens.textSecondary.copy(alpha = 0.5f)
        primary -> Color.White
        else -> tokens.textPrimary
    }
    val borderColor = if (primary) tokens.accentPressed else tokens.borderStrong

    Surface(
        shape = RectangleShape,
        color = bg,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun Win10CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val tokens = LocalWin10Tokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = label,
            tint = if (checked) tokens.accent else tokens.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = tokens.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary
                )
            }
        }
    }
}
