package com.example

import com.example.data.engine.MultiAgentSecurityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun defaultForensicDataDoesNotContainSyntheticFindings() {
        val engine = MultiAgentSecurityEngine()
        assertTrue("Synthetic tactical findings must not be seeded", engine.defaultFindings.isEmpty())
        assertTrue("Synthetic RF signals must not be seeded", engine.defaultSignals.isEmpty())
    }

    @Test
    fun realDeviceEnvironmentDoesNotManufactureEvidence() {
        val engine = MultiAgentSecurityEngine()
        val signals = engine.getSignalsForEnvironment(
            environmentMode = com.example.data.model.TacticalEnvironmentMode.REAL_DEVICE,
            deviceLocation = null,
            realWifiSignal = null
        )
        val findings = engine.getFindingsForEnvironment(
            environmentMode = com.example.data.model.TacticalEnvironmentMode.REAL_DEVICE,
            deviceLocation = null
        )
        assertTrue("No RF evidence should be created without a supported observation source", signals.isEmpty())
        assertTrue("GPS alone must not create a vulnerability finding", findings.isEmpty())
    }
}
