package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Microwave
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.RecordVoiceOver
import com.example.data.hardware.VoiceCaptureEntry
import com.example.data.local.ThreatAlertEntity
import com.example.data.local.ThreatSignatureEntity
import com.example.data.model.RfSignalInfo
import com.example.ui.components.LiveThreatAlertBanner
import com.example.ui.components.MaliciousSignaturesDatabaseDialog
import com.example.ui.components.SpectrumWaterfallCanvas
import com.example.ui.components.ThreatAlertCard
import com.example.ui.components.ThreatAlertDetailDialog
import com.example.ui.components.ThreatDetectionHudCard
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
import kotlin.math.sin

data class FrequencyBandPreset(
    val name: String,
    val centerFreqMhz: Double,
    val spanMhz: Double,
    val description: String
)

@Composable
fun AudioOscilloscopeCanvas(
    isPlaying: Boolean,
    waveform: FloatArray,
    currentToneHz: Int,
    modulationType: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "oscilloscope")
    val phaseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF020A14))
            .border(1.dp, Color(0xFF10263E), RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // Draw grid lines
            val gridCols = 8
            for (i in 1 until gridCols) {
                val x = width * (i.toFloat() / gridCols)
                drawLine(
                    color = Color(0xFF0A1C2E),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }
            drawLine(
                color = Color(0xFF16324D),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1f
            )

            // Draw Live Oscilloscope Waveform Path
            val path = Path()
            val pointsCount = if (waveform.isNotEmpty()) waveform.size else 64
            val stepX = width / (pointsCount - 1)

            for (i in 0 until pointsCount) {
                val sample = if (isPlaying && waveform.isNotEmpty()) {
                    waveform[i]
                } else if (isPlaying) {
                    (sin(i * 0.35 + phaseAnim) * 0.6).toFloat()
                } else {
                    0f
                }
                val x = i * stepX
                val y = centerY - (sample * (height * 0.38f))

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Glow Stroke
            drawPath(
                path = path,
                color = if (isPlaying) NeonGreen.copy(alpha = 0.35f) else TextMuted.copy(alpha = 0.15f),
                style = Stroke(width = 6f)
            )

            // Primary Oscilloscope Line
            drawPath(
                path = path,
                color = if (isPlaying) NeonGreen else Color(0xFF334155),
                style = Stroke(width = 2.2f)
            )
        }

        // Overlay Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) NeonGreen else TextMuted)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "DEMOD OSCILLOSCOPE :: $currentToneHz Hz" else "OSCILLOSCOPE :: STANDBY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaying) NeonGreen else TextMuted
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isPlaying) Color(0xFF00382B) else Color(0xFF1E293B))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = modulationType,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaying) NeonCyan else TextMuted
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RealtimeVoiceCapturePanel(
    isVoiceRecording: Boolean,
    voiceRmsDb: Float,
    voicePeakFreqHz: Int,
    voiceLiveWaveform: FloatArray,
    isSpeechDetected: Boolean,
    capturedTranscripts: List<VoiceCaptureEntry>,
    onToggleVoiceCapture: () -> Unit,
    onAddManualVoiceNote: (String) -> Unit,
    onClearVoiceTranscripts: () -> Unit
) {
    var noteInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, if (isVoiceRecording) NeonRed else CyberBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Capture",
                    tint = if (isVoiceRecording) NeonRed else NeonAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ACOUSTIC VOICE CAPTURE & MIC MONITOR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isVoiceRecording) NeonRed else TextPrimary,
                        fontSize = 11.sp
                    )
                )
            }

            if (isSpeechDetected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF520B10))
                        .border(1.dp, NeonRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SPEECH DETECTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonRed,
                            fontSize = 8.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mic Waveform Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF090305))
                .border(1.dp, if (isVoiceRecording) Color(0xFF4A1016) else CyberBorder, RoundedCornerShape(8.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f

                drawLine(
                    color = Color(0xFF260D13),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1f
                )

                val pointsCount = if (voiceLiveWaveform.isNotEmpty()) voiceLiveWaveform.size else 64
                val stepX = width / (pointsCount - 1)
                val path = Path()

                for (i in 0 until pointsCount) {
                    val sample = if (isVoiceRecording) voiceLiveWaveform[i] else 0f
                    val x = i * stepX
                    val y = centerY - (sample * (height * 0.42f))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = if (isVoiceRecording) NeonRed.copy(alpha = 0.35f) else TextMuted.copy(alpha = 0.1f),
                    style = Stroke(width = 5f)
                )

                drawPath(
                    path = path,
                    color = if (isVoiceRecording) NeonRed else TextMuted,
                    style = Stroke(width = 2f)
                )
            }

            // Overlay Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isVoiceRecording) "LIVE MIC WAVEFORM :: ${voicePeakFreqHz} Hz" else "MIC MONITOR :: OFF",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (isVoiceRecording) NeonRed else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = String.format(Locale.US, "RMS: %.1f dB FS", voiceRmsDb),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (voiceRmsDb > -30f) NeonAmber else TextMuted,
                        fontSize = 9.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // RMS Decibel Level Progress Bar + Record Toggle Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MIC LEVEL:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    fontSize = 8.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            val levelProgress = ((voiceRmsDb + 60f) / 60f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { levelProgress },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    levelProgress > 0.85f -> NeonRed
                    levelProgress > 0.5f -> NeonAmber
                    else -> NeonGreen
                },
                trackColor = CyberSurfaceElevated
            )
            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onToggleVoiceCapture,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVoiceRecording) NeonRed else NeonAmber,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("voice_capture_toggle_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isVoiceRecording) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic Toggle",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVoiceRecording) "STOP MIC" else "REC VOICE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manual Voice Note / Intercept Entry Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = noteInputText,
                onValueChange = { noteInputText = it },
                placeholder = {
                    Text(
                        "Log acoustic voice transcript or tactical note...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("voice_note_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (noteInputText.isNotBlank()) {
                        onAddManualVoiceNote(noteInputText)
                        noteInputText = ""
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .testTag("send_voice_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Voice Note",
                    tint = NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (capturedTranscripts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTERCEPTED VOICE TRANSCRIPTS (${capturedTranscripts.size}):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "CLEAR ALL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NeonRed,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onClearVoiceTranscripts() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                capturedTranscripts.take(5).forEach { entry ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = "Voice Log",
                                        tint = NeonAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "[${entry.timestampFormatted}] ${entry.peakFrequencyHz} Hz | ${String.format(Locale.US, "%.1f dB", entry.dbVolume)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted,
                                            fontSize = 8.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = entry.textContent,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpectrumAnalyzerScreen(
    signals: List<RfSignalInfo>,
    selectedSignal: RfSignalInfo?,
    isAudioPlaying: Boolean,
    currentToneHz: Int,
    audioVolume: Float = 0.35f,
    currentModulationType: String = "FSK",
    audioWaveform: FloatArray = FloatArray(64),
    isVoiceRecording: Boolean = false,
    voiceRmsDb: Float = -60f,
    voicePeakFreqHz: Int = 0,
    voiceLiveWaveform: FloatArray = FloatArray(64),
    isSpeechDetected: Boolean = false,
    capturedVoiceTranscripts: List<VoiceCaptureEntry> = emptyList(),
    threatAlerts: List<ThreatAlertEntity> = emptyList(),
    threatSignatures: List<ThreatSignatureEntity> = emptyList(),
    activeAlertsCount: Int = 0,
    isThreatScanning: Boolean = false,
    isThreatSentinelActive: Boolean = true,
    selectedThreatAlert: ThreatAlertEntity? = null,
    onSelectSignal: (RfSignalInfo) -> Unit,
    onToggleAudioDemod: (RfSignalInfo?) -> Unit,
    onSetAudioVolume: (Float) -> Unit = {},
    onSetModulationType: (String) -> Unit = {},
    onUpdateAudioTone: (Int) -> Unit = {},
    onToggleVoiceCapture: () -> Unit = {},
    onAddManualVoiceNote: (String) -> Unit = {},
    onClearVoiceTranscripts: () -> Unit = {},
    onRunThreatScan: () -> Unit = {},
    onToggleThreatSentinel: () -> Unit = {},
    onSelectThreatAlert: (ThreatAlertEntity?) -> Unit = {},
    onUpdateThreatAlertStatus: (Long, String) -> Unit = { _, _ -> },
    onDismissThreatAlert: (Long) -> Unit = {},
    onDeleteThreatAlert: (Long) -> Unit = {},
    onClearAllThreatAlerts: () -> Unit = {},
    onToggleSignatureEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onAddCustomSignature: (ThreatSignatureEntity) -> Unit = {},
    onDeleteSignature: (String) -> Unit = {},
    onFocusAlertSignalOnMap: (ThreatAlertEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bandPresets = remember {
        listOf(
            FrequencyBandPreset("5.8 GHz Microwave", 5842.50, 80.0, "Microwave Point-to-Point Uplinks & Relays"),
            FrequencyBandPreset("915 MHz ISM Mesh", 915.00, 20.0, "Sub-GHz Tactical Mesh Waveforms"),
            FrequencyBandPreset("2.4 GHz ISM / BT", 2437.00, 60.0, "Standard WiFi 802.11 & Bluetooth"),
            FrequencyBandPreset("433 MHz Sub-GHz", 433.92, 10.0, "Industrial Telemetry & Rogue Beacons")
        )
    }

    var currentBand by remember { mutableStateOf(bandPresets[0]) }
    var fineTuneOffsetMhz by remember { mutableFloatStateOf(0f) }

    val activeCenterFreq = currentBand.centerFreqMhz + fineTuneOffsetMhz

    val availableModulations = remember {
        listOf("FSK", "AM", "FM", "CW / Morse", "PSK", "Chirp", "Microwave")
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var isDeepAnalyzing by remember { mutableStateOf(false) }
    var deepAnalysisProgress by remember { mutableFloatStateOf(0f) }
    var deepAnalysisLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var signalFilterMode by remember { mutableIntStateOf(0) } // 0 = ALL, 1 = ROGUE/DANGEROUS, 2 = AUTHORIZED

    var showSignaturesDbDialog by remember { mutableStateOf(false) }
    var showAlertsFeed by remember { mutableStateOf(true) }
    var viewingDetailAlert by remember { mutableStateOf<ThreatAlertEntity?>(null) }
    var alertFilterStatus by remember { mutableStateOf("ALL") }

    val filteredAlerts = remember(threatAlerts, alertFilterStatus) {
        if (alertFilterStatus == "ALL") threatAlerts
        else threatAlerts.filter { it.status.equals(alertFilterStatus, ignoreCase = true) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. RF MICROWAVE THREAT DETECTION SYSTEM HUD CARD
            item {
                ThreatDetectionHudCard(
                    activeAlertsCount = activeAlertsCount,
                    totalSignaturesCount = threatSignatures.size,
                    isScanning = isThreatScanning,
                    isSentinelActive = isThreatSentinelActive,
                    onRunScan = onRunThreatScan,
                    onToggleSentinel = onToggleThreatSentinel,
                    onOpenSignaturesDb = { showSignaturesDbDialog = true },
                    onViewAllAlerts = { showAlertsFeed = !showAlertsFeed }
                )
            }

            // 2. High-Priority Live Threat Alert Banner
            if (activeAlertsCount > 0) {
                val topActiveAlert = threatAlerts.firstOrNull { it.status == "ACTIVE" } ?: threatAlerts.first()
                item {
                    LiveThreatAlertBanner(
                        alert = topActiveAlert,
                        onClick = { viewingDetailAlert = topActiveAlert }
                    )
                }
            }

            // 3. Active Threat Alerts Feed (Collapsible / Filterable)
            if (threatAlerts.isNotEmpty() && showAlertsFeed) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurface)
                            .border(1.dp, if (activeAlertsCount > 0) NeonRed.copy(alpha = 0.5f) else CyberBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (activeAlertsCount > 0) NeonRed else NeonGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "THREAT DETECTION ALERTS FEED",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (activeAlertsCount > 0) NeonRed.copy(alpha = 0.2f) else CyberSurfaceElevated)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${filteredAlerts.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activeAlertsCount > 0) NeonRed else TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            if (threatAlerts.isNotEmpty()) {
                                Text(
                                    text = "CLEAR ALL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier
                                        .clickable { onClearAllThreatAlerts() }
                                        .padding(4.dp)
                                )
                            }
                        }

                        // Filter Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ALL", "ACTIVE", "INVESTIGATING", "MITIGATED").forEach { statusFilter ->
                                val isSelected = alertFilterStatus == statusFilter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                                        .border(1.dp, if (isSelected) NeonCyan else CyberBorder, RoundedCornerShape(6.dp))
                                        .clickable { alertFilterStatus = statusFilter }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = statusFilter,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isSelected) NeonCyan else TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }

                        // Threat Alert Cards List
                        filteredAlerts.forEach { alert ->
                            ThreatAlertCard(
                                alert = alert,
                                onSelect = { viewingDetailAlert = alert },
                                onTriangulate = {
                                    onFocusAlertSignalOnMap(alert)
                                    Toast.makeText(context, "Triangulating ${alert.matchedSignatureName} on Tactical Map", Toast.LENGTH_SHORT).show()
                                },
                                onUpdateStatus = { newStatus -> onUpdateThreatAlertStatus(alert.id, newStatus) },
                                onDismiss = { onDismissThreatAlert(alert.id) }
                            )
                        }
                    }
                }
            }

            // Frequency Band Quick Selectors
        item {
            Column {
                Text(
                    text = "SELECT SPECTRUM / MICROWAVE BAND:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bandPresets.forEach { band ->
                        val isSelected = band.name == currentBand.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF003644) else CyberSurfaceElevated)
                                .border(1.dp, if (isSelected) NeonCyan else CyberBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    currentBand = band
                                    fineTuneOffsetMhz = 0f
                                    val inBand = signals.find { sig ->
                                        kotlin.math.abs(sig.frequencyMhz - band.centerFreqMhz) < (band.spanMhz / 2.0)
                                    }
                                    if (inBand != null) onSelectSignal(inBand)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = band.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else TextPrimary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Live Real-Time Spectrum Waterfall Display
        item {
            SpectrumWaterfallCanvas(
                centerFrequencyMhz = activeCenterFreq,
                spanMhz = currentBand.spanMhz,
                signals = signals,
                selectedSignal = selectedSignal,
                onSelectSignal = onSelectSignal
            )
        }

        // Receiver Tuner & Demodulator Controls with Live Oscilloscope
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                // Panel Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Fine Tuning",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RECEIVER TUNER & SIGNAL DEMODULATOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Text(
                        text = String.format(Locale.US, "OFFSET: %+.2f MHz", fineTuneOffsetMhz),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live Audio Oscilloscope Display
                AudioOscilloscopeCanvas(
                    isPlaying = isAudioPlaying,
                    waveform = audioWaveform,
                    currentToneHz = currentToneHz,
                    modulationType = currentModulationType
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Fine Frequency Tuning Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FREQUENCY FINE-TUNE OFFSET:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 8.sp
                        )
                    )
                    Text(
                        text = "${activeCenterFreq.let { String.format(Locale.US, "%.2f MHz", it) }}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }

                Slider(
                    value = fineTuneOffsetMhz,
                    onValueChange = { offset ->
                        fineTuneOffsetMhz = offset
                        selectedSignal?.let { sig ->
                            val shiftedTone = (sig.audioToneFrequencyHz + (offset * 50)).toInt().coerceIn(150, 4000)
                            onUpdateAudioTone(shiftedTone)
                        }
                    },
                    valueRange = -10f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonGreen,
                        activeTrackColor = NeonGreen,
                        inactiveTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("frequency_tuner_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Modulation Type Selector Chips
                Column {
                    Text(
                        text = "DEMODULATION WAVEFORM MODE:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableModulations.forEach { mod ->
                            val isSelected = mod == currentModulationType
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF00382B) else CyberSurfaceElevated)
                                    .border(1.dp, if (isSelected) NeonGreen else CyberBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        onSetModulationType(mod)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = mod,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonGreen else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Volume Slider & Listen Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAudioPlaying) Color(0xFF022B1E) else CyberSurfaceElevated)
                        .border(1.dp, if (isAudioPlaying) NeonGreen else CyberBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (audioVolume == 0f) Icons.Default.VolumeOff else if (audioVolume < 0.5f) Icons.Default.VolumeDown else Icons.Default.VolumeUp,
                                contentDescription = "Volume",
                                tint = if (isAudioPlaying) NeonGreen else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OUTPUT VOLUME: ${(audioVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAudioPlaying) NeonGreen else TextPrimary,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Slider(
                            value = audioVolume,
                            onValueChange = { onSetAudioVolume(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = if (isAudioPlaying) NeonGreen else TextSecondary,
                                activeTrackColor = if (isAudioPlaying) NeonGreen else TextSecondary,
                                inactiveTrackColor = CyberSurface
                            ),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("audio_volume_slider")
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onToggleAudioDemod(selectedSignal) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAudioPlaying) NeonRed else NeonGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("demod_audio_listen_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.VolumeUp,
                                contentDescription = "Audio Play/Pause",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAudioPlaying) "MUTE" else "DEMOD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Live Acoustic Voice Capture & Microphone Panel
        item {
            RealtimeVoiceCapturePanel(
                isVoiceRecording = isVoiceRecording,
                voiceRmsDb = voiceRmsDb,
                voicePeakFreqHz = voicePeakFreqHz,
                voiceLiveWaveform = voiceLiveWaveform,
                isSpeechDetected = isSpeechDetected,
                capturedTranscripts = capturedVoiceTranscripts,
                onToggleVoiceCapture = onToggleVoiceCapture,
                onAddManualVoiceNote = onAddManualVoiceNote,
                onClearVoiceTranscripts = onClearVoiceTranscripts
            )
        }

        // TACTICAL SIGNAL ANALYST & RF INTELLIGENCE DOSSIER (Analista de Señales)
        selectedSignal?.let { sig ->
            item {
                SignalAnalystDossierCard(
                    sig = sig,
                    isDeepAnalyzing = isDeepAnalyzing,
                    deepAnalysisProgress = deepAnalysisProgress,
                    deepAnalysisLogs = deepAnalysisLogs,
                    onStartDeepAnalysis = {
                        if (!isDeepAnalyzing) {
                            coroutineScope.launch {
                                isDeepAnalyzing = true
                                deepAnalysisProgress = 0.05f
                                deepAnalysisLogs = listOf("Initiating real-time DSP spectrum capture on ${sig.frequencyMhz} MHz...")
                                delay(400)
                                deepAnalysisProgress = 0.25f
                                deepAnalysisLogs = deepAnalysisLogs + "Sampling IQ constellation (Modulation: ${sig.modulationType}) at 20 MSps..."
                                delay(450)
                                deepAnalysisProgress = 0.55f
                                deepAnalysisLogs = deepAnalysisLogs + "Carrier frequency stability verified (+/- 1.2 kHz deviation, SNR: ${sig.snrDb} dB)"
                                delay(450)
                                deepAnalysisProgress = 0.80f
                                deepAnalysisLogs = deepAnalysisLogs + "Inspecting frame cryptographic entropy: ${if (sig.isRogue) "0.94 (Unauthenticated / Vulnerable)" else "0.08 (AES/WPA Encrypted)"}"
                                delay(450)
                                deepAnalysisProgress = 1.0f
                                deepAnalysisLogs = deepAnalysisLogs + "Analysis Verdict: [${sig.threatRating}] Spectral Purity ${sig.spectralPurityPercent}% - Action: ${sig.recommendedAction}"
                                isDeepAnalyzing = false
                                Toast.makeText(context, "Deep Signal Analysis Complete: ${sig.threatRating}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onCopyDossier = {
                        val report = buildString {
                            appendLine("=== TACTICAL SIGNAL ANALYST REPORT ===")
                            appendLine("EMITTER: ${sig.protocolName} (${sig.frequencyMhz} MHz)")
                            appendLine("BAND: ${sig.bandName} | MODULATION: ${sig.modulationType}")
                            appendLine("THREAT RATING: ${sig.threatRating}")
                            appendLine("TRANSMISSION MODE: ${sig.transmissionMode}")
                            appendLine("ENCRYPTION STATE: ${sig.encryptionState}")
                            appendLine("SPECTRAL PURITY: ${sig.spectralPurityPercent}%")
                            appendLine("POWER / SNR: ${sig.powerDbm} dBm / ${sig.snrDb} dB")
                            appendLine("GEOLOCATION: ${sig.locationName} - ${sig.locationAddress}")
                            appendLine("BEARING: ${sig.azimuthDegrees}° | DISTANCE: ${sig.estimatedDistanceM}m")
                            appendLine()
                            appendLine("SIGNAL DESCRIPTION:")
                            appendLine(sig.description)
                            appendLine()
                            appendLine("ANALYST ASSESSMENT:")
                            appendLine(sig.analystAssessment)
                            appendLine()
                            appendLine("RECOMMENDED ACTION:")
                            appendLine(sig.recommendedAction)
                        }
                        clipboardManager.setText(AnnotatedString(report))
                        Toast.makeText(context, "Signal Analyst Dossier copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Deciphered Telemetry & Signal Payload Terminal
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurface)
                        .border(1.dp, if (sig.isRogue) NeonRed else NeonCyan, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (sig.isRogue) NeonRed else NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DECODED TELEMETRY STREAM & PAYLOAD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sig.isRogue) NeonRed else NeonCyan,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        if (sig.isRogue) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF380E15))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "UNCOORDINATED EMITTER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Decoded Text Terminal Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF030713))
                            .border(1.dp, Color(0xFF132238), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "ASCII DEMODULATED STREAM:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted,
                                    fontSize = 8.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sig.demodulatedText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = if (sig.isRogue) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Raw Hex Stream Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF030713))
                            .border(1.dp, Color(0xFF132238), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "RAW HEXADECIMAL PACKET BYTES:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted,
                                    fontSize = 8.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sig.rawHexPayload,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonAmber,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }
            }
        } ?: item {
            // Signal Analyst Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF091220))
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Signal Analyst",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TACTICAL SIGNAL ANALYST (ANALISTA DE SEÑALES)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Select any intercepted emitter below to open comprehensive technical descriptions, threat assessments, spectral purity analysis, and countermeasure plans.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // Active Intercepted Signals List with Descriptions & Analyst Badges
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE DETECTED RF EMITTERS (${signals.size}):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    )

                    // Filter mode indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (signalFilterMode == 0) NeonCyan.copy(alpha = 0.2f) else CyberSurface)
                                .border(1.dp, if (signalFilterMode == 0) NeonCyan else CyberBorder, RoundedCornerShape(4.dp))
                                .clickable { signalFilterMode = 0 }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ALL (${signals.size})",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (signalFilterMode == 0) NeonCyan else TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (signalFilterMode == 1) NeonRed.copy(alpha = 0.2f) else CyberSurface)
                                .border(1.dp, if (signalFilterMode == 1) NeonRed else CyberBorder, RoundedCornerShape(4.dp))
                                .clickable { signalFilterMode = 1 }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ROGUE (${signals.count { it.isRogue }})",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (signalFilterMode == 1) NeonRed else TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (signalFilterMode == 2) NeonGreen.copy(alpha = 0.2f) else CyberSurface)
                                .border(1.dp, if (signalFilterMode == 2) NeonGreen else CyberBorder, RoundedCornerShape(4.dp))
                                .clickable { signalFilterMode = 2 }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AUTH (${signals.count { !it.isRogue }})",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (signalFilterMode == 2) NeonGreen else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val filteredSignals = when (signalFilterMode) {
                    1 -> signals.filter { it.isRogue }
                    2 -> signals.filter { !it.isRogue }
                    else -> signals
                }

                filteredSignals.forEach { sig ->
                    val isSelected = sig.id == selectedSignal?.id
                    val cardBorder = if (isSelected) NeonCyan else if (sig.isRogue) Color(0xFF5E141E) else CyberBorder
                    val cardBg = if (isSelected) CyberSurfaceElevated else CyberSurface

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectSignal(sig) }
                            .padding(10.dp)
                    ) {
                        Column {
                            // Top Row: Frequency, Threat Badge, and Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (sig.isRogue) NeonRed else NeonGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.2f MHz - %s", sig.frequencyMhz, sig.protocolName),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Threat Rating Tag
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (sig.isRogue) Color(0xFF380E15) else Color(0xFF0F3B25))
                                            .border(1.dp, if (sig.isRogue) NeonRed else NeonGreen, RoundedCornerShape(3.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = sig.threatRating.ifBlank { if (sig.isRogue) "ROGUE" else "AUTH" },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sig.isRogue) NeonRed else NeonGreen,
                                                fontSize = 7.5.sp
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { onToggleAudioDemod(sig) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Listen to tone",
                                            tint = if (isAudioPlaying && selectedSignal?.id == sig.id) NeonGreen else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // SIGNAL DESCRIPTION BOX
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF030713))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "DESCRIPTION: ${sig.description.ifBlank { "Carrier operating on ${sig.frequencyMhz} MHz (${sig.bandName})." }}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 9.5.sp,
                                            lineHeight = 13.sp
                                        ),
                                        maxLines = 3
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Subtitle Metadata and Location Address
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Band: ${sig.bandName} | Mod: ${sig.modulationType} | Pwr: ${sig.powerDbm} dBm",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted,
                                        fontSize = 8.5.sp
                                    )
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = sig.locationAddress.take(28),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFFFD54F),
                                            fontSize = 8.5.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialogs for Threat Detection & Signatures Database
    val alertToView = viewingDetailAlert ?: selectedThreatAlert
    if (alertToView != null) {
        ThreatAlertDetailDialog(
            alert = alertToView,
            onDismiss = {
                viewingDetailAlert = null
                onSelectThreatAlert(null)
            },
            onTriangulateOnMap = {
                onFocusAlertSignalOnMap(alertToView)
                viewingDetailAlert = null
                onSelectThreatAlert(null)
            },
            onMarkStatus = { newStatus ->
                onUpdateThreatAlertStatus(alertToView.id, newStatus)
            },
            onDeleteAlert = {
                onDeleteThreatAlert(alertToView.id)
                viewingDetailAlert = null
                onSelectThreatAlert(null)
            }
        )
    }

    if (showSignaturesDbDialog) {
        MaliciousSignaturesDatabaseDialog(
            signatures = threatSignatures,
            onToggleEnabled = { sigId, isEnabled ->
                onToggleSignatureEnabled(sigId, isEnabled)
            },
            onAddCustomSignature = { newSig ->
                onAddCustomSignature(newSig)
            },
            onDeleteSignature = { sigId ->
                onDeleteSignature(sigId)
            },
            onDismiss = { showSignaturesDbDialog = false }
        )
    }
}
}

@Composable
fun SignalAnalystDossierCard(
    sig: RfSignalInfo,
    isDeepAnalyzing: Boolean,
    deepAnalysisProgress: Float,
    deepAnalysisLogs: List<String>,
    onStartDeepAnalysis: () -> Unit,
    onCopyDossier: () -> Unit
) {
    val themeColor = if (sig.threatRating.contains("ROGUE", true) || sig.isRogue) NeonRed 
        else if (sig.threatRating.contains("ELEVATED", true) || sig.threatRating.contains("HIGH", true)) NeonAmber 
        else NeonGreen

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_analyst")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, themeColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Dossier Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Signal Analyst",
                    tint = themeColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "TACTICAL SIGNAL ANALYST // DOSSIER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = themeColor,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "ANALISTA DE SEÑALES RF & SIGINT THREAT INTEL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 8.sp
                        )
                    )
                }
            }

            // Threat Rating Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(themeColor.copy(alpha = 0.18f))
                    .border(1.dp, themeColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(themeColor.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sig.threatRating.ifBlank { if (sig.isRogue) "ROGUE EMITTER" else "AUTHORIZED" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = themeColor,
                            fontSize = 8.5.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SECTION 1: SIGNAL DESCRIPTION (DESCRIPCIÓN DE SEÑAL)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF040A18))
                .border(1.dp, Color(0xFF132742), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "Description",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SIGNAL IDENTITY & OPERATIONAL DESCRIPTION:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 9.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = sig.description.ifBlank {
                        "Radio frequency transmitter operating on ${sig.frequencyMhz} MHz (${sig.bandName}) utilizing ${sig.modulationType} carrier modulation."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata Tags Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(CyberSurfaceElevated)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "MODE: ${sig.transmissionMode}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NeonAmber,
                                fontSize = 8.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(CyberSurfaceElevated)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SECURITY: ${sig.encryptionState}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (sig.isRogue) NeonRed else NeonGreen,
                                fontSize = 8.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(CyberSurfaceElevated)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BW: ${sig.bandwidthKhz.toInt()} kHz",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 8.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SECTION 2: ANALYST ASSESSMENT (EVALUACIÓN DEL ANALISTA)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (sig.isRogue) Color(0xFF1E070B) else Color(0xFF071810))
                .border(
                    1.dp,
                    if (sig.isRogue) Color(0xFF4A101A) else Color(0xFF0F3B25),
                    RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Analyst Assessment",
                        tint = if (sig.isRogue) NeonRed else NeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SIGINT ANALYST ASSESSMENT & TECHNICAL EVALUATION:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (sig.isRogue) NeonRed else NeonGreen,
                            fontSize = 9.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = sig.analystAssessment.ifBlank {
                        "Carrier modulation verified within nominal parameters. Passive intercept recording ongoing with zero anomalous phase deviations detected."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (sig.isRogue) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                        fontSize = 10.5.sp,
                        lineHeight = 14.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Spectral Purity Metric Gauge
                val purity = sig.spectralPurityPercent
                val purityColor = if (purity >= 95f) NeonGreen else if (purity >= 85f) NeonAmber else NeonRed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPECTRAL PURITY & CARRIER STABILITY:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 8.5.sp
                        )
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", purity),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = purityColor,
                            fontSize = 9.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                LinearProgressIndicator(
                    progress = purity / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = purityColor,
                    trackColor = Color(0xFF132238)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tactical Countermeasure
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Countermeasure",
                        tint = NeonAmber,
                        modifier = Modifier.size(13.dp).padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RECOMMENDED ACTION: ${sig.recommendedAction}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Geolocation Pin
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(13.dp).padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FACILITY: ${sig.locationName} (${sig.locationAddress}) | Azimuth: ${sig.azimuthDegrees.toInt()}° | Dist: ${sig.estimatedDistanceM.toInt()}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFD54F),
                            fontSize = 8.5.sp,
                            lineHeight = 11.5.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Deep Signal Analysis Terminal (if activated)
        if (isDeepAnalyzing || deepAnalysisLogs.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF030712))
                    .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "DSP",
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDeepAnalyzing) "REAL-TIME DSP ENGINE: SCANNING..." else "DSP SPECTRAL DIAGNOSTIC COMPLETE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    fontSize = 8.5.sp
                                )
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%.0f%%", deepAnalysisProgress * 100f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen,
                                fontSize = 8.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = deepAnalysisProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonCyan,
                        trackColor = Color(0xFF132238)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    deepAnalysisLogs.takeLast(4).forEach { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                fontSize = 8.sp,
                                lineHeight = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartDeepAnalysis,
                enabled = !isDeepAnalyzing,
                modifier = Modifier
                    .weight(1f)
                    .testTag("run_deep_signal_analysis_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Run Deep Analysis",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isDeepAnalyzing) "ANALYZING..." else "RUN DEEP ANALYSIS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                )
            }

            Button(
                onClick = onCopyDossier,
                modifier = Modifier
                    .weight(0.9f)
                    .testTag("copy_analyst_dossier_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberSurfaceElevated,
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Dossier",
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "COPY DOSSIER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

