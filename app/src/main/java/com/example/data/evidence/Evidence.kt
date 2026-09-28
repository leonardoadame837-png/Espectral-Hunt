package com.example.data.evidence

enum class EvidenceClassification { MEASURED, CALCULATED, AI_INTERPRETATION, UNVERIFIED_HYPOTHESIS }

data class EvidenceDraft(
    val sourceType: String,
    val classification: EvidenceClassification,
    val title: String,
    val payload: String,
    val locationLabel: String? = null
)
