package com.example.data.evidence

import java.security.MessageDigest

object EvidenceHasher {
    fun sha256(canonical: String): String =
        MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun canonical(sourceType: String, classification: EvidenceClassification, title: String,
                  payload: String, locationLabel: String?, createdAt: Long, previousHash: String?) =
        listOf(sourceType, classification.name, title, payload, locationLabel.orEmpty(),
            createdAt.toString(), previousHash.orEmpty()).joinToString("|")
}
