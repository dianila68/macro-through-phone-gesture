package io.github.dianila68.gesturemacro.android.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recorded_gesture")
data class RecordedGestureEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val envelopeJson: String,
    val confidence: Float,
    val sampleCount: Int,
    val integritySeal: String? = null,
)

@Dao
interface RecordedGestureDao {
    @Query("SELECT * FROM recorded_gesture ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RecordedGestureEntity>>

    @Query("SELECT * FROM recorded_gesture WHERE id = :id")
    suspend fun getById(id: String): RecordedGestureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecordedGestureEntity)

    @Delete
    suspend fun delete(entity: RecordedGestureEntity)

    @Query("SELECT COUNT(*) FROM macros WHERE document LIKE '%' || :envelopeId || '%'")
    suspend fun countMacrosUsing(envelopeId: String): Int
}
