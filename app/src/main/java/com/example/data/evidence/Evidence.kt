package com.example.data.evidence

data class EvidenceDraft(
    val sourceType: String,
    val classification: EvidenceClassification,
    val title: String,
    val payload: String,
    val locationLabel: String? = null
)
