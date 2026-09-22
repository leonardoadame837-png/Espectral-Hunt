package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_reports")
data class AuditReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetName: String,
    val targetType: String,
    val overallScore: Float,
    val overallSeverity: String,
    val consensusSummary: String,
    val findingsCount: Int,
    val generatedPatch: String,
    val rawFindingsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "intercepted_signals")
data class InterceptedSignalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val signalIdentifier: String,
    val frequencyMhz: Double,
    val bandName: String,
    val modulationType: String,
    val powerDbm: Float,
    val isRogue: Boolean,
    val azimuthDegrees: Float,
    val distanceMeters: Float,
    val protocolName: String,
    val decodedPayload: String,
    val hexData: String,
    val capturedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "threat_signatures")
data class ThreatSignatureEntity(
    @PrimaryKey
    val id: String, // e.g. "SIG-MICROWAVE-001"
    val name: String,
    val category: String, // e.g. "Microwave Uplink", "Cellular IMSI Catcher", "RF Jammer"
    val threatLevel: String, // "CRITICAL", "HIGH", "MEDIUM", "LOW"
    val targetBand: String, // "5.8 GHz Microwave", "Sub-GHz ISM", etc.
    val minFrequencyMhz: Double,
    val maxFrequencyMhz: Double,
    val modulationPattern: String, // "Chirp", "FSK", "Microwave Relay", "ALL", etc.
    val signaturePatternHex: String, // Hex token to match or empty
    val payloadRegex: String = "", // String regex to match in demodulated text
    val spectralAnomalyTrigger: String, // Anomaly rule description
    val behavioralAnomalyRules: String,
    val technicalDetails: String,
    val recommendedCountermeasure: String,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "threat_alerts")
data class ThreatAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alertId: String, // e.g. "ALT-5842-ROGUE"
    val signalId: String,
    val signalFrequencyMhz: Double,
    val signalBandName: String,
    val signalProtocol: String,
    val signalPowerDbm: Float,
    val signalDistanceM: Float,
    val signalAzimuthDeg: Float,
    val locationAddress: String,
    val matchedSignatureId: String? = null,
    val matchedSignatureName: String,
    val threatLevel: String, // "CRITICAL", "HIGH", "MEDIUM", "LOW"
    val detectionType: String, // "SIGNATURE_MATCH", "BEHAVIORAL_ANOMALY", "HYBRID_DETECTION"
    val anomalyIndicators: String, // Semi-colon separated list of anomalies
    val confidencePercent: Float, // 0-100%
    val technicalSummary: String,
    val recommendedCountermeasure: String,
    val status: String = "ACTIVE", // "ACTIVE", "INVESTIGATING", "MITIGATED", "DISMISSED"
    val timestamp: Long = System.currentTimeMillis()
)

