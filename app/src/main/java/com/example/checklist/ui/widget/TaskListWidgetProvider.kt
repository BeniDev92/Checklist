package com.example.checklist.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.example.checklist.data.AppDatabase
import com.example.checklist.data.ChecklistRepository
import com.example.checklist.data.Task

class TaskListWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskListWidget
}

object TaskListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val tasks = db.taskDao().getActiveTasks()
        val today = ChecklistRepository.today()
        val completions = db.taskDao().getCompletionsForDate(today)

        val rows: List<Pair<Task, Boolean>> = tasks.map { task ->
            task to completions.any { it.taskId == task.id && it.completed }
        }

        provideContent {
            TaskListContent(rows)
        }
    }
}

@Composable
private fun TaskListContent(rows: List<Pair<Task, Boolean>>) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(16)) {
        if (rows.isEmpty()) {
            Text("Sin tareas")
        } else {
            rows.forEach { (task, completedToday) ->
                Row(modifier = GlanceModifier.padding(4)) {
                    Text(
                        text = task.title,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    CheckBox(
                        checked = completedToday,
                        onCheckedChange = actionRunCallback<ToggleTaskAction>(
                            actionParametersOf(TaskToggleParams.taskId to task.id)
                        )
                    )
                }
            }
        }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskToggleParams.taskId] ?: return
        val db = AppDatabase.getInstance(context)
        val task = db.taskDao().getTaskById(taskId) ?: return
        if (!task.active) return
        db.taskDao().toggleCompletion(taskId, ChecklistRepository.today())
        TaskListWidget.updateAll(context)
    }
}

object TaskToggleParams {
    val taskId = ActionParameters.Key<Long>("taskId")
}