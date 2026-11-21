package com.example.core.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilityCheckerTest {

    private fun gb(amount: Long): Long = amount * 1024L * 1024L * 1024L

    @Test
    fun fourGbDeviceFlagsLowMemoryNotice() {
        val report = DeviceCapabilityChecker.evaluate(gb(4))
        assertTrue(report.isLowMemoryDevice)
        assertEquals("Gemma 1B", report.recommendedModel)
        assertNotNull(report.compatibilityNotice)
    }

    @Test
    fun eightGbPixel7PassesCleanlyForGemma1B() {
        val report = DeviceCapabilityChecker.evaluate(gb(8))
        assertFalse(report.isLowMemoryDevice)
        assertEquals("Gemma 1B", report.recommendedModel)
        assertNull(report.compatibilityNotice)
    }

    @Test
    fun twelveGbFlagshipRecommendsGemma4B() {
        val report = DeviceCapabilityChecker.evaluate(gb(12))
        assertFalse(report.isLowMemoryDevice)
        assertEquals("Gemma 4B", report.recommendedModel)
        assertNull(report.compatibilityNotice)
    }
}

