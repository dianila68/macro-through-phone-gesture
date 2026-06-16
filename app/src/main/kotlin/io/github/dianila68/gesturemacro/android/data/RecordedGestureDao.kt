package io.github.dianila68.gesturemacro.android.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recorded_gesture")
data class RecordedGestureEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "envelope_json") val envelopeJson: String,
    val confidence: Float,
    @ColumnInfo(name = "sample_count") val sampleCount: Int,
    val hmac: String = "",
    val enabled: Boolean = true,
)

@Dao
interface RecordedGestureDao {
    @Query("SELECT * FROM recorded_gesture ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RecordedGestureEntity>>

    @Query("SELECT * FROM recorded_gesture WHERE id = :id")
    suspend fun getById(id: String): RecordedGestureEntity?

    @Query("SELECT * FROM recorded_gesture WHERE enabled = 1")
    suspend fun getAllEnabled(): List<RecordedGestureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecordedGestureEntity)

    @Delete
    suspend fun delete(entity: RecordedGestureEntity)

    @Query("UPDATE recorded_gesture SET enabled = 0 WHERE id = :id")
    suspend fun disable(id: String)
}
