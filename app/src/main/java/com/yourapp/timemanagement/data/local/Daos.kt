package com.yourapp.timemanagement.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dateEpochDay ASC, sortOrder ASC, startMinuteOfDay ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dateEpochDay = :dateEpochDay ORDER BY sortOrder ASC, startMinuteOfDay ASC")
    fun observeByDate(dateEpochDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE dateEpochDay = :dateEpochDay ORDER BY sortOrder ASC, startMinuteOfDay ASC")
    suspend fun getByDate(dateEpochDay: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE startedAtMillis >= :startMillis AND startedAtMillis < :endMillis ORDER BY startedAtMillis DESC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE endedAtMillis IS NULL ORDER BY startedAtMillis DESC LIMIT 1")
    fun observeActive(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE endedAtMillis IS NULL ORDER BY startedAtMillis DESC LIMIT 1")
    suspend fun getActive(): FocusSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("UPDATE focus_sessions SET endedAtMillis = :endedAtMillis, productiveMinutes = :productiveMinutes, distractedMinutes = :distractedMinutes, breakMinutes = :breakMinutes, interruptionCount = :interruptionCount, note = :note WHERE id = :id")
    suspend fun finish(
        id: Long,
        endedAtMillis: Long,
        productiveMinutes: Int,
        distractedMinutes: Int,
        breakMinutes: Int,
        interruptionCount: Int,
        note: String,
    )
}

@Dao
interface InterruptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interruption: InterruptionEntity): Long

    @Query("SELECT * FROM interruptions WHERE sessionId = :sessionId ORDER BY startedAtMillis DESC")
    fun observeBySession(sessionId: Long): Flow<List<InterruptionEntity>>
}

@Dao
interface DailyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats ORDER BY dateEpochDay DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyStatsEntity>>
}

@Dao
interface WidgetPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: WidgetPreferenceEntity)

    @Query("SELECT * FROM widget_preferences WHERE widgetId = :widgetId")
    suspend fun get(widgetId: Int): WidgetPreferenceEntity?

    @Query("SELECT * FROM widget_preferences")
    fun observeAll(): Flow<List<WidgetPreferenceEntity>>

    @Query("DELETE FROM widget_preferences WHERE widgetId = :widgetId")
    suspend fun delete(widgetId: Int)
}
