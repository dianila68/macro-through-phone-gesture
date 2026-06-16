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

@Database(entities = [MacroEntity::class, ExecutionLogEntity::class], version = 2, exportSchema = true)
abstract class MacroDatabase : RoomDatabase() {
    abstract fun macroDao(): MacroDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE macros ADD COLUMN integritySeal TEXT")
            }
        }

        fun build(context: Context): MacroDatabase =
            Room.databaseBuilder(context.applicationContext, MacroDatabase::class.java, "macros.db")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
