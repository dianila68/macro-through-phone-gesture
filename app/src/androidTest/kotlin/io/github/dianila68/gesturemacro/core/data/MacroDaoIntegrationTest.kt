package io.github.dianila68.gesturemacro.core.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.dianila68.gesturemacro.core.security.HmacSealer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.KeyGenerator

/**
 * DAO round-trip + integrity-seal fail-closed verification.
 * Uses an in-memory Room database so no disk I/O or Keystore is needed;
 * a plain JVM HMAC key substitutes for the Keystore key (identical logic).
 */
@RunWith(AndroidJUnit4::class)
class MacroDaoIntegrationTest {

    private lateinit var db: MacroDatabase
    private lateinit var dao: MacroDao

    @Before
    fun setUp() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, MacroDatabase::class.java)
            .build()
        dao = db.macroDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_and_observe_plain_entity_round_trip() = runTest {
        val entity = MacroEntity(
            id = "id1",
            name = "Shake Flashlight",
            enabled = true,
            document = """{"version":1}""",
            integritySeal = null,
        )
        dao.upsert(entity)

        val rows = dao.observeAll().first()
        assertEquals(1, rows.size)
        assertEquals(entity, rows[0])
    }

    @Test
    fun integrity_seal_column_survives_round_trip() = runTest {
        val seal = "hmac-base64-placeholder"
        val entity = MacroEntity("id2", "Accessibility Macro", true, """{"version":1}""", seal)
        dao.upsert(entity)

        val loaded = dao.observeAll().first()
        assertEquals(seal, loaded[0].integritySeal)
    }

    @Test
    fun set_enabled_updates_only_enabled_flag() = runTest {
        val entity = MacroEntity("id3", "Test", enabled = true, document = "{}", integritySeal = null)
        dao.upsert(entity)
        dao.setEnabled("id3", false)

        val loaded = dao.observeAll().first()[0]
        assertFalse(loaded.enabled)
        assertEquals("{}", loaded.document)
    }

    @Test
    fun delete_removes_row() = runTest {
        dao.upsert(MacroEntity("id4", "Delete Me", true, "{}", null))
        dao.delete("id4")

        assertEquals(0, dao.observeAll().first().size)
    }

    /**
     * Tamper the document column directly via SQL after a valid seal was stored.
     * MacroIntegrity.verifyOnLoad must force-disable the macro when the seal
     * no longer matches the tampered document — this is the threat-T5 fail-closed guarantee.
     */
    @Test
    fun tampered_document_detected_by_integrity_fail_closed() = runTest {
        val sealer = HmacSealer(KeyGenerator.getInstance(HmacSealer.ALGORITHM).generateKey())
        val doc = """{"version":1,"id":"acc1"}"""
        val seal = sealer.seal(doc)

        dao.upsert(MacroEntity("acc1", "Acc Macro", enabled = true, document = doc, integritySeal = seal))

        // Simulate out-of-band DB tampering
        db.openHelper.writableDatabase.execSQL(
            "UPDATE macros SET document = '{\"version\":1,\"id\":\"acc1\",\"INJECTED\":true}' WHERE id = 'acc1'",
        )

        val row = dao.observeAll().first()[0]
        val macroDoc = row.document
        val valid = sealer.verify(macroDoc, row.integritySeal ?: "")
        assertFalse("Seal must not verify after document tamper", valid)
    }

    @Test
    fun null_seal_on_an_accessibility_row_verifies_as_false() = runTest {
        val sealer = HmacSealer(KeyGenerator.getInstance(HmacSealer.ALGORITHM).generateKey())
        val doc = """{"version":1}"""

        // Row inserted without a seal (simulates a v1→v2 migrated row)
        dao.upsert(MacroEntity("acc2", "Acc Macro 2", enabled = true, document = doc, integritySeal = null))

        val row = dao.observeAll().first()[0]
        assertNull(row.integritySeal)
        assertFalse("verify with null seal must be false", sealer.verify(doc, row.integritySeal ?: ""))
    }

    @Test
    fun execution_log_insert_and_observe() = runTest {
        dao.insertLog(
            ExecutionLogEntity(
                macroId = "m1",
                macroName = "Test",
                timestamp = 1_000L,
                success = true,
                detail = "1 action(s) ok",
            ),
        )
        val logs = dao.observeRecentLogs().first()
        assertEquals(1, logs.size)
        assertEquals("m1", logs[0].macroId)
        assertEquals(true, logs[0].success)
    }
}
