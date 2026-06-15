package io.github.dianila68.gesturemacro.core.actions

import io.github.dianila68.gesturemacro.core.serialization.AccessibilityAction
import io.github.dianila68.gesturemacro.core.serialization.IntentAction
import io.github.dianila68.gesturemacro.core.serialization.LocationAlertAction
import io.github.dianila68.gesturemacro.core.serialization.MediaControlAction
import io.github.dianila68.gesturemacro.core.serialization.PlaySoundAction
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionCatalogTest {

    @Test
    fun `available entries are a subset of all`() {
        assertTrue(ActionCatalog.all.containsAll(ActionCatalog.available))
        assertTrue(ActionCatalog.available.all { it.available })
    }

    @Test
    fun `no duplicate ids`() {
        val ids = ActionCatalog.all.map { it.id }
        assertEquals("Duplicate catalog ids found: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}", ids.size, ids.distinct().size)
    }

    @Test
    fun `every available entry has a builder (init invariant)`() {
        // The init block enforces this, but assert the semantic via buildDirect / buildWithPackage
        ActionCatalog.available.forEach { spec ->
            if (!spec.requiresPackage) {
                assertNotNull("${spec.id} is available but buildDirect returned null", spec.buildDirect())
            } else {
                assertNotNull("${spec.id} is available but buildWithPackage returned null", spec.buildWithPackage("com.test.pkg"))
            }
        }
    }

    @Test
    fun `every category has at least one available entry`() {
        val covered = ActionCatalog.available.map { it.category }.toSet()
        ActionCategory.entries.forEach { cat ->
            assertTrue("Category $cat has no available entries", cat in covered)
        }
    }

    @Test
    fun `byCategory partitions available`() {
        val byCat = ActionCatalog.byCategory()
        val flattened = byCat.values.flatten()
        assertEquals(ActionCatalog.available.sortedBy { it.id }, flattened.sortedBy { it.id })
    }

    @Test
    fun `forId returns correct spec`() {
        val spec = ActionCatalog.forId("media.play_pause")
        assertNotNull(spec)
        assertEquals(ActionCategory.MEDIA_CONTROL, spec!!.category)
    }

    @Test
    fun `unavailable entry has no builder (init invariant for future entries)`() {
        // All current entries are available; verify the invariant would enforce it via reflection is N/A here.
        // Instead verify that requiring available==true with null builder throws.
        var threw = false
        try {
            ActionSpec(
                id = "test.invalid",
                category = ActionCategory.SOUND,
                displayName = "Invalid",
                description = "",
                available = true,
                builder = null,
            )
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("ActionSpec with available=true and null builder must throw", threw)
    }

    // ── Catalog/executor parity ───────────────────────────────────────────────
    // Every available entry must build a MacroAction whose type is accepted by
    // the executor dispatch (ActionDispatcher.when branches). We check structural
    // type since executors are Android-bound and can't be instantiated in JVM tests.

    @Test
    fun `media entries build MediaControlAction with known command`() {
        val known = setOf("play_pause", "next", "previous", "stop")
        ActionCatalog.forCategory(ActionCategory.MEDIA_CONTROL).forEach { spec ->
            val action = ActionAssembly.assemble(spec) as? MediaControlAction
            assertNotNull("${spec.id} did not produce MediaControlAction", action)
            assertTrue("${spec.id} command '${action!!.command}' not in executor vocabulary", action.command in known)
        }
    }

    @Test
    fun `system toggle entries build SystemToggleAction with known target`() {
        val known = setOf("flashlight")
        ActionCatalog.forCategory(ActionCategory.SYSTEM_TOGGLE).forEach { spec ->
            val action = ActionAssembly.assemble(spec) as? SystemToggleAction
            assertNotNull("${spec.id} did not produce SystemToggleAction", action)
            assertTrue("${spec.id} target '${action!!.target}' not in executor vocabulary", action.target in known)
        }
    }

    @Test
    fun `app launch entry builds IntentAction with launch command`() {
        ActionCatalog.forCategory(ActionCategory.APP_LAUNCH).forEach { spec ->
            val action = ActionAssembly.assembleWithPackage(spec, "com.spotify.music") as? IntentAction
            assertNotNull("${spec.id} did not produce IntentAction", action)
            assertEquals("launch", action!!.command)
            assertEquals("com.spotify.music", action.target)
        }
    }

    @Test
    fun `sound entries build PlaySoundAction`() {
        ActionCatalog.forCategory(ActionCategory.SOUND).forEach { spec ->
            assertFalse(spec.requiresPackage)
            val action = ActionAssembly.assemble(spec)
            assertTrue("${spec.id} did not produce PlaySoundAction", action is PlaySoundAction)
        }
    }

    @Test
    fun `location alert entry builds LocationAlertAction`() {
        ActionCatalog.forCategory(ActionCategory.LOCATION_ALERT).forEach { spec ->
            val action = ActionAssembly.assemble(spec)
            assertTrue("${spec.id} did not produce LocationAlertAction", action is LocationAlertAction)
        }
    }

    @Test
    fun `accessibility entries build AccessibilityAction with known command`() {
        val known = setOf("back", "notifications")
        ActionCatalog.forCategory(ActionCategory.ACCESSIBILITY).forEach { spec ->
            val action = ActionAssembly.assemble(spec) as? AccessibilityAction
            assertNotNull("${spec.id} did not produce AccessibilityAction", action)
            assertTrue("${spec.id} command '${action!!.command}' not in executor vocabulary", action.command in known)
        }
    }
}
