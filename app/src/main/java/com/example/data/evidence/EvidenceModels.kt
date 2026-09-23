package com.example.data.evidence

/**
 * Classification used by the investigation agent.
 *
 * MEASURED = directly recorded by a supported Android source.
 * CALCULATED = deterministic computation from recorded evidence.
 * AI_INTERPRETATION = model interpretation that must cite evidence IDs.
 * UNVERIFIED_HYPOTHESIS = possible explanation requiring additional evidence.
 */
enum class EvidenceClassification {
    MEASURED,
    CALCULATED,
    AI_INTERPRETATION,
    UNVERIFIED_HYPOTHESIS
}

enum class EvidenceSourceType {
    ANDROID_WIFI,
    ANDROID_BLUETOOTH,
    MICROPHONE,
    GPS,
    MOTION_SENSOR,
    CAMERA,
    IMPORTED_FILE,
    SIMULATED,
    UNKNOWN
}

data class EvidenceRecord(
    val evidenceId: String,
    val sourceType: EvidenceSourceType,
    val classification: EvidenceClassification,
    val collectedAtEpochMs: Long,
    val payloadSha256: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val description: String,
    val parentEvidenceIds: List<String> = emptyList()
)

data class AgentFinding(
    val findingId: String,
    val classification: EvidenceClassification,
    val statement: String,
    val supportingEvidenceIds: List<String>,
    val limitations: List<String>,
    val generatedAtEpochMs: Long = System.currentTimeMillis()
)
