package com.example.checklist.data

import com.example.checklist.ui.TaskStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ChecklistRepositoryTest {

    private val repo = ChecklistRepository(FakeTaskDao())

    private fun task(id: Long, createdAt: LocalDate) =
        Task(id = id, title = "t$id", position = 0, active = true,
            createdAt = createdAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())

    private fun comp(taskId: Long, date: String, completed: Boolean = true) =
        DailyCompletion(taskId = taskId, date = date, completed = completed)

    // ---- currentStreak ----

    @Test
    fun `sin datos devuelve 0`() {
        assertEquals(0, repo.currentStreak(emptyList(), emptyList(), "2026-09-05"))
    }

    @Test
    fun `cero tareas activas devuelve 0 sin bucle infinito`() {
        val tasks = listOf(task(1, LocalDate.of(2026, 9, 1)).copy(active = false))
        val completions = listOf(comp(1, "2026-09-05"))
        assertEquals(0, repo.currentStreak(completions, tasks, "2026-09-05"))
    }

    @Test
    fun `hoy lleno devuelve 1`() {
        val tasks = listOf(task(1, LocalDate.of(2026, 9, 1)))
        val completions = listOf(comp(1, "2026-09-05"))
        assertEquals(1, repo.currentStreak(completions, tasks, "2026-09-05"))
    }

    @Test
    fun `ayer lleno y hoy parcial devuelve 1 y hoy no rompe`() {
        val tasks = listOf(task(1, LocalDate.of(2026, 9, 1)), task(2, LocalDate.of(2026, 9, 1)))
        val completions = listOf(
            comp(1, "2026-09-04"), comp(2, "2026-09-04"), // ayer lleno
            comp(1, "2026-09-05")                          // hoy parcial (falta t2)
        )
        assertEquals(1, repo.currentStreak(completions, tasks, "2026-09-05"))
    }

    @Test
    fun `ayer no lleno y hoy parcial devuelve 0`() {
        val tasks = listOf(task(1, LocalDate.of(2026, 9, 1)), task(2, LocalDate.of(2026, 9, 1)))
        val completions = listOf(
            comp(1, "2026-09-04"),                          // ayer no lleno (falta t2)
            comp(1, "2026-09-05")                           // hoy parcial
        )
        assertEquals(0, repo.currentStreak(completions, tasks, "2026-09-05"))
    }

    @Test
    fun `racha de 3 dias llenos consecutivos devuelve 3`() {
        val tasks = listOf(task(1, LocalDate.of(2026, 8, 1)))
        val completions = listOf(
            comp(1, "2026-09-03"), comp(1, "2026-09-04"), comp(1, "2026-09-05")
        )
        assertEquals(3, repo.currentStreak(completions, tasks, "2026-09-05"))
    }

    @Test
    fun `tarea creada despues de un dia no cuenta para ese dia`() {
        // t2 creada el 9-05, no cuenta para el 9-04
        val tasks = listOf(
            task(1, LocalDate.of(2026, 8, 1)),
            task(2, LocalDate.of(2026, 9, 5))
        )
        // 9-04: solo t1 puede estar (t2 aún no existe) -> lleno
        // 9-05: t1 y t2 -> t2 falta -> no lleno
        val completions = listOf(
            comp(1, "2026-09-04"), comp(1, "2026-09-05")
        )
        assertEquals(1, repo.currentStreak(completions, tasks, "2026-09-05"))
    }

    @Test
    fun `tarea creada hoy si cuenta para hoy`() {
        val tasks = listOf(task(1, LocalDate.of(2026, 9, 5)))
        val completions = listOf(comp(1, "2026-09-05"))
        assertEquals(1, repo.currentStreak(completions, tasks, "2026-09-05"))
    }

    // ---- computeStats ----

    @Test
    fun `tarea sin registros rate 0 y trackedDays 0`() {
        val tasks = listOf(task(1, LocalDate.of(2026, 9, 1)))
        val stats = repo.computeStats(emptyList(), tasks)
        val s = stats.single()
        assertEquals(TaskStat(1, "t1", 0, 0, 0f), s)
        assertEquals(0f, s.rate, 0f)
    }

    @Test
    fun `2 dias con fila 1 completado rate 0_5`() {
        val tasks = listOf(task(1, LocalDate.of(2026, 9, 1)))
        val completions = listOf(
            comp(1, "2026-09-04", completed = true),
            comp(1, "2026-09-05", completed = false)
        )
        val s = repo.computeStats(completions, tasks).single()
        assertEquals(2, s.trackedDays)
        assertEquals(1, s.completedDays)
        assertEquals(0.5f, s.rate, 0.0001f)
    }

    @Test
    fun `filas duplicadas del mismo dia no inflan trackedDays`() {
        val tasks = listOf(task(1, LocalDate.of(2026, 9, 1)))
        val completions = listOf(
            comp(1, "2026-09-04", completed = true),
            comp(1, "2026-09-04", completed = false), // mismo día duplicado
            comp(1, "2026-09-05", completed = true)
        )
        val s = repo.computeStats(completions, tasks).single()
        assertEquals(2, s.trackedDays)
        assertEquals(2, s.completedDays)
    }

    // ---- fake dao ----

    private class FakeTaskDao : TaskDao {
        override fun observeActiveTasks(): Flow<List<Task>> = emptyFlow()
        override fun observeAllCompletions(): Flow<List<DailyCompletion>> = emptyFlow()
        override suspend fun maxPosition(): Int = 0
        override suspend fun insertTask(task: Task): Long = 0
        override suspend fun updateTask(task: Task) {}
        override suspend fun deleteTaskById(taskId: Long) {}
        override suspend fun getCompletion(taskId: Long, date: String): DailyCompletion? = null
        override suspend fun upsertCompletion(completion: DailyCompletion) {}
        override suspend fun getCompletionsForDate(date: String): List<DailyCompletion> = emptyList()
    }
}
