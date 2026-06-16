package io.github.dianila68.gesturemacro.android.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * ticket-037: Room database quarantined to `.android.data`.
 * Pure data models and Dao interface stay here; `core.data` exposes typealiases.
 */
@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    val document: String,
    val integritySeal: String? = null,
)

@Entity(tableName = "execution_log")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val macroId: String,
    val macroName: String,
    val timestamp: Long,
    val success: Boolean,
    val detail: String,
)

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY name")
    fun observeAll(): Flow<List<MacroEntity>>

    @Query("SELECT COUNT(*) FROM macros")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MacroEntity)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE macros SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Insert
    suspend fun insertLog(entry: ExecutionLogEntity)

    @Query("SELECT * FROM execution_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentLogs(limit: Int = 50): Flow<List<ExecutionLogEntity>>
}

@Database(
    entities = [MacroEntity::class, ExecutionLogEntity::class, RecordedGestureEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class MacroDatabase : RoomDatabase() {
    abstract fun macroDao(): MacroDao
    abstract fun recordedGestureDao(): RecordedGestureDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE macros ADD COLUMN integritySeal TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `recorded_gesture` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `envelope_json` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `sample_count` INTEGER NOT NULL,
                        `hmac` TEXT NOT NULL DEFAULT '',
                        `enabled` INTEGER NOT NULL DEFAULT 1
                    )"""
                )
            }
        }

        @Volatile private var instance: MacroDatabase? = null

        fun get(context: Context): MacroDatabase = instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }

        fun build(context: Context): MacroDatabase =
            Room.databaseBuilder(context.applicationContext, MacroDatabase::class.java, "macros.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
