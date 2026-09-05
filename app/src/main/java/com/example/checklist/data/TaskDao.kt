package com.example.checklist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE active = 1 ORDER BY position ASC")
    fun observeActiveTasks(): Flow<List<Task>>

    @Query("SELECT COALESCE(MAX(position), 0) FROM tasks")
    suspend fun maxPosition(): Int

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("SELECT * FROM daily_completions WHERE task_id = :taskId AND date = :date")
    suspend fun getCompletion(taskId: Long, date: String): DailyCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: DailyCompletion)

    @Transaction
    suspend fun toggleCompletion(taskId: Long, date: String) {
        val cur = getCompletion(taskId, date)
        upsertCompletion(DailyCompletion(taskId = taskId, date = date, completed = cur?.completed != true))
    }

    @Query("SELECT * FROM daily_completions WHERE date = :date")
    suspend fun getCompletionsForDate(date: String): List<DailyCompletion>

    @Query("SELECT * FROM daily_completions")
    fun observeAllCompletions(): Flow<List<DailyCompletion>>
}
