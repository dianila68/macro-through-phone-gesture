package io.github.dianila68.gesturemacro.core.triggers

import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerLibraryTest {

    @Test
    fun `patterns are unique across the catalog`() {
        val patterns = TriggerLibrary.all.map { it.pattern }
        assertEquals(patterns.size, patterns.toSet().size)
    }

    @Test
    fun `custom is never offered as a trigger`() {
        assertTrue(TriggerLibrary.all.none { it.pattern == PatternKind.CUSTOM })
    }

    @Test
    fun `available entries build a detector and unavailable ones do not`() {
        TriggerLibrary.all.forEach { spec ->
            if (spec.available) {
                assertNotNull("${spec.pattern} is available but built no detector", spec.buildDetector())
            } else {
                assertNull("${spec.pattern} is unavailable but built a detector", spec.buildDetector())
            }
        }
    }

    @Test
    fun `detectors cover exactly the available triggers`() {
        val detectorPatterns = TriggerLibrary.detectors().map { it.pattern }.toSet()
        assertEquals(
            setOf(
                GesturePattern.SHAKE,
                GesturePattern.DOUBLE_SHAKE,
                GesturePattern.FLIP_FACE_DOWN,
                GesturePattern.FLIP_FACE_UP,
                GesturePattern.TWIST,
                GesturePattern.PROXIMITY_WAVE,
                GesturePattern.FALL,
                // M4
                GesturePattern.STEP_DETECTED,
                GesturePattern.IS_STATIONARY,
                GesturePattern.PICKED_UP,
                GesturePattern.GOING_DARK,
                GesturePattern.GOING_BRIGHT,
                GesturePattern.ALTITUDE_RISE,
                GesturePattern.ALTITUDE_FALL,
            ),
            detectorPatterns,
        )
    }

    @Test
    fun `live detectors span accelerometer, gyroscope, proximity, step, light, pressure`() {
        val sensors = TriggerLibrary.detectors().map { it.sensor }.toSet()
        assertTrue(sensors.contains(SensorType.ACCELEROMETER))
        assertTrue(sensors.contains(SensorType.GYROSCOPE))
        assertTrue(sensors.contains(SensorType.STEP_COUNTER))
        assertTrue(sensors.contains(SensorType.LIGHT))
        assertTrue(sensors.contains(SensorType.PRESSURE))
    }

    @Test
    fun `forPattern resolves a known trigger and is null for absent ones`() {
        assertEquals(PatternKind.SHAKE, TriggerLibrary.forPattern(PatternKind.SHAKE)?.pattern)
        assertNull(TriggerLibrary.forPattern(PatternKind.CUSTOM))
    }

    @Test
    fun `available contains only available specs and never exceeds the catalog`() {
        assertTrue(TriggerLibrary.available.all { it.available })
        assertTrue(TriggerLibrary.available.isNotEmpty())
        assertTrue(TriggerLibrary.available.size <= TriggerLibrary.all.size)
    }
}
