package io.github.dianila68.gesturemacro.core.data

import io.github.dianila68.gesturemacro.core.security.HmacSealer
import io.github.dianila68.gesturemacro.core.serialization.AccessibilityAction
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.serialization.SensorKind
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction
import io.github.dianila68.gesturemacro.core.serialization.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

class MacroIntegrityTest {

    // A plain JVM HMAC key stands in for the Keystore key (the policy logic is identical).
    private val sealer = HmacSealer(KeyGenerator.getInstance(HmacSealer.ALGORITHM).generateKey())
    private val integrity = MacroIntegrity(sealer)

    private fun macro(action: io.github.dianila68.gesturemacro.core.serialization.MacroAction) = GestureMacro(
        version = 1,
        id = "m1",
        name = "m1",
        enabled = true,
        trigger = Trigger(sensor = SensorKind.ACCELEROMETER, pattern = PatternKind.SHAKE),
        actions = listOf(action),
    )

    private val accessibilityMacro = macro(AccessibilityAction(target = "com.bank.app", command = "back"))
    private val plainMacro = macro(SystemToggleAction(target = "flashlight"))

    @Test
    fun `only accessibility macros get a seal`() {
        assertNull(integrity.sealFor("doc", plainMacro))
        assertEquals(sealer.seal("doc"), integrity.sealFor("doc", accessibilityMacro))
    }

    @Test
    fun `valid seal loads accessibility macro unchanged`() {
        val seal = integrity.sealFor("doc", accessibilityMacro)
        val loaded = integrity.verifyOnLoad(accessibilityMacro, "doc", seal)
        assertTrue(loaded.enabled)
    }

    @Test
    fun `tampered document fails closed`() {
        val seal = integrity.sealFor("doc", accessibilityMacro)
        val loaded = integrity.verifyOnLoad(accessibilityMacro, "doc-TAMPERED", seal)
        assertFalse(loaded.enabled)
    }

    @Test
    fun `missing seal on an accessibility macro fails closed`() {
        val loaded = integrity.verifyOnLoad(accessibilityMacro, "doc", null)
        assertFalse(loaded.enabled)
    }

    @Test
    fun `plain macros are never disabled by integrity`() {
        val loaded = integrity.verifyOnLoad(plainMacro, "doc", null)
        assertTrue(loaded.enabled)
    }

    @Test
    fun `without a sealer accessibility macros fail closed`() {
        val noSealer = MacroIntegrity(null)
        assertNull(noSealer.sealFor("doc", accessibilityMacro))
        assertFalse(noSealer.verifyOnLoad(accessibilityMacro, "doc", "anything").enabled)
        assertTrue(noSealer.verifyOnLoad(plainMacro, "doc", null).enabled)
    }
}
