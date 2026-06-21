package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class SensorUtilsTest {

    @Test
    fun `magnitude of unit vector is 1`() {
        val mag = SensorUtils.magnitude(floatArrayOf(1f, 0f, 0f))
        assertEquals(1f, mag, 0.001f)
    }

    @Test
    fun `magnitude of 3-4-0 is 5`() {
        val mag = SensorUtils.magnitude(floatArrayOf(3f, 4f, 0f))
        assertEquals(5f, mag, 0.001f)
    }

    @Test
    fun `rollingRms of constant array returns that value`() {
        val rms = SensorUtils.rollingRms(floatArrayOf(4f, 4f, 4f))
        assertEquals(4f, rms, 0.001f)
    }

    @Test
    fun `rollingRms of empty array returns 0`() {
        assertEquals(0f, SensorUtils.rollingRms(floatArrayOf()), 0.001f)
    }

    @Test
    fun `lowPass with alpha=0 returns input immediately`() {
        assertEquals(5f, SensorUtils.lowPass(5f, 100f, 0f), 0.001f)
    }

    @Test
    fun `lowPass with alpha=1 returns previous unchanged`() {
        assertEquals(100f, SensorUtils.lowPass(5f, 100f, 1f), 0.001f)
    }

    @Test
    fun `variance of identical values is 0`() {
        val v = SensorUtils.variance(listOf(5f, 5f, 5f))
        assertEquals(0f, v, 0.001f)
    }

    @Test
    fun `variance of empty returns MAX_VALUE`() {
        assertEquals(Float.MAX_VALUE, SensorUtils.variance(emptyList()), 0.001f)
    }

    @Test
    fun `headingDegrees east is 0 or 360`() {
        val h = SensorUtils.headingDegrees(floatArrayOf(1f, 0f, 0f))
        assertTrue(abs(h) < 1f || abs(h - 360f) < 1f)
    }

    @Test
    fun `angleDifferenceDeg 350 to 10 is 20`() {
        val diff = SensorUtils.angleDifferenceDeg(350f, 10f)
        assertEquals(20f, diff, 0.5f)
    }

    @Test
    fun `stepsPerMinute calculation`() {
        val spm = SensorUtils.stepsPerMinute(100, 60_000L)
        assertEquals(100f, spm, 0.1f)
    }
}
