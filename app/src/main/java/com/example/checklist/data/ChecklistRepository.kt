package com.example.checklist.data

import com.example.checklist.ui.DailyPoint
import com.example.checklist.ui.TaskStat
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ChecklistRepository(private val dao: TaskDao) {

    fun observeActiveTasks(): Flow<List<Task>> = dao.observeActiveTasks()

    fun observeAllCompletions(): Flow<List<DailyCompletion>> = dao.observeAllCompletions()

    suspend fun addTask(title: String): Long {
        val position = dao.maxPosition() + 1
        return dao.insertTask(
            Task(title = title, position = position, active = true, createdAt = System.currentTimeMillis())
        )
    }

    suspend fun updateTask(taskId: Long, title: String) {
        val current = dao.getTaskById(taskId) ?: return
        dao.updateTask(current.copy(title = title))
    }

    suspend fun deleteTask(taskId: Long) {
        dao.deleteTaskById(taskId) // completions removed via FK CASCADE
    }

    suspend fun toggleTask(taskId: Long) {
        dao.toggleCompletion(taskId, today())
    }

    fun today(): String = LocalDate.now().toString()

    fun currentStreak(completions: List<DailyCompletion>, tasks: List<Task>, today: String): Int {
        val active = tasks.filter { it.active }
        if (active.isEmpty()) return 0
        // ponytail: only completed rows matter for a full day
        val completedByDate = completions.filter { it.completed }.groupBy { it.date }

        fun isFull(day: LocalDate): Boolean {
            val dayString = day.toString()
            val done = completedByDate[dayString]?.map { it.taskId }?.toSet() ?: emptySet()
            val required = active.filter {
                LocalDate.ofInstant(Instant.ofEpochMilli(it.createdAt), ZoneId.systemDefault()) <= day
            }
            return required.isNotEmpty() && required.all { it.id in done }
        }

        val todayDate = LocalDate.parse(today)
        var day = if (isFull(todayDate)) todayDate else todayDate.minusDays(1) // today neither adds nor breaks
        var streak = 0
        while (isFull(day)) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    fun computeStats(
        completions: List<DailyCompletion>,
        tasks: List<Task>,
        today: String = today()
    ): List<TaskStat> {
        val todayDate = LocalDate.parse(today)
        return tasks.map { task ->
            val taskCompletions = completions.filter { it.taskId == task.id }
            val trackedDays = taskCompletions.map { it.date }.distinct().size
            val completedDays = taskCompletions.filter { it.completed }.map { it.date }.distinct().size
            val rate = if (trackedDays == 0) 0f else completedDays.toFloat() / trackedDays
            val completedDates = taskCompletions.filter { it.completed }.map { it.date }.toSet()
            // ponytail: index 0 = today-6, index 6 = today
            val recent7Days = (0 until 7).map { todayDate.minusDays(6L - it).toString() in completedDates }
            TaskStat(task.id, task.title, completedDays, trackedDays, rate, recent7Days)
        }
    }

    fun dailySeries(
        completions: List<DailyCompletion>,
        today: String,
        days: Int = 14
    ): List<DailyPoint> {
        val todayDate = LocalDate.parse(today)
        // defensive: distinct taskId+date so duplicate rows never inflate the count
        val countByDate = completions
            .filter { it.completed }
            .distinctBy { it.taskId to it.date }
            .groupBy { it.date }
            .mapValues { (_, rows) -> rows.size }
        // ponytail: index 0 = today-(days-1), last index = today
        return (0 until days).map { offset ->
            val date = todayDate.minusDays((days - 1 - offset).toLong()).toString()
            DailyPoint(date, countByDate[date] ?: 0)
        }
    }
}
