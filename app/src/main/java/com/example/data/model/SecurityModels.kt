package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AgentRole(
    val title: String,
    val description: String,
    val badgeColor: Long,
    val iconName: String
) {
    SAST_SENTINEL(
        "Agent Sentinel",
        "SAST & Static Vulnerability Auditor",
        0xFF00E5FF,
        "Security"
    ),
    CIPHER_INSPECTOR(
        "Agent Cipher",
        "Cryptographic & Key Exchange Auditor",
        0xFFA855F7,
        "Key"
    ),
    SPECTRUM_ANALYST(
        "Agent Evidence",
        "Sensor, measurement, and provenance analyst",
        0xFF00FF9D,
        "SettingsInputAntenna"
    ),
    THREAT_MODELER(
        "Agent Consensus",
        "Threat Modeler & Patch Synthesizer",
        0xFFFF9100,
        "Psychology"
    )
}

enum class AgentStatus {
    IDLE,
    SCANNING,
    THINKING,
    CORRELATING,
    CONSENSUS_REACHED,
    FAILED
}

data class AgentThought(
    val agentRole: AgentRole,
    val status: AgentStatus,
    val thoughtText: String,
    val confidence: Float,
    val timestampMs: Long = System.currentTimeMillis(),
    val findingSnippet: String? = null
)

enum class VulnerabilitySeverity(val label: String, val colorHex: Long, val baseScore: Float) {
    CRITICAL("CRITICAL", 0xFFFF1744, 9.6f),
    HIGH("HIGH", 0xFFFF5252, 7.8f),
    MEDIUM("MEDIUM", 0xFFFFB300, 5.4f),
    LOW("LOW", 0xFF00E676, 3.1f),
    INFORMATIONAL("INFO", 0xFF00B0FF, 1.0f)
}

data class VulnerabilityFinding(
    val id: String,
    val title: String,
    val cweId: String,
    val cvssScore: Float,
    val severity: VulnerabilitySeverity,
    val affectedComponent: String,
    val description: String,
    val exploitScenario: String,
    val remediation: String,
    val vulnerableCodeSnippet: String,
    val patchedCodeSnippet: String,
    val discoveredBy: AgentRole,
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val locationName: String = "Tactical Cyber Range Alpha",
    val locationAddress: String = "San Francisco, CA",
    val elevationMeters: Int = 38
)

data class ConsensusReport(
    val targetName: String,
    val targetType: String,
    val overallScore: Float,
    val overallSeverity: VulnerabilitySeverity,
    val consensusSummary: String,
    val keyTakeaways: List<String>,
    val findings: List<VulnerabilityFinding>,
    val generatedRemediationPatch: String,
    val agentConfidenceMatrix: Map<AgentRole, Float>,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class TargetPreset(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val sampleCodeOrTelemetry: String
)

data class RfSignalInfo(
    val id: String,
    val frequencyMhz: Double,
    val bandName: String,
    val modulationType: String,
    val powerDbm: Float,
    val snrDb: Float,
    val bandwidthKhz: Float,
    val isRogue: Boolean,
    val azimuthDegrees: Float,
    val estimatedDistanceM: Float,
    val latitudeOffset: Float,
    val longitudeOffset: Float,
    val protocolName: String,
    val demodulatedText: String,
    val rawHexPayload: String,
    val audioToneFrequencyHz: Int,
    val latitude: Double = 37.7833,
    val longitude: Double = -122.4167,
    val locationName: String = "Microwave Uplink Mast",
    val locationAddress: String = "SOMA Tactical Sector, SF",
    val elevationMeters: Int = 54,
    val description: String = "",
    val analystAssessment: String = "",
    val threatRating: String = "LOW",
    val encryptionState: String = "Cleartext",
    val transmissionMode: String = "Continuous",
    val spectralPurityPercent: Float = 96.5f,
    val recommendedAction: String = "Passive monitoring"
)

enum class MapDisplayMode(val label: String) {
    REAL_MAP("Real Geographic Map"),
    TACTICAL_RADAR("Evidence Analysis Overlay")
}

enum class TacticalEnvironmentMode(val label: String, val badge: String, val description: String) {
    REAL_DEVICE(
        "Real Device Location & Hardware",
        "LIVE GPS",
        "Uses measurements and observations actually reported by the Android device; unavailable values remain unavailable."
    ),
    SAN_FRANCISCO_RANGE(
        "San Francisco Cyber-Range",
        "TESTBED",
        "Explicit simulated testbed for development only; results are not field measurements."
    )
}

data class DeviceLocationInfo(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val provider: String = "GPS",
    val locationName: String = "Device Real GPS Fix",
    val locationAddress: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
    val isRealHardwareFix: Boolean = false
)

data class RealWifiTelemetry(
    val isConnected: Boolean,
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val linkSpeedMbps: Int,
    val securityType: String,
    val ipAddress: String,
    val isRealHardware: Boolean = true
)

sealed class MapTargetItem {
    abstract val id: String
    abstract val title: String
    abstract val subtitle: String
    abstract val latitude: Double
    abstract val longitude: Double
    abstract val locationName: String
    abstract val locationAddress: String
    abstract val isThreat: Boolean
    abstract val badgeLabel: String
    abstract val elevationMeters: Int

    data class FindingTarget(val finding: VulnerabilityFinding) : MapTargetItem() {
        override val id: String get() = finding.id
        override val title: String get() = finding.title
        override val subtitle: String get() = "CWE: ${finding.cweId} | CVSS ${finding.cvssScore}"
        override val latitude: Double get() = finding.latitude
        override val longitude: Double get() = finding.longitude
        override val locationName: String get() = finding.locationName
        override val locationAddress: String get() = finding.locationAddress
        override val isThreat: Boolean get() = finding.severity == VulnerabilitySeverity.CRITICAL || finding.severity == VulnerabilitySeverity.HIGH
        override val badgeLabel: String get() = finding.severity.name
        override val elevationMeters: Int get() = finding.elevationMeters
    }

    data class SignalTarget(val signal: RfSignalInfo) : MapTargetItem() {
        override val id: String get() = signal.id
        override val title: String get() = "${signal.bandName} (${signal.frequencyMhz} MHz)"
        override val subtitle: String get() = "Mod: ${signal.modulationType} | Az: ${signal.azimuthDegrees.toInt()}° | Dist: ${signal.estimatedDistanceM.toInt()}m"
        override val latitude: Double get() = signal.latitude
        override val longitude: Double get() = signal.longitude
        override val locationName: String get() = signal.locationName
        override val locationAddress: String get() = signal.locationAddress
        override val isThreat: Boolean get() = signal.isRogue
        override val badgeLabel: String get() = if (signal.isRogue) "ROGUE EMITTER" else "AUTHORIZED"
        override val elevationMeters: Int get() = signal.elevationMeters
    }
}
