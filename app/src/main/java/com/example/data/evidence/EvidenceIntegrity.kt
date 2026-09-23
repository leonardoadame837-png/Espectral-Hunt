package com.example.data.evidence

import java.security.MessageDigest

object EvidenceIntegrity {
    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    fun sha256(text: String): String = sha256(text.toByteArray(Charsets.UTF_8))

    fun requireSupportingEvidence(
        finding: AgentFinding,
        evidence: Map<String, EvidenceRecord>
    ) {
        require(finding.supportingEvidenceIds.isNotEmpty()) {
            "AI finding " + finding.findingId + " has no supporting evidence."
        }
        finding.supportingEvidenceIds.forEach { id ->
            require(evidence.containsKey(id)) {
                "AI finding " + finding.findingId + " references missing evidence " + id + "."
            }
        }
    }
}
