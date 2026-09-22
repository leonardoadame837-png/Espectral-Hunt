package com.example

import com.example.data.engine.MultiAgentSecurityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testAllFindingsHaveValidRealGeolocation() {
        val engine = MultiAgentSecurityEngine()
        val findings = engine.defaultFindings
        assertTrue("Findings list should not be empty", findings.isNotEmpty())

        findings.forEach { finding ->
            assertTrue("Latitude out of range: ${finding.latitude}", finding.latitude in -90.0..90.0)
            assertTrue("Longitude out of range: ${finding.longitude}", finding.longitude in -180.0..180.0)
            assertFalse("Facility name must not be blank", finding.locationName.isBlank())
            assertFalse("Street address must not be blank", finding.locationAddress.isBlank())
            assertTrue("Elevation should be positive", finding.elevationMeters > 0)
        }
    }

    @Test
    fun testAllRfSignalsHaveValidRealGeolocation() {
        val engine = MultiAgentSecurityEngine()
        val signals = engine.defaultSignals
        assertTrue("Signals list should not be empty", signals.isNotEmpty())

        signals.forEach { sig ->
            assertTrue("Latitude out of range: ${sig.latitude}", sig.latitude in -90.0..90.0)
            assertTrue("Longitude out of range: ${sig.longitude}", sig.longitude in -180.0..180.0)
            assertFalse("Location name must not be blank", sig.locationName.isBlank())
            assertFalse("Location address must not be blank", sig.locationAddress.isBlank())
            assertTrue("Elevation should be positive", sig.elevationMeters >= 0)
        }
    }
}
