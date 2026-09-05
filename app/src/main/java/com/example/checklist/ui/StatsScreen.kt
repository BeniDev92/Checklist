package com.example.checklist.ui

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    viewModel: ChecklistViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val dailySeries by viewModel.dailySeries.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (dailySeries.any { it.count > 0 }) {
            DailyChartCard(dailySeries)
            Spacer(Modifier.height(16.dp))
        }

        if (uiState.streak > 0) {
            StreakCard(uiState.streak)
            Spacer(Modifier.height(16.dp))
        }

        if (stats.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Sin datos aún",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats, key = { it.id }) { stat ->
                    StatCard(stat)
                }
            }
        }
    }
}

@Composable
private fun DailyChartCard(series: List<DailyPoint>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Gráfico de tareas completadas en los últimos 14 días. " +
                    series.joinToString("; ") { "${it.date}: ${it.count}" }
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Completados — últimos 14 días",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))
            DailyChart(series)
        }
    }
}

@Composable
private fun DailyChart(series: List<DailyPoint>) {
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val dayFormatter = DateTimeFormatter.ofPattern("dd/MM")
    // El eje X etiqueta solo índices impares (1,3,...,13, terminando en "hoy"),
    // es decir una etiqueta cada 2 puntos. Controlado por ItemPlacer (no strings vacíos).
    val xItemPlacer = remember(series) {
        object : HorizontalAxis.ItemPlacer {
            private val delegate = HorizontalAxis.ItemPlacer.aligned()
            override fun getShiftExtremeLines(context: CartesianDrawingContext): Boolean =
                delegate.getShiftExtremeLines(context)
            override fun getFirstLabelValue(
                context: CartesianMeasuringContext,
                maxLabelWidth: Float,
            ): Double? =
                delegate.getFirstLabelValue(context, maxLabelWidth)?.takeIf { it.toInt() % 2 == 1 }
            override fun getLastLabelValue(
                context: CartesianMeasuringContext,
                maxLabelWidth: Float,
            ): Double? =
                delegate.getLastLabelValue(context, maxLabelWidth)?.takeIf { it.toInt() % 2 == 1 }
            override fun getLabelValues(
                context: CartesianDrawingContext,
                visibleXRange: ClosedFloatingPointRange<Double>,
                fullXRange: ClosedFloatingPointRange<Double>,
                maxLabelWidth: Float,
            ): List<Double> =
                delegate.getLabelValues(context, visibleXRange, fullXRange, maxLabelWidth)
                    .filter { it.toInt() % 2 == 1 }
            override fun getWidthMeasurementLabelValues(
                context: CartesianMeasuringContext,
                layerDimensions: CartesianLayerDimensions,
                fullXRange: ClosedFloatingPointRange<Double>,
            ): List<Double> =
                delegate.getWidthMeasurementLabelValues(context, layerDimensions, fullXRange)
            override fun getHeightMeasurementLabelValues(
                context: CartesianMeasuringContext,
                layerDimensions: CartesianLayerDimensions,
                fullXRange: ClosedFloatingPointRange<Double>,
                maxLabelWidth: Float,
            ): List<Double> =
                delegate.getHeightMeasurementLabelValues(context, layerDimensions, fullXRange, maxLabelWidth)
            override fun getLineValues(
                context: CartesianDrawingContext,
                visibleXRange: ClosedFloatingPointRange<Double>,
                fullXRange: ClosedFloatingPointRange<Double>,
                maxLabelWidth: Float,
            ): List<Double>? =
                delegate.getLineValues(context, visibleXRange, fullXRange, maxLabelWidth)
            override fun getStartLayerMargin(
                context: CartesianMeasuringContext,
                layerDimensions: CartesianLayerDimensions,
                tickThickness: Float,
                maxLabelWidth: Float,
            ): Float = delegate.getStartLayerMargin(context, layerDimensions, tickThickness, maxLabelWidth)
            override fun getEndLayerMargin(
                context: CartesianMeasuringContext,
                layerDimensions: CartesianLayerDimensions,
                tickThickness: Float,
                maxLabelWidth: Float,
            ): Float = delegate.getEndLayerMargin(context, layerDimensions, tickThickness, maxLabelWidth)
        }
    }
    // Solo se invoca para los valores que ItemPlacer eligió etiquetar (índices impares);
    // nunca devuelve una string vacía.
    val xLabelFormatter = remember(series) {
        object : CartesianValueFormatter {
            override fun format(
                context: CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: Axis.Position.Vertical?
            ): CharSequence {
                val index = value.toInt().coerceIn(0, series.lastIndex)
                return dayFormatter.format(LocalDate.parse(series[index].date))
            }
        }
    }
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(series) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = series.indices.toList(),
                    y = series.map { it.count }
                )
            }
        }
    }
    val lineProvider = remember(primaryArgb) {
        LineCartesianLayer.LineProvider.series(
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(primaryArgb)),
                stroke = LineCartesianLayer.LineStroke.Continuous(
                    thicknessDp = 2f,
                    cap = Paint.Cap.ROUND
                ),
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        component = ShapeComponent(fill = Fill(primaryArgb), shape = CorneredShape.Pill),
                        sizeDp = 6f
                    )
                )
            )
        )
    }
    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(lineProvider = lineProvider),
        startAxis = VerticalAxis.rememberStart(),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = xLabelFormatter,
            itemPlacer = xItemPlacer
        )
    )
    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
private fun StreakCard(streak: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$streak",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "días de racha",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun StatCard(stat: TaskStat) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stat.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(stat.rate * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { stat.rate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Heatmap7(stat.recent7Days)
        }
    }
}

@Composable
private fun Heatmap7(days: List<Boolean>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Últimos 7 días: completado ${days.count { it }} de ${days.size}"
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Índice 0 (hace 6 días) a la izquierda, índice 6 (hoy) a la derecha.
        days.forEach { completed ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .size(height = 14.dp, width = 14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val labels = listOf("H-6", "H-5", "H-4", "H-3", "H-2", "H-1", "Hoy")
        labels.forEach { label ->
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
