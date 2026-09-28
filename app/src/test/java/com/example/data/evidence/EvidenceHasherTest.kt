package com.example.data.evidence

import org.junit.Assert.assertEquals
import org.junit.Test

class EvidenceHasherTest {
    @Test
    fun sameCanonicalInputProducesSameHash() {
        val canonical = EvidenceHasher.canonical(
            "ANDROID_LOCATION",
            EvidenceClassification.MEASURED,
            "Verified device location fix",
            "lat=1.0,lon=2.0",
            "Phone position",
            1000L,
            null
        )
        assertEquals(EvidenceHasher.sha256(canonical), EvidenceHasher.sha256(canonical))
    }

    @Test
    fun classificationIsPartOfCanonicalHashInput() {
        val measured = EvidenceHasher.canonical(
            "SOURCE", EvidenceClassification.MEASURED, "T", "P", null, 1L, null
        )
        val calculated = EvidenceHasher.canonical(
            "SOURCE", EvidenceClassification.CALCULATED, "T", "P", null, 1L, null
        )
        assertEquals(false, EvidenceHasher.sha256(measured) == EvidenceHasher.sha256(calculated))
    }
}
