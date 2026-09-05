package com.example.checklist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.checklist.data.ChecklistRepository
import com.example.checklist.data.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskUi(
    val id: Long,
    val title: String,
    val completedToday: Boolean
)

data class ChecklistUiState(
    val tasks: List<TaskUi>,
    val today: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val progress: Float,
    val streak: Int
)

data class TaskStat(
    val id: Long,
    val title: String,
    val completedDays: Int,
    val trackedDays: Int,
    val rate: Float,
    val recent7Days: List<Boolean> = List(7) { false }
)

data class DailyPoint(
    val date: String,
    val count: Int
)

class ChecklistViewModel(private val repo: ChecklistRepository) : ViewModel() {

    private val initialUiState = ChecklistUiState(
        tasks = emptyList(),
        today = repo.today(),
        totalTasks = 0,
        completedTasks = 0,
        progress = 0f,
        streak = 0
    )

    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ChecklistUiState> =
        combine(
            repo.observeActiveTasks(),
            repo.observeAllCompletions(),
            refreshTrigger
        ) { tasks, completions, _ ->
            val today = repo.today()
            val tasksUi = tasks.map { task ->
                TaskUi(
                    id = task.id,
                    title = task.title,
                    completedToday = completions.any {
                        it.taskId == task.id && it.date == today && it.completed
                    }
                )
            }
            val completedTasks = tasksUi.count { it.completedToday }
            ChecklistUiState(
                tasks = tasksUi,
                today = today,
                totalTasks = tasksUi.size,
                completedTasks = completedTasks,
                progress = if (tasksUi.isEmpty()) 0f
                else completedTasks.toFloat() / tasksUi.size,
                streak = repo.currentStreak(completions, tasks, today)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialUiState)

    val stats: StateFlow<List<TaskStat>> =
        combine(repo.observeAllCompletions(), repo.observeActiveTasks(), refreshTrigger) { completions, tasks, _ ->
            repo.computeStats(completions, tasks)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySeries: StateFlow<List<DailyPoint>> =
        combine(repo.observeAllCompletions(), refreshTrigger) { completions, _ ->
            repo.dailySeries(completions, repo.today())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTask(taskId: Long) {
        viewModelScope.launch { repo.toggleTask(taskId) }
    }

    fun addTask(title: String) {
        viewModelScope.launch { repo.addTask(title) }
    }

    fun updateTask(taskId: Long, title: String) {
        viewModelScope.launch { repo.updateTask(taskId, title) }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch { repo.deleteTask(taskId) }
    }

    fun refresh() {
        // Re-emits state so "today" is recalculated on app resume.
        refreshTrigger.value++
    }

    companion object {
        fun factory(repo: ChecklistRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ChecklistViewModel(repo) }
            }
    }
}
