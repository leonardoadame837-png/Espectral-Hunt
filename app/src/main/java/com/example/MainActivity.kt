package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sensors
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.MapTargetItem
import com.example.data.model.VulnerabilityFinding
import com.example.ui.components.PersistentLocationBar
import com.example.ui.components.TacticalTopAppBar
import com.example.ui.screens.AuditHistoryScreen
import com.example.ui.screens.BugHuntAgentScreen
import com.example.ui.screens.SpectrumAnalyzerScreen
import com.example.ui.screens.TacticalMapScreen
import com.example.ui.screens.PhoneEvidenceScreen
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SpectraViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SpectraApp()
            }
        }
    }
}

@Composable
fun SpectraApp(viewModel: SpectraViewModel = viewModel()) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedNavTab.collectAsState()
    val isHunting by viewModel.isHunting.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val currentToneHz by viewModel.currentAudioToneHz.collectAsState()
    val signals by viewModel.signals.collectAsState()
    val selectedSignal by viewModel.selectedSignal.collectAsState()
    val presets = viewModel.presets
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val currentCode by viewModel.currentCode.collectAsState()
    val targetName by viewModel.targetName.collectAsState()
    val agentThoughts by viewModel.agentThoughts.collectAsState()
    val consensusReport by viewModel.consensusReport.collectAsState()
    val findings by viewModel.findings.collectAsState()
    val selectedMapTarget by viewModel.selectedMapTarget.collectAsState()
    val isSaved by viewModel.isCurrentReportSaved.collectAsState()
    val savedReports by viewModel.savedReports.collectAsState()
    val interceptedSignals by viewModel.interceptedSignals.collectAsState()
    val environmentMode by viewModel.environmentMode.collectAsState()
    val deviceLocation by viewModel.deviceLocation.collectAsState()
    val audioVolume by viewModel.audioVolume.collectAsState()
    val currentModulationType by viewModel.currentModulationType.collectAsState()
    val audioWaveform by viewModel.audioWaveform.collectAsState()

    val isVoiceRecording by viewModel.isVoiceRecording.collectAsState()
    val voiceRmsDb by viewModel.voiceRmsDb.collectAsState()
    val voicePeakFreqHz by viewModel.voicePeakFreqHz.collectAsState()
    val voiceLiveWaveform by viewModel.voiceLiveWaveform.collectAsState()
    val isSpeechDetected by viewModel.isSpeechDetected.collectAsState()
    val capturedVoiceTranscripts by viewModel.capturedVoiceTranscripts.collectAsState()

    val threatAlerts by viewModel.threatAlerts.collectAsState()
    val threatSignatures by viewModel.threatSignatures.collectAsState()
    val isThreatScanning by viewModel.isThreatScanning.collectAsState()
    val isThreatSentinelActive by viewModel.isThreatSentinelActive.collectAsState()
    val selectedThreatAlert by viewModel.selectedThreatAlert.collectAsState()
    val activeAlertsCount = remember(threatAlerts) { threatAlerts.count { it.status == "ACTIVE" } }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceCapture()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshRealTelemetry()
            viewModel.startContinuousLocationTracking()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg),
        topBar = {
            androidx.compose.foundation.layout.Column {
                TacticalTopAppBar(
                    isScanningOrHunting = isHunting,
                    isAudioPlaying = isAudioPlaying,
                    onToggleAudio = { viewModel.toggleAudioDemod() },
                    activeThreatCount = activeAlertsCount
                )
                PersistentLocationBar(
                    deviceLocation = deviceLocation,
                    onRefreshLocation = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        viewModel.refreshRealTelemetry()
                    },
                    onSetCustomLocation = { lat, lon, name ->
                        viewModel.setCustomLocation(lat, lon, name)
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                contentColor = TextPrimary,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(1.dp, CyberBorder)
                    .testTag("bottom_navigation_bar")
            ) {
                val navItems = listOf(
                    Triple("Bug Hunt", Icons.Filled.Security, Icons.Outlined.Security),
                    Triple("Spectrum", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
                    Triple("Tactical Map", Icons.Filled.Radar, Icons.Outlined.Radar),
                    Triple("Archive", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
                    Triple("Evidence", Icons.Filled.Sensors, Icons.Outlined.Sensors)
                )

                navItems.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setNavTab(index) },
                        icon = {
                            if (index == 1 && activeAlertsCount > 0) {
                                androidx.compose.material3.BadgedBox(
                                    badge = {
                                        androidx.compose.material3.Badge(
                                            containerColor = NeonRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = "$activeAlertsCount",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                        contentDescription = label,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                    contentDescription = label,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = CyberSurfaceElevated,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBg)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> BugHuntAgentScreen(
                    presets = presets,
                    selectedPreset = selectedPreset,
                    currentCode = currentCode,
                    targetName = targetName,
                    isHunting = isHunting,
                    agentThoughts = agentThoughts,
                    consensusReport = consensusReport,
                    isSaved = isSaved,
                    onSelectPreset = { viewModel.selectPreset(it) },
                    onCodeChange = { viewModel.updateCode(it) },
                    onTargetNameChange = { viewModel.updateTargetName(it) },
                    onRunParallelHunt = { viewModel.runParallelBugHunt() },
                    onSaveReport = { viewModel.saveCurrentReport(it) },
                    onLocateFindingOnMap = { viewModel.focusFindingOnMap(it) },
                    environmentMode = environmentMode,
                    deviceLocation = deviceLocation,
                    onToggleEnvironmentMode = { viewModel.setEnvironmentMode(it) }
                )
                1 -> SpectrumAnalyzerScreen(
                    signals = signals,
                    selectedSignal = selectedSignal,
                    isAudioPlaying = isAudioPlaying,
                    currentToneHz = currentToneHz,
                    audioVolume = audioVolume,
                    currentModulationType = currentModulationType,
                    audioWaveform = audioWaveform,
                    isVoiceRecording = isVoiceRecording,
                    voiceRmsDb = voiceRmsDb,
                    voicePeakFreqHz = voicePeakFreqHz,
                    voiceLiveWaveform = voiceLiveWaveform,
                    isSpeechDetected = isSpeechDetected,
                    capturedVoiceTranscripts = capturedVoiceTranscripts,
                    threatAlerts = threatAlerts,
                    threatSignatures = threatSignatures,
                    activeAlertsCount = activeAlertsCount,
                    isThreatScanning = isThreatScanning,
                    isThreatSentinelActive = isThreatSentinelActive,
                    selectedThreatAlert = selectedThreatAlert,
                    onSelectSignal = { viewModel.selectSignal(it) },
                    onToggleAudioDemod = { viewModel.toggleAudioDemod(it) },
                    onSetAudioVolume = { viewModel.setAudioVolume(it) },
                    onSetModulationType = { viewModel.setAudioModulationType(it) },
                    onUpdateAudioTone = { viewModel.updateAudioTone(it) },
                    onToggleVoiceCapture = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPerm) {
                            viewModel.toggleVoiceCapture()
                        } else {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onAddManualVoiceNote = { viewModel.addManualVoiceNote(it) },
                    onClearVoiceTranscripts = { viewModel.clearVoiceTranscripts() },
                    onRunThreatScan = { viewModel.runThreatScan() },
                    onToggleThreatSentinel = { viewModel.toggleThreatSentinel() },
                    onSelectThreatAlert = { viewModel.selectThreatAlert(it) },
                    onUpdateThreatAlertStatus = { id, status -> viewModel.updateThreatAlertStatus(id, status) },
                    onDismissThreatAlert = { viewModel.dismissThreatAlert(it) },
                    onDeleteThreatAlert = { viewModel.deleteThreatAlert(it) },
                    onClearAllThreatAlerts = { viewModel.clearAllThreatAlerts() },
                    onToggleSignatureEnabled = { sigId, isEnabled -> viewModel.toggleSignatureEnabled(sigId, isEnabled) },
                    onAddCustomSignature = { viewModel.addCustomThreatSignature(it) },
                    onDeleteSignature = { viewModel.deleteThreatSignature(it) },
                    onFocusAlertSignalOnMap = { alert ->
                        val matchedSignal = signals.find { sig ->
                            sig.frequencyMhz == alert.signalFrequencyMhz ||
                            kotlin.math.abs(sig.frequencyMhz - alert.signalFrequencyMhz) < 0.2
                        }
                        if (matchedSignal != null) {
                            viewModel.focusSignalOnMap(matchedSignal)
                        }
                        viewModel.setNavTab(2) // Switch to Tactical Map screen
                    }
                )
                2 -> TacticalMapScreen(
                    signals = signals,
                    findings = findings,
                    selectedTarget = selectedMapTarget,
                    onSelectTarget = { viewModel.selectMapTarget(it) },
                    selectedSignal = selectedSignal,
                    onSelectSignal = { viewModel.focusSignalOnMap(it) },
                    environmentMode = environmentMode,
                    deviceLocation = deviceLocation,
                    onToggleEnvironmentMode = { viewModel.setEnvironmentMode(it) },
                    onRefreshLocation = { viewModel.refreshRealTelemetry() },
                    onSearchAddress = { viewModel.searchAddress(it) }
                )
                3 -> AuditHistoryScreen(
                    savedReports = savedReports,
                    interceptedSignals = interceptedSignals,
                    onDeleteReport = { viewModel.deleteReport(it) },
                    onClearAllReports = { viewModel.clearAllReports() },
                    onDeleteSignal = { viewModel.deleteSignal(it) },
                    onClearAllSignals = { viewModel.clearAllSignals() }
                )
                4 -> PhoneEvidenceScreen(
                    deviceLocation = deviceLocation
                )
            }
        }
    }
}
