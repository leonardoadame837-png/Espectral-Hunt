package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.SignalAudioSynthesizer
import com.example.data.engine.MultiAgentSecurityEngine
import com.example.data.hardware.RealDeviceHardwareManager
import com.example.data.hardware.VoiceAudioCapturer
import com.example.data.local.AuditReportEntity
import com.example.data.local.InterceptedSignalEntity
import com.example.data.local.SpectraDatabase
import com.example.data.local.ThreatAlertEntity
import com.example.data.local.ThreatSignatureEntity
import com.example.data.threat.ThreatDetectionEngine
import com.example.data.model.AgentThought
import com.example.data.model.ConsensusReport
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.MapTargetItem
import com.example.data.model.RealWifiTelemetry
import com.example.data.model.RfSignalInfo
import com.example.data.model.TacticalEnvironmentMode
import com.example.data.model.TargetPreset
import com.example.data.model.VulnerabilityFinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpectraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SpectraDatabase.getInstance(application)
    private val dao = db.spectraDao()
    private val securityEngine = MultiAgentSecurityEngine()
    private val hardwareManager = RealDeviceHardwareManager(application)
    private val audioSynthesizer = SignalAudioSynthesizer(viewModelScope)
    private val voiceCapturer = VoiceAudioCapturer(application)
    private val threatEngine = ThreatDetectionEngine()

    // Voice Capture State
    val isVoiceRecording: StateFlow<Boolean> = voiceCapturer.isRecording
    val voiceRmsDb: StateFlow<Float> = voiceCapturer.rmsDb
    val voicePeakFreqHz: StateFlow<Int> = voiceCapturer.peakFreqHz
    val voiceLiveWaveform: StateFlow<FloatArray> = voiceCapturer.liveVoiceWaveform
    val isSpeechDetected: StateFlow<Boolean> = voiceCapturer.isSpeechDetected
    val capturedVoiceTranscripts = voiceCapturer.capturedTranscripts

    // Environment Mode: REAL_DEVICE vs SAN_FRANCISCO_RANGE
    private val _environmentMode = MutableStateFlow(TacticalEnvironmentMode.REAL_DEVICE)
    val environmentMode: StateFlow<TacticalEnvironmentMode> = _environmentMode.asStateFlow()

    // Real Hardware Telemetry
    private val _deviceLocation = MutableStateFlow(
        DeviceLocationInfo(
            latitude = 37.7749,
            longitude = -122.4194,
            altitudeMeters = 30.0,
            accuracyMeters = 0f,
            provider = "ACQUIRING",
            locationName = "Hardware GPS Sensor",
            locationAddress = "Querying live GNSS & network providers...",
            isRealHardwareFix = false
        )
    )
    val deviceLocation: StateFlow<DeviceLocationInfo> = _deviceLocation.asStateFlow()

    private val _realWifiTelemetry = MutableStateFlow<RealWifiTelemetry?>(hardwareManager.getRealWifiTelemetry())
    val realWifiTelemetry: StateFlow<RealWifiTelemetry?> = _realWifiTelemetry.asStateFlow()

    val presets: List<TargetPreset> = securityEngine.targetPresets

    private val _selectedPreset = MutableStateFlow<TargetPreset?>(presets.firstOrNull())
    val selectedPreset: StateFlow<TargetPreset?> = _selectedPreset.asStateFlow()

    private val _targetName = MutableStateFlow(presets.firstOrNull()?.name ?: "Microwave Receiver v4.2")
    val targetName: StateFlow<String> = _targetName.asStateFlow()

    private val _currentCode = MutableStateFlow(presets.firstOrNull()?.sampleCodeOrTelemetry ?: "")
    val currentCode: StateFlow<String> = _currentCode.asStateFlow()

    private val _isHunting = MutableStateFlow(false)
    val isHunting: StateFlow<Boolean> = _isHunting.asStateFlow()

    private val _agentThoughts = MutableStateFlow<List<AgentThought>>(emptyList())
    val agentThoughts: StateFlow<List<AgentThought>> = _agentThoughts.asStateFlow()

    private val _consensusReport = MutableStateFlow<ConsensusReport?>(null)
    val consensusReport: StateFlow<ConsensusReport?> = _consensusReport.asStateFlow()

    private val _isCurrentReportSaved = MutableStateFlow(false)
    val isCurrentReportSaved: StateFlow<Boolean> = _isCurrentReportSaved.asStateFlow()

    // Vulnerability Findings from parallel agents
    private val _findings = MutableStateFlow<List<VulnerabilityFinding>>(securityEngine.defaultFindings)
    val findings: StateFlow<List<VulnerabilityFinding>> = _findings.asStateFlow()

    // Signals & Spectrum
    private val _signals = MutableStateFlow<List<RfSignalInfo>>(securityEngine.defaultSignals)
    val signals: StateFlow<List<RfSignalInfo>> = _signals.asStateFlow()

    private val _selectedSignal = MutableStateFlow<RfSignalInfo?>(_signals.value.firstOrNull())
    val selectedSignal: StateFlow<RfSignalInfo?> = _selectedSignal.asStateFlow()

    // Unified Selected Map Target (Finding or Signal)
    private val _selectedMapTarget = MutableStateFlow<MapTargetItem?>(
        securityEngine.defaultFindings.firstOrNull()?.let { MapTargetItem.FindingTarget(it) }
    )
    val selectedMapTarget: StateFlow<MapTargetItem?> = _selectedMapTarget.asStateFlow()

    val isAudioPlaying: StateFlow<Boolean> = audioSynthesizer.isPlaying
    val currentAudioToneHz: StateFlow<Int> = audioSynthesizer.currentSignalTone
    val currentModulationType: StateFlow<String> = audioSynthesizer.currentModulationType
    val audioVolume: StateFlow<Float> = audioSynthesizer.volume
    val audioWaveform: StateFlow<FloatArray> = audioSynthesizer.waveformSamples

    // Room Database Flows
    val savedReports: StateFlow<List<AuditReportEntity>> = dao.getAllAuditReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val interceptedSignals: StateFlow<List<InterceptedSignalEntity>> = dao.getAllInterceptedSignals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Threat Detection System Flows
    val threatSignatures: StateFlow<List<ThreatSignatureEntity>> = dao.getAllThreatSignatures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val threatAlerts: StateFlow<List<ThreatAlertEntity>> = dao.getAllThreatAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAlertsCount: StateFlow<Int> = dao.getActiveThreatAlerts()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isThreatScanning = MutableStateFlow(false)
    val isThreatScanning: StateFlow<Boolean> = _isThreatScanning.asStateFlow()

    private val _isThreatSentinelActive = MutableStateFlow(true)
    val isThreatSentinelActive: StateFlow<Boolean> = _isThreatSentinelActive.asStateFlow()

    private val _selectedThreatAlert = MutableStateFlow<ThreatAlertEntity?>(null)
    val selectedThreatAlert: StateFlow<ThreatAlertEntity?> = _selectedThreatAlert.asStateFlow()

    // Active bottom navigation tab index (0 = Bug Hunt, 1 = Spectrum, 2 = Tactical Map, 3 = History)
    private val _selectedNavTab = MutableStateFlow(0)
    val selectedNavTab: StateFlow<Int> = _selectedNavTab.asStateFlow()

    init {
        // Auto initialize environment telemetry
        refreshRealTelemetry()
        startContinuousLocationTracking()

        // Auto seed initial intercepted signals & threat signatures to Room if empty
        viewModelScope.launch {
            _signals.value.forEach { sig ->
                dao.insertInterceptedSignal(
                    InterceptedSignalEntity(
                        signalIdentifier = sig.id,
                        frequencyMhz = sig.frequencyMhz,
                        bandName = sig.bandName,
                        modulationType = sig.modulationType,
                        powerDbm = sig.powerDbm,
                        isRogue = sig.isRogue,
                        azimuthDegrees = sig.azimuthDegrees,
                        distanceMeters = sig.estimatedDistanceM,
                        protocolName = sig.protocolName,
                        decodedPayload = sig.demodulatedText,
                        hexData = sig.rawHexPayload
                    )
                )
            }

            // Seed Malicious Signatures Database
            val sigCount = dao.getSignatureCount()
            if (sigCount == 0) {
                dao.insertThreatSignatures(threatEngine.defaultMaliciousSignatures)
            }

            // Run initial threat evaluation
            val activeSignatures = if (sigCount == 0) {
                threatEngine.defaultMaliciousSignatures
            } else {
                dao.getEnabledThreatSignaturesSync()
            }
            val initialAlerts = threatEngine.evaluateAllSignals(_signals.value, activeSignatures)
            initialAlerts.forEach { alert ->
                dao.insertThreatAlert(alert)
            }
        }
    }

    fun refreshRealTelemetry() {
        viewModelScope.launch {
            val loc = hardwareManager.getRealDeviceLocation()
            val wifi = hardwareManager.getRealWifiTelemetry()
            _deviceLocation.value = loc
            _realWifiTelemetry.value = wifi

            if (_environmentMode.value == TacticalEnvironmentMode.REAL_DEVICE) {
                applyLocationToEnvironment(loc)
            }
        }
    }

    fun startContinuousLocationTracking() {
        hardwareManager.startContinuousLocationUpdates(viewModelScope) { newLoc ->
            _deviceLocation.value = newLoc
            if (_environmentMode.value == TacticalEnvironmentMode.REAL_DEVICE) {
                applyLocationToEnvironment(newLoc)
            }
        }
    }

    private fun applyLocationToEnvironment(loc: DeviceLocationInfo) {
        val realWifiSig = hardwareManager.createRealWifiSignal(loc)
        val dynamicSignals = securityEngine.getSignalsForEnvironment(
            environmentMode = TacticalEnvironmentMode.REAL_DEVICE,
            deviceLocation = loc,
            realWifiSignal = realWifiSig
        )
        _signals.value = dynamicSignals
        if (_selectedSignal.value == null || !_signals.value.any { it.id == _selectedSignal.value?.id }) {
            _selectedSignal.value = dynamicSignals.firstOrNull()
        }

        val dynamicFindings = securityEngine.getFindingsForEnvironment(
            environmentMode = TacticalEnvironmentMode.REAL_DEVICE,
            deviceLocation = loc
        )
        _findings.value = dynamicFindings

        if (_isThreatSentinelActive.value) {
            evaluateSignalsForThreats(dynamicSignals)
        }
    }

    fun setCustomLocation(latitude: Double, longitude: Double, name: String) {
        viewModelScope.launch {
            val customLoc = hardwareManager.createCustomLocation(latitude, longitude, name)
            _deviceLocation.value = customLoc
            if (_environmentMode.value == TacticalEnvironmentMode.REAL_DEVICE) {
                applyLocationToEnvironment(customLoc)
            }
        }
    }

    fun searchAddress(query: String) {
        viewModelScope.launch {
            val searchedLoc = hardwareManager.searchAddressLocation(query)
            _deviceLocation.value = searchedLoc
            if (_environmentMode.value == TacticalEnvironmentMode.REAL_DEVICE) {
                applyLocationToEnvironment(searchedLoc)
            }
        }
    }

    fun setEnvironmentMode(mode: TacticalEnvironmentMode) {
        _environmentMode.value = mode
        if (mode == TacticalEnvironmentMode.REAL_DEVICE) {
            refreshRealTelemetry()
        } else {
            _signals.value = securityEngine.defaultSignals
            _findings.value = securityEngine.defaultFindings
            _selectedSignal.value = securityEngine.defaultSignals.firstOrNull()
            _selectedMapTarget.value = securityEngine.defaultFindings.firstOrNull()?.let { MapTargetItem.FindingTarget(it) }
            if (_isThreatSentinelActive.value) {
                evaluateSignalsForThreats(securityEngine.defaultSignals)
            }
        }
    }

    fun selectPreset(preset: TargetPreset) {
        _selectedPreset.value = preset
        _targetName.value = preset.name
        _currentCode.value = preset.sampleCodeOrTelemetry
        _consensusReport.value = null
        _agentThoughts.value = emptyList()
        _isCurrentReportSaved.value = false
    }

    fun updateCode(newCode: String) {
        _currentCode.value = newCode
    }

    fun updateTargetName(newName: String) {
        _targetName.value = newName
    }

    fun setNavTab(index: Int) {
        _selectedNavTab.value = index
    }

    fun selectSignal(signal: RfSignalInfo) {
        val previous = _selectedSignal.value
        _selectedSignal.value = signal
        if (isAudioPlaying.value) {
            if (previous?.id != signal.id) {
                audioSynthesizer.startDemodAudio(signal.audioToneFrequencyHz, signal.modulationType)
            } else {
                audioSynthesizer.updateTone(signal.audioToneFrequencyHz)
                audioSynthesizer.updateModulationType(signal.modulationType)
            }
        }
    }

    fun toggleAudioDemod(signal: RfSignalInfo? = null) {
        val targetSig = signal ?: _selectedSignal.value ?: _signals.value.firstOrNull() ?: return
        val currentSig = _selectedSignal.value

        if (isAudioPlaying.value) {
            if (signal != null && currentSig != null && signal.id != currentSig.id) {
                // Switch to different signal's audio tone
                _selectedSignal.value = signal
                audioSynthesizer.startDemodAudio(signal.audioToneFrequencyHz, signal.modulationType)
            } else {
                // Toggle off
                audioSynthesizer.stopDemodAudio()
            }
        } else {
            _selectedSignal.value = targetSig
            audioSynthesizer.startDemodAudio(targetSig.audioToneFrequencyHz, targetSig.modulationType)
        }
    }

    fun setAudioVolume(vol: Float) {
        audioSynthesizer.setVolume(vol)
    }

    fun setAudioModulationType(modType: String) {
        audioSynthesizer.updateModulationType(modType)
    }

    fun updateAudioTone(toneHz: Int) {
        audioSynthesizer.updateTone(toneHz)
    }

    // Voice Microphone Capture Controls
    fun startVoiceCapture() {
        voiceCapturer.startVoiceCapture(viewModelScope)
    }

    fun stopVoiceCapture() {
        voiceCapturer.stopVoiceCapture()
    }

    fun toggleVoiceCapture() {
        if (isVoiceRecording.value) {
            stopVoiceCapture()
        } else {
            startVoiceCapture()
        }
    }

    fun addManualVoiceNote(text: String) {
        voiceCapturer.addTranscript(text, fromMicrophone = false)
    }

    fun clearVoiceTranscripts() {
        voiceCapturer.clearVoiceTranscripts()
    }

    fun selectMapTarget(target: MapTargetItem) {
        _selectedMapTarget.value = target
        if (target is MapTargetItem.SignalTarget) {
            _selectedSignal.value = target.signal
        }
    }

    fun focusFindingOnMap(finding: VulnerabilityFinding) {
        _selectedMapTarget.value = MapTargetItem.FindingTarget(finding)
        _selectedNavTab.value = 2 // Switch to Tactical Map
    }

    fun focusSignalOnMap(signal: RfSignalInfo) {
        _selectedSignal.value = signal
        _selectedMapTarget.value = MapTargetItem.SignalTarget(signal)
        _selectedNavTab.value = 2 // Switch to Tactical Map
    }

    fun runParallelBugHunt() {
        if (_isHunting.value) return
        _isHunting.value = true
        _agentThoughts.value = emptyList()
        _consensusReport.value = null
        _isCurrentReportSaved.value = false

        viewModelScope.launch {
            try {
                val report = securityEngine.executeParallelBugHunt(
                    targetName = _targetName.value,
                    targetType = _selectedPreset.value?.category ?: "Custom RF/Firmware Binary",
                    codeOrPayload = _currentCode.value,
                    environmentMode = _environmentMode.value,
                    deviceLocation = _deviceLocation.value,
                    onAgentThought = { thought ->
                        _agentThoughts.value = _agentThoughts.value + thought
                    }
                )
                _consensusReport.value = report
                if (report.findings.isNotEmpty()) {
                    _findings.value = report.findings
                    _selectedMapTarget.value = MapTargetItem.FindingTarget(report.findings.first())
                }
            } catch (e: Exception) {
                // Graceful handling
            } finally {
                _isHunting.value = false
            }
        }
    }

    fun saveCurrentReport(report: ConsensusReport) {
        viewModelScope.launch {
            dao.insertAuditReport(
                AuditReportEntity(
                    targetName = report.targetName,
                    targetType = report.targetType,
                    overallScore = report.overallScore,
                    overallSeverity = report.overallSeverity.name,
                    consensusSummary = report.consensusSummary,
                    findingsCount = report.findings.size,
                    generatedPatch = report.generatedRemediationPatch,
                    rawFindingsJson = ""
                )
            )
            _isCurrentReportSaved.value = true
        }
    }

    fun deleteReport(id: Long) {
        viewModelScope.launch {
            dao.deleteAuditReport(id)
        }
    }

    fun clearAllReports() {
        viewModelScope.launch {
            dao.clearAllAuditReports()
        }
    }

    fun deleteSignal(id: Long) {
        viewModelScope.launch {
            dao.deleteInterceptedSignal(id)
        }
    }

    fun clearAllSignals() {
        viewModelScope.launch {
            dao.clearAllSignals()
        }
    }

    // ==========================================
    // THREAT DETECTION SYSTEM & MALICIOUS DB
    // ==========================================

    private fun evaluateSignalsForThreats(signalsList: List<RfSignalInfo>) {
        viewModelScope.launch {
            val enabledSignatures = dao.getEnabledThreatSignaturesSync()
            val detectedAlerts = threatEngine.evaluateAllSignals(signalsList, enabledSignatures)
            for (alert in detectedAlerts) {
                dao.insertThreatAlert(alert)
            }
        }
    }

    fun runThreatScan() {
        viewModelScope.launch {
            _isThreatScanning.value = true
            kotlinx.coroutines.delay(650) // scanning animation delay
            val enabledSignatures = dao.getEnabledThreatSignaturesSync()
            val detectedAlerts = threatEngine.evaluateAllSignals(_signals.value, enabledSignatures)
            for (alert in detectedAlerts) {
                dao.insertThreatAlert(alert)
            }
            _isThreatScanning.value = false
        }
    }

    fun toggleThreatSentinel() {
        _isThreatSentinelActive.value = !_isThreatSentinelActive.value
        if (_isThreatSentinelActive.value) {
            runThreatScan()
        }
    }

    fun selectThreatAlert(alert: ThreatAlertEntity?) {
        _selectedThreatAlert.value = alert
    }

    fun updateThreatAlertStatus(alertId: Long, newStatus: String) {
        viewModelScope.launch {
            dao.updateThreatAlertStatus(alertId, newStatus)
            if (_selectedThreatAlert.value?.id == alertId) {
                _selectedThreatAlert.value = _selectedThreatAlert.value?.copy(status = newStatus)
            }
        }
    }

    fun dismissThreatAlert(alertId: Long) {
        viewModelScope.launch {
            dao.updateThreatAlertStatus(alertId, "DISMISSED")
            if (_selectedThreatAlert.value?.id == alertId) {
                _selectedThreatAlert.value = null
            }
        }
    }

    fun deleteThreatAlert(alertId: Long) {
        viewModelScope.launch {
            dao.deleteThreatAlert(alertId)
            if (_selectedThreatAlert.value?.id == alertId) {
                _selectedThreatAlert.value = null
            }
        }
    }

    fun clearAllThreatAlerts() {
        viewModelScope.launch {
            dao.clearAllThreatAlerts()
            _selectedThreatAlert.value = null
        }
    }

    fun toggleSignatureEnabled(signatureId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            dao.toggleSignatureEnabled(signatureId, isEnabled)
        }
    }

    fun addCustomThreatSignature(signature: ThreatSignatureEntity) {
        viewModelScope.launch {
            dao.insertThreatSignature(signature)
        }
    }

    fun deleteThreatSignature(signatureId: String) {
        viewModelScope.launch {
            dao.deleteThreatSignature(signatureId)
        }
    }

    fun focusAlertSignal(alert: ThreatAlertEntity) {
        val matchingSignal = _signals.value.firstOrNull { it.id == alert.signalId }
        if (matchingSignal != null) {
            selectSignal(matchingSignal)
            _selectedMapTarget.value = MapTargetItem.SignalTarget(matchingSignal)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioSynthesizer.stopDemodAudio()
        voiceCapturer.stopVoiceCapture()
        hardwareManager.stopLocationUpdates()
    }
}
