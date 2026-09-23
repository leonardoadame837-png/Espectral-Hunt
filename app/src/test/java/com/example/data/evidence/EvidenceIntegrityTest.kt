package com.example.data.evidence

import org.junit.Assert.assertEquals
import org.junit.Test

class EvidenceIntegrityTest {
    @Test
    fun sha256_is_deterministic() {
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            EvidenceIntegrity.sha256("hello")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun finding_without_evidence_is_rejected() {
        EvidenceIntegrity.requireSupportingEvidence(
            AgentFinding(
                findingId = "finding-1",
                classification = EvidenceClassification.AI_INTERPRETATION,
                statement = "Unsupported claim",
                supportingEvidenceIds = emptyList(),
                limitations = listOf("No measurement supplied")
            ),
            emptyMap()
        )
    }
}
