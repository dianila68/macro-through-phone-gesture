package io.github.dianila68.gesturemacro.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented migration test — requires an emulator or device.
 * Schema files committed under app/schemas/ are wired as androidTest assets
 * via sourceSets in build.gradle.kts so MigrationTestHelper can create a v1
 * database from the exported DDL and validate the post-migration v2 schema.
 */
@RunWith(AndroidJUnit4::class)
class MacroMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MacroDatabase::class.java,
    )

    @Test
    fun migration_1_to_2_preserves_rows_and_adds_null_seal() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO macros (id, name, enabled, document) VALUES " +
                    "('m1', 'Shake Flashlight', 1, '{\"version\":1}')",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MacroDatabase.MIGRATION_1_2).use { db ->
            db.query("SELECT id, integritySeal FROM macros WHERE id = 'm1'").use { cursor ->
                assertTrue("Row must survive migration", cursor.moveToFirst())
                // Pre-existing rows get NULL seal — they fail closed on load if they drive accessibility
                assertNull(
                    "integritySeal must be NULL for rows that predate the sealing column",
                    cursor.getString(cursor.getColumnIndexOrThrow("integritySeal")),
                )
            }
        }
    }

    @Test
    fun migration_1_to_2_schema_matches_v2_entity_definition() {
        // createDatabase creates an empty v1 DB; runMigrationsAndValidate checks structure
        helper.createDatabase(TEST_DB + "_empty", 1).use { /* empty — structure only */ }

        helper.runMigrationsAndValidate(
            TEST_DB + "_empty",
            2,
            true,
            MacroDatabase.MIGRATION_1_2,
        ).use { db ->
            // Verify integritySeal column is present and nullable in the migrated schema
            db.query("PRAGMA table_info(macros)").use { cursor ->
                val cols = mutableMapOf<String, Pair<String, Int>>()
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                    val notNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull"))
                    cols[name] = type to notNull
                }
                assertTrue("integritySeal column must exist", cols.containsKey("integritySeal"))
                val (_, notNull) = cols.getValue("integritySeal")
                assertTrue("integritySeal must be nullable (notnull=0)", notNull == 0)
            }
        }
    }

    companion object {
        private const val TEST_DB = "macro_migration_test.db"
    }
}
