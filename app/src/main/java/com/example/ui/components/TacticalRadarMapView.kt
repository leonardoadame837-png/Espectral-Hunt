package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RfSignalInfo
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TacticalRadarMapView(
    signals: List<RfSignalInfo>,
    selectedSignal: RfSignalInfo?,
    onSelectSignal: (RfSignalInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val radarAngleDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )

    val pingPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Map Header & Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TACTICAL SIGNAL TRIANGULATION MAP",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "3-NODE TDoA / AoA BEARING RADAR (GRID: TACTICAL-SECTOR-7)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF002B1D))
                    .border(1.dp, NeonGreen, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "TRIANGULATING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Canvas Radar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF030A14))
                .border(1.dp, Color(0xFF16324A), RoundedCornerShape(8.dp))
                .pointerInput(signals) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = minOf(centerX, centerY) * 0.88f

                        // Find closest tapped signal
                        val tapped = signals.minByOrNull { signal ->
                            val rad = Math.toRadians((signal.azimuthDegrees - 90.0))
                            val distRatio = (signal.estimatedDistanceM / 400f).coerceIn(0.2f, 0.95f)
                            val sigX = centerX + (cos(rad) * radius * distRatio).toFloat()
                            val sigY = centerY + (sin(rad) * radius * distRatio).toFloat()

                            val dx = offset.x - sigX
                            val dy = offset.y - sigY
                            (dx * dx + dy * dy)
                        }

                        if (tapped != null) {
                            onSelectSignal(tapped)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h / 2f)
                val maxRadius = minOf(w, h) / 2f * 0.88f

                // 1. Tactical Grid Background Lines
                val gridStep = 30f
                var gx = 0f
                while (gx < w) {
                    drawLine(
                        color = Color(0x1500E5FF),
                        start = Offset(gx, 0f),
                        end = Offset(gx, h),
                        strokeWidth = 1f
                    )
                    gx += gridStep
                }
                var gy = 0f
                while (gy < h) {
                    drawLine(
                        color = Color(0x1500E5FF),
                        start = Offset(0f, gy),
                        end = Offset(w, gy),
                        strokeWidth = 1f
                    )
                    gy += gridStep
                }

                // 2. Concentric Distance Range Rings (100m, 200m, 300m, 400m)
                val rings = 4
                for (i in 1..rings) {
                    val r = maxRadius * (i.toFloat() / rings)
                    drawCircle(
                        color = Color(0x3000FF9D),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.2f)
                    )
                }

                // Crosshairs
                drawLine(
                    color = Color(0x4000FF9D),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0x4000FF9D),
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1f
                )

                // 3. Receiver Triangulation Nodes (Fixed Anchor Stations)
                val nodeA = Offset(center.x - maxRadius * 0.65f, center.y + maxRadius * 0.5f)
                val nodeB = Offset(center.x + maxRadius * 0.65f, center.y + maxRadius * 0.5f)
                val nodeC = Offset(center.x, center.y - maxRadius * 0.75f)

                val nodes = listOf(Pair("NODE-α", nodeA), Pair("NODE-β", nodeB), Pair("NODE-γ", nodeC))
                nodes.forEach { (name, pos) ->
                    drawCircle(
                        color = NeonCyan,
                        radius = 5f,
                        center = pos
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.4f),
                        radius = 9f,
                        center = pos,
                        style = Stroke(width = 1f)
                    )
                }

                // 4. Rotating Radar Sweep Beam
                val sweepRad = Math.toRadians((radarAngleDeg - 90.0))
                val sweepEnd = Offset(
                    (center.x + cos(sweepRad) * maxRadius).toFloat(),
                    (center.y + sin(sweepRad) * maxRadius).toFloat()
                )

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(NeonGreen.copy(alpha = 0.9f), Color.Transparent),
                        start = center,
                        end = sweepEnd
                    ),
                    start = center,
                    end = sweepEnd,
                    strokeWidth = 2.5f
                )

                // 5. Plotted Signals & Bearing Lines
                signals.forEach { sig ->
                    val isSelected = sig.id == selectedSignal?.id
                    val rad = Math.toRadians((sig.azimuthDegrees - 90.0))
                    val distRatio = (sig.estimatedDistanceM / 400f).coerceIn(0.2f, 0.95f)
                    val sigPos = Offset(
                        (center.x + cos(rad) * maxRadius * distRatio).toFloat(),
                        (center.y + sin(rad) * maxRadius * distRatio).toFloat()
                    )

                    val sigColor = if (sig.isRogue) NeonRed else NeonGreen

                    // Draw Bearing Triangulation Lines from Nodes to Signal
                    if (isSelected || sig.isRogue) {
                        nodes.forEach { (_, nodePos) ->
                            drawLine(
                                color = sigColor.copy(alpha = 0.45f),
                                start = nodePos,
                                end = sigPos,
                                strokeWidth = 1.2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        }

                        // Pulsing Rogue Threat Ring
                        if (sig.isRogue) {
                            drawCircle(
                                color = NeonRed.copy(alpha = (1.0f - (pingPulse - 1f) / 1.4f).coerceIn(0f, 0.8f)),
                                radius = 8f * pingPulse,
                                center = sigPos,
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }

                    // Signal Dot
                    drawCircle(
                        color = sigColor,
                        radius = if (isSelected) 8f else 5.5f,
                        center = sigPos
                    )
                }

                // Center Origin Point
                drawCircle(color = Color.White, radius = 3.5f, center = center)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Target Telemetry Card
        selectedSignal?.let { sig ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, if (sig.isRogue) NeonRed else NeonCyan, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (sig.isRogue) NeonRed else NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (sig.isRogue) "ROGUE TRANSMITTER LOCKED" else "AUTHORIZED SIGNAL LOCKED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (sig.isRogue) NeonRed else NeonGreen
                            )
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "FREQ: %.2f MHz | AZIMUTH: %.1f° | DIST: %.0fm | POWER: %.1f dBm",
                                sig.frequencyMhz,
                                sig.azimuthDegrees,
                                sig.estimatedDistanceM,
                                sig.powerDbm
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "PROTOCOL: ${sig.protocolName} | MOD: ${sig.modulationType}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
