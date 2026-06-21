package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class SensorUtilsTest {

    // --------------------------------------------------------------------- magnitude

    @Test
    fun `magnitude of zero vector returns zero`() {
        assertEquals(0f, SensorUtils.magnitude(floatArrayOf(0f, 0f, 0f)), 0f)
    }

    @Test
    fun `magnitude of unit x returns 1`() {
        assertEquals(1f, SensorUtils.magnitude(floatArrayOf(1f, 0f, 0f)), 1e-6f)
    }

    @Test
    fun `magnitude of 3_4_0 vector returns 5`() {
        assertEquals(5f, SensorUtils.magnitude(floatArrayOf(3f, 4f, 0f)), 1e-5f)
    }

    @Test
    fun `magnitude of all-equal components equals sqrt3 times component`() {
        val v = 2f
        val expected = sqrt(3f) * v
        assertEquals(expected, SensorUtils.magnitude(floatArrayOf(v, v, v)), 1e-5f)
    }

    // --------------------------------------------------------------------- rollingRms

    @Test
    fun `rollingRms empty window returns zero`() {
        assertEquals(0f, SensorUtils.rollingRms(floatArrayOf()), 0f)
    }

    @Test
    fun `rollingRms constant array returns that constant`() {
        assertEquals(5f, SensorUtils.rollingRms(floatArrayOf(5f, 5f, 5f)), 1e-5f)
    }

    @Test
    fun `rollingRms all zeros returns zero`() {
        assertEquals(0f, SensorUtils.rollingRms(floatArrayOf(0f, 0f, 0f)), 0f)
    }

    @Test
    fun `rollingRms of 3 and 4 returns sqrt of 12_5`() {
        val expected = sqrt(12.5f)
        assertEquals(expected, SensorUtils.rollingRms(floatArrayOf(3f, 4f)), 1e-5f)
    }

    // --------------------------------------------------------------------- lowPass

    @Test
    fun `lowPass alpha zero returns input`() {
        assertEquals(7f, SensorUtils.lowPass(input = 7f, previous = 100f, alpha = 0f), 1e-6f)
    }

    @Test
    fun `lowPass alpha one returns previous`() {
        assertEquals(100f, SensorUtils.lowPass(input = 7f, previous = 100f, alpha = 1f), 1e-6f)
    }

    @Test
    fun `lowPass alpha 0_5 returns midpoint`() {
        assertEquals(10f, SensorUtils.lowPass(input = 20f, previous = 0f, alpha = 0.5f), 1e-5f)
    }

    // --------------------------------------------------------------------- variance

    @Test
    fun `variance of single element returns MAX_VALUE`() {
        assertEquals(Float.MAX_VALUE, SensorUtils.variance(listOf(5f)), 0f)
    }

    @Test
    fun `variance of empty list returns MAX_VALUE`() {
        assertEquals(Float.MAX_VALUE, SensorUtils.variance(emptyList()), 0f)
    }

    @Test
    fun `variance of identical values returns zero`() {
        assertEquals(0f, SensorUtils.variance(listOf(3f, 3f, 3f, 3f)), 1e-6f)
    }

    @Test
    fun `variance of 1 and 3 is 1`() {
        assertEquals(1f, SensorUtils.variance(listOf(1f, 3f)), 1e-5f)
    }

    // --------------------------------------------------------------------- headingDegrees

    @Test
    fun `headingDegrees east vector returns 0 degrees`() {
        assertEquals(0f, SensorUtils.headingDegrees(floatArrayOf(1f, 0f, 0f)), 1e-4f)
    }

    @Test
    fun `headingDegrees north vector returns 90 degrees`() {
        assertEquals(90f, SensorUtils.headingDegrees(floatArrayOf(0f, 1f, 0f)), 1e-4f)
    }

    @Test
    fun `headingDegrees west vector returns 180 degrees`() {
        assertEquals(180f, SensorUtils.headingDegrees(floatArrayOf(-1f, 0f, 0f)), 1e-4f)
    }

    @Test
    fun `headingDegrees south vector wraps to 270 degrees`() {
        assertEquals(270f, SensorUtils.headingDegrees(floatArrayOf(0f, -1f, 0f)), 1e-4f)
    }

    // --------------------------------------------------------------------- angleDifferenceDeg

    @Test
    fun `angleDifferenceDeg same heading returns zero`() {
        assertEquals(0f, SensorUtils.angleDifferenceDeg(90f, 90f), 1e-5f)
    }

    @Test
    fun `angleDifferenceDeg opposite headings returns 180`() {
        assertEquals(180f, SensorUtils.angleDifferenceDeg(0f, 180f), 1e-5f)
    }

    @Test
    fun `angleDifferenceDeg 350 to 10 wraps correctly to 20`() {
        assertEquals(20f, SensorUtils.angleDifferenceDeg(350f, 10f), 1e-4f)
    }

    @Test
    fun `angleDifferenceDeg is symmetric`() {
        val ab = SensorUtils.angleDifferenceDeg(30f, 120f)
        val ba = SensorUtils.angleDifferenceDeg(120f, 30f)
        assertEquals(ab, ba, 1e-5f)
    }

    // --------------------------------------------------------------------- stepsPerMinute

    @Test
    fun `stepsPerMinute zero delta returns zero`() {
        assertEquals(0f, SensorUtils.stepsPerMinute(100, 0L), 0f)
    }

    @Test
    fun `stepsPerMinute 60 steps in 60 seconds returns 60`() {
        assertEquals(60f, SensorUtils.stepsPerMinute(60, 60_000L), 1e-5f)
    }

    @Test
    fun `stepsPerMinute proportional to step count`() {
        val rate30 = SensorUtils.stepsPerMinute(30, 60_000L)
        val rate60 = SensorUtils.stepsPerMinute(60, 60_000L)
        assertEquals(rate60, rate30 * 2f, 1e-4f)
    }
}
