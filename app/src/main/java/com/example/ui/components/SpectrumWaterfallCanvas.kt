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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.SpecBlue
import com.example.ui.theme.SpecCyan
import com.example.ui.theme.SpecGreen
import com.example.ui.theme.SpecRed
import com.example.ui.theme.SpecYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.sin

@Composable
fun SpectrumWaterfallCanvas(
    centerFrequencyMhz: Double,
    spanMhz: Double,
    signals: List<RfSignalInfo>,
    selectedSignal: RfSignalInfo?,
    onSelectSignal: (RfSignalInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rf_sweep")
    val sweepPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepPhase"
    )

    var touchMarkerX by remember { mutableFloatStateOf(0.5f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        // Header Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "REAL-TIME SPECTRAL WATERFALL & FFT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "CF: %.2f MHz | SPAN: %.1f MHz | RBW: 10 kHz | 60 FPS",
                        centerFrequencyMhz,
                        spanMhz
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberSurfaceElevated)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "DYNAMIC PHOSPHOR",
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

        // Spectrum Graph & Waterfall
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF020612))
                .border(1.dp, Color(0xFF1B2A4A), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val ratio = offset.x / size.width
                        touchMarkerX = ratio.coerceIn(0f, 1f)
                        // Find closest signal
                        val targetFreq = centerFrequencyMhz - (spanMhz / 2.0) + (ratio * spanMhz)
                        val closest = signals.minByOrNull { kotlin.math.abs(it.frequencyMhz - targetFreq) }
                        if (closest != null) {
                            onSelectSignal(closest)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val fftHeight = h * 0.55f
                val waterfallHeight = h * 0.45f

                // 1. Draw Grid Lines (dBm levels and Frequencies)
                val gridLinesY = 5
                for (i in 0..gridLinesY) {
                    val y = (fftHeight / gridLinesY) * i
                    drawLine(
                        color = Color(0x2200E5FF),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                val gridLinesX = 8
                for (i in 0..gridLinesX) {
                    val x = (w / gridLinesX) * i
                    drawLine(
                        color = Color(0x2200E5FF),
                        start = Offset(x, 0f),
                        end = Offset(x, fftHeight),
                        strokeWidth = 1f
                    )
                }

                // 2. Synthesize Real-Time FFT Waveform curve
                val path = Path()
                val fillPath = Path()
                val pointsCount = 120
                val minFreq = centerFrequencyMhz - (spanMhz / 2.0)
                val maxFreq = centerFrequencyMhz + (spanMhz / 2.0)

                var firstPoint = Offset(0f, fftHeight * 0.85f)
                path.moveTo(firstPoint.x, firstPoint.y)
                fillPath.moveTo(0f, fftHeight)
                fillPath.lineTo(firstPoint.x, firstPoint.y)

                for (i in 0..pointsCount) {
                    val xRatio = i.toFloat() / pointsCount
                    val x = xRatio * w
                    val currentPointFreq = minFreq + (xRatio * spanMhz)

                    // Base noise floor with sweep modulation
                    val noise = (sin((i * 0.8f + sweepPhase).toDouble()) * 4f).toFloat() +
                            (sin((i * 1.7f - sweepPhase * 0.5f).toDouble()) * 3f).toFloat()
                    var powerLevelRatio = 0.15f + (noise / 80f)

                    // Add Peaks for active signals
                    signals.forEach { signal ->
                        val freqDiff = kotlin.math.abs(signal.frequencyMhz - currentPointFreq)
                        val peakWidth = (signal.bandwidthKhz / 1000.0) * 1.5
                        if (freqDiff < peakWidth) {
                            val peakInfluence = (1.0 - (freqDiff / peakWidth)).toFloat()
                            val signalPowerNorm = ((signal.powerDbm + 100f) / 80f).coerceIn(0.2f, 0.95f)
                            powerLevelRatio = maxOf(powerLevelRatio, signalPowerNorm * peakInfluence)
                        }
                    }

                    val y = (fftHeight * (1.0f - powerLevelRatio)).coerceIn(8f, fftHeight - 2f)

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    fillPath.lineTo(x, y)
                }

                fillPath.lineTo(w, fftHeight)
                fillPath.close()

                // Draw Gradient Fill under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.45f),
                            SpecGreen.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = fftHeight
                    )
                )

                // Draw Waveform Line
                drawPath(
                    path = path,
                    color = NeonCyan,
                    style = Stroke(width = 2.2f)
                )

                // 3. Draw Peak Markers & Rogue Signal Tags
                signals.forEach { signal ->
                    val signalXRatio = ((signal.frequencyMhz - minFreq) / spanMhz).toFloat()
                    if (signalXRatio in 0.05f..0.95f) {
                        val peakX = signalXRatio * w
                        val peakY = (fftHeight * (1.0f - ((signal.powerDbm + 100f) / 80f).coerceIn(0.2f, 0.95f)))

                        // Draw Pin marker
                        val markerColor = if (signal.isRogue) NeonRed else NeonGreen
                        drawCircle(
                            color = markerColor,
                            radius = if (signal.id == selectedSignal?.id) 6f else 4f,
                            center = Offset(peakX, peakY)
                        )
                        drawLine(
                            color = markerColor.copy(alpha = 0.7f),
                            start = Offset(peakX, peakY),
                            end = Offset(peakX, fftHeight),
                            strokeWidth = 1.5f
                        )
                    }
                }

                // 4. Draw Waterfall Spectrogram Cascade (Bottom Half)
                val waterfallStartY = fftHeight + 2f
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(0f, waterfallStartY),
                    end = Offset(w, waterfallStartY),
                    strokeWidth = 1.5f
                )

                val rows = 18
                val rowHeight = waterfallHeight / rows
                for (r in 0 until rows) {
                    val rowY = waterfallStartY + (r * rowHeight)
                    val rowPhaseOffset = (sweepPhase * 2.0f + r * 15f) % 100f

                    for (c in 0..60) {
                        val colRatio = c / 60f
                        val cellX = colRatio * w
                        val cellFreq = minFreq + (colRatio * spanMhz)

                        var cellHeat = 0.1f + (sin((c * 0.6f + rowPhaseOffset).toDouble()) * 0.08f).toFloat()

                        signals.forEach { sig ->
                            val diff = kotlin.math.abs(sig.frequencyMhz - cellFreq)
                            if (diff < (sig.bandwidthKhz / 1000.0)) {
                                cellHeat = if (sig.isRogue) 0.95f else 0.65f
                            }
                        }

                        val heatColor = when {
                            cellHeat > 0.8f -> SpecRed
                            cellHeat > 0.6f -> SpecYellow
                            cellHeat > 0.4f -> SpecGreen
                            cellHeat > 0.25f -> SpecCyan
                            else -> SpecBlue.copy(alpha = 0.4f)
                        }

                        drawRect(
                            color = heatColor.copy(alpha = (1.0f - (r.toFloat() / rows) * 0.5f)),
                            topLeft = Offset(cellX, rowY),
                            size = Size(w / 60f + 1f, rowHeight + 0.5f)
                        )
                    }
                }

                // 5. Draw Interactive Cursor Marker
                val cursorX = touchMarkerX * w
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cursorX, 0f),
                    end = Offset(cursorX, h),
                    strokeWidth = 1.2f
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Frequency Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val minF = centerFrequencyMhz - (spanMhz / 2.0)
            val midF = centerFrequencyMhz
            val maxF = centerFrequencyMhz + (spanMhz / 2.0)

            Text(
                text = String.format(Locale.US, "%.2f MHz", minF),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextMuted
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = String.format(Locale.US, "▲ TUNED: %.2f MHz", midF),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = String.format(Locale.US, "%.2f MHz", maxF),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextMuted
                )
            )
        }
    }
}
