package com.example.checklist.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChecklistScreen(
    viewModel: ChecklistViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskUi?>(null) }
    var deletingTask by remember { mutableStateOf<TaskUi?>(null) }

    // Konfetti: se dispara UNA vez al llegar al 100% del día (progress == 1f).
    // El flag evita re-disparos en recomposición; se resetea si baja del 100%.
    // konfetti-compose 2.0.5 construye los PartySystems con las parties de la primera
    // composición, así que el overlay se compone solo al dispararse (if showConfetti).
    var showConfetti by remember { mutableStateOf(false) }
    var confettiFired by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.progress) {
        if (uiState.progress == 1f && !confettiFired) {
            showConfetti = true
            confettiFired = true
            // Auto-oculta el overlay tras la duración del emisor (5000 ms).
            delay(5000)
            showConfetti = false
        } else if (uiState.progress < 1f) {
            showConfetti = false
            confettiFired = false
        }
    }

    Box(modifier = modifier) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar tarea")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Checklist", style = MaterialTheme.typography.headlineMedium)
                    Text(uiState.today, style = MaterialTheme.typography.bodyMedium)
                }
                if (uiState.streak > 0) {
                    StreakChip(uiState.streak)
                }
            }

            Spacer(Modifier.height(16.dp))

            DayProgressCard(uiState)

            Spacer(Modifier.height(16.dp))

            if (uiState.tasks.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleTask(task.id) },
                            onEdit = { editingTask = task },
                            onDelete = { deletingTask = task },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        speed = 30f,
                        maxSpeed = 50f,
                        damping = 0.9f,
                        spread = 360,
                        colors = listOf(
                            0xFF2E7D32.toInt(), 0xFF00796B.toInt(), 0xFFEF6C00.toInt(),
                            0xFF81C995.toInt(), 0xFF4DB6AC.toInt(), 0xFFFFD54F.toInt()
                        ),
                        position = Position.Relative(0.5, 0.3),
                        emitter = Emitter(duration = 5000, TimeUnit.MILLISECONDS).max(200)
                    )
                )
            )
        }
    }

    if (showAddDialog) {
        TaskDialog(
            title = "Nueva tarea",
            initial = "",
            onConfirm = { viewModel.addTask(it); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }

    editingTask?.let { task ->
        TaskDialog(
            title = "Editar tarea",
            initial = task.title,
            onConfirm = { viewModel.updateTask(task.id, it); editingTask = null },
            onDismiss = { editingTask = null }
        )
    }

    deletingTask?.let { task ->
        AlertDialog(
            onDismissRequest = { deletingTask = null },
            title = { Text("Eliminar tarea") },
            text = { Text("¿Eliminar \"${task.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task.id)
                    deletingTask = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deletingTask = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun StreakChip(streak: Int) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "$streak",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "días",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun DayProgressCard(uiState: ChecklistUiState) {
    val percentage = (uiState.progress * 100).toInt()
    val message = when {
        uiState.progress <= 0f -> "Empieza el día"
        uiState.progress < 1f -> "¡Sigue así!"
        else -> "¡Todo listo!"
    }
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier.size(180.dp),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.2f),
                    strokeWidth = 14.dp
                )
                Text(
                    "$percentage%",
                    style = MaterialTheme.typography.displaySmall,
                    color = contentColor
                )
            }
            Spacer(Modifier.height(16.dp))
            if (uiState.progress == 1f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Celebration,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.titleLarge,
                        color = contentColor
                    )
                }
            } else {
                Text(
                    message,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${uiState.completedTasks}/${uiState.totalTasks} completadas",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ponytail: lottie asset no disponible, icono estático
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(8.dp))
        Text("Sin tareas aún", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Toca + para crear tu primera tarea",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskUi,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor by animateColorAsState(
        targetValue = if (task.completedToday) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "taskTitleColor"
    )
    val cardColor by animateColorAsState(
        targetValue = if (task.completedToday) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "taskCardColor"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = cardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completedToday,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics { contentDescription = task.title }
            )
            Text(
                task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                textDecoration = if (task.completedToday) {
                    TextDecoration.LineThrough
                } else {
                    null
                }
            )
            IconButton(onClick = { onEdit() }) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = { onDelete() }) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@Composable
private fun TaskDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Título") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
