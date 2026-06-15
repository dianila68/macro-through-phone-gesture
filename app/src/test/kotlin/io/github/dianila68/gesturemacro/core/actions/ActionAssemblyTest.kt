package io.github.dianila68.gesturemacro.core.actions

import io.github.dianila68.gesturemacro.core.serialization.IntentAction
import io.github.dianila68.gesturemacro.core.serialization.MediaControlAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ActionAssemblyTest {

    @Test
    fun `assemble fixed spec returns correct MacroAction`() {
        val spec = ActionCatalog.forId("media.play_pause")!!
        val action = ActionAssembly.assemble(spec) as MediaControlAction
        assertEquals("play_pause", action.command)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assemble template spec throws`() {
        val spec = ActionCatalog.forId("app.launch")!!
        ActionAssembly.assemble(spec)
    }

    @Test
    fun `assembleWithPackage binds package into IntentAction`() {
        val spec = ActionCatalog.forId("app.launch")!!
        val action = ActionAssembly.assembleWithPackage(spec, "com.test.pkg") as IntentAction
        assertEquals("com.test.pkg", action.target)
        assertEquals("launch", action.command)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assembleWithPackage with blank package throws`() {
        val spec = ActionCatalog.forId("app.launch")!!
        ActionAssembly.assembleWithPackage(spec, "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assembleWithPackage on non-template spec throws`() {
        val spec = ActionCatalog.forId("media.next")!!
        ActionAssembly.assembleWithPackage(spec, "com.test.pkg")
    }

    @Test
    fun `assembleById fixed spec works`() {
        val action = ActionAssembly.assembleById("system.flashlight")
        assertNotNull(action)
    }

    @Test
    fun `assembleById template with package works`() {
        val action = ActionAssembly.assembleById("app.launch", "com.spotify.music") as? IntentAction
        assertNotNull(action)
        assertEquals("com.spotify.music", action!!.target)
    }

    @Test
    fun `assembleById template without package returns null`() {
        val action = ActionAssembly.assembleById("app.launch")
        assertNull(action)
    }

    @Test
    fun `assembleById unknown id returns null`() {
        assertNull(ActionAssembly.assembleById("does.not.exist"))
    }
}
