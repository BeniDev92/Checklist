package com.example.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checklist.data.AppDatabase
import com.example.checklist.data.ChecklistRepository
import com.example.checklist.ui.ChecklistScreen
import com.example.checklist.ui.ChecklistViewModel
import com.example.checklist.ui.StatsScreen
import com.example.checklist.ui.widget.TaskListWidget
import kotlinx.coroutines.launch

// Paleta propia verde/teal de hábitos. Sin dynamic color: mismo tema en todos los dispositivos.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6E5B0),
    onPrimaryContainer = Color(0xFF0B3B10),
    secondary = Color(0xFF00796B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00332E),
    tertiary = Color(0xFFEF6C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF422C00),
    background = Color(0xFFF7FAF5),
    onBackground = Color(0xFF181D18),
    surface = Color(0xFFF7FAF5),
    onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDEE5DA),
    onSurfaceVariant = Color(0xFF424940),
    surfaceDim = Color(0xFFD9DFD7),
    surfaceBright = Color(0xFFF7FAF5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5EF),
    surfaceContainer = Color(0xFFEBF0E9),
    surfaceContainerHigh = Color(0xFFE5EBE3),
    surfaceContainerHighest = Color(0xFFDFE5DD),
    outline = Color(0xFF73796F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C995),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB9F3BC),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF004F48),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF4A2700),
    tertiaryContainer = Color(0xFF7A4A00),
    onTertiaryContainer = Color(0xFFFFE0B2),
    background = Color(0xFF111512),
    onBackground = Color(0xFFDFE3DB),
    surface = Color(0xFF111512),
    onSurface = Color(0xFFDFE3DB),
    surfaceVariant = Color(0xFF41483F),
    onSurfaceVariant = Color(0xFFC2C9BE),
    surfaceDim = Color(0xFF111512),
    surfaceBright = Color(0xFF373B36),
    surfaceContainerLowest = Color(0xFF0B0F0C),
    surfaceContainerLow = Color(0xFF1A1E1A),
    surfaceContainer = Color(0xFF1E221E),
    surfaceContainerHigh = Color(0xFF282C28),
    surfaceContainerHighest = Color(0xFF333733),
    outline = Color(0xFF8C9387)
)

// Tipografía display redondeada y amigable (Baloo 2, variable; se usa en peso Bold).
// Solo los estilos display/headline usan la fuente; el resto mantiene la Material3 por defecto.
private val DisplayFont = FontFamily(Font(R.font.display_font, FontWeight.Bold))

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(this)
        val repo = ChecklistRepository(db.taskDao()) {
            lifecycleScope.launch { TaskListWidget.updateAll(applicationContext) }
        }
        enableEdgeToEdge()
        setContent {
            ChecklistApp(repo)
        }
    }
}

@Composable
private fun ChecklistApp(repo: ChecklistRepository) {
    ChecklistTheme {
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
private fun ChecklistTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
