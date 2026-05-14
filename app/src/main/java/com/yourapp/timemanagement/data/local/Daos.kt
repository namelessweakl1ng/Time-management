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
interface SubTaskDao {
    @Query("SELECT * FROM sub_tasks WHERE parentTaskId = :taskId ORDER BY id ASC")
    fun observeForTask(taskId: Long): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM sub_tasks ORDER BY parentTaskId ASC, id ASC")
    fun observeAll(): Flow<List<SubTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subTask: SubTaskEntity): Long

    @Query("UPDATE sub_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompleted(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM sub_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id IN (:ids) ORDER BY name ASC")
    fun observeByIds(ids: List<Long>): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    @Query("SELECT tags.* FROM tags INNER JOIN task_tag_cross_refs ON tags.id = task_tag_cross_refs.tagId WHERE task_tag_cross_refs.taskId = :taskId ORDER BY tags.name ASC")
    fun observeForTask(taskId: Long): Flow<List<TagEntity>>

    @Query("SELECT tagId FROM task_tag_cross_refs WHERE taskId = :taskId")
    fun observeTagIdsForTask(taskId: Long): Flow<List<Long>>

    @Query("SELECT taskId FROM task_tag_cross_refs WHERE tagId IN (:tagIds)")
    fun observeTaskIdsForTags(tagIds: List<Long>): Flow<List<Long>>

    @Query("SELECT * FROM task_tag_cross_refs")
    fun observeCrossRefs(): Flow<List<TaskTagCrossRefEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrossRef(ref: TaskTagCrossRefEntity)

    @Query("DELETE FROM task_tag_cross_refs WHERE taskId = :taskId AND tagId = :tagId")
    suspend fun deleteCrossRef(taskId: Long, tagId: Long)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)
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
