package com.example.checklist

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checklist.data.AppDatabase
import com.example.checklist.data.ChecklistRepository
import com.example.checklist.ui.ChecklistScreen
import com.example.checklist.ui.ChecklistViewModel
import com.example.checklist.ui.StatsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(this)
        val repo = ChecklistRepository(db.taskDao())
        enableEdgeToEdge()
        setContent {
            ChecklistApp(repo)
        }
    }
}

@Composable
private fun ChecklistApp(repo: ChecklistRepository) {
    val context = LocalContext.current
    ChecklistTheme(context) {
        val viewModel: ChecklistViewModel = viewModel(
            factory = ChecklistViewModel.factory(repo)
        )
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }

        val lifecycleOwner = LocalLifecycleOwner.current
        androidx.compose.runtime.LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.addObserver(
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        viewModel.refresh()
                    }
                }
            )
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Checklist") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        label = { Text("Stats") }
                    )
                }
            }
        ) { padding ->
            val contentModifier = Modifier.padding(padding)
            when (selectedTab) {
                0 -> ChecklistScreen(viewModel, contentModifier)
                else -> StatsScreen(viewModel, contentModifier)
            }
        }
    }
}

@Composable
private fun ChecklistTheme(context: android.content.Context, content: @Composable () -> Unit) {
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    // ponytail: dynamic color requires Android 12+, else static schemes
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
