package com.example.sira.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sira.data.model.PlantStatus
import com.example.sira.data.model.SensorType
import com.example.sira.ui.common.visual
import com.example.sira.ui.theme.LeafGreen
import kotlinx.coroutines.delay

/**
 * Contenido del panel (se muestra dentro del Scaffold de HomeScreen).
 *
 * @param onSensorClick navega al detalle del sensor tocado.
 */
@Composable
fun DashboardScreen(
    onSensorClick: (SensorType) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        state.isLoading || state.plant == null ->
            LoadingState(modifier.fillMaxSize().padding(contentPadding))
        else -> PlantContent(
            plant = state.plant!!,
            contentPadding = contentPadding,
            onSensorClick = onSensorClick,
            modifier = modifier
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Conectando con tu maceta…")
        }
    }
}

@Composable
private fun PlantContent(
    plant: PlantStatus,
    contentPadding: PaddingValues,
    onSensorClick: (SensorType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PlantHeader(plant)
        }
        items(SensorType.entries) { sensor ->
            val visual = sensor.visual()
            SensorCard(
                title = sensor.displayName,
                value = sensor.format(plant),
                icon = visual.icon,
                accent = visual.accent,
                progress = sensor.progressFrom(plant),
                onClick = { onSensorClick(sensor) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlantHeader(plant: PlantStatus) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = plant.plantName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        LiveStatusChip(lastUpdated = plant.lastUpdated)
    }
}

/**
 * Chip de "tiempo real": punto verde + tiempo relativo que se actualiza cada
 * segundo. Si la maceta lleva más de 2 minutos sin reportar, se muestra en gris
 * como "Sin conexión reciente" (más honesto que decir "en vivo" siempre).
 */
@Composable
private fun LiveStatusChip(lastUpdated: Long) {
    // Reloj que avanza cada segundo para refrescar el texto relativo.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastUpdated) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val elapsedSec = ((now - lastUpdated) / 1000).coerceAtLeast(0)
    val isFresh = elapsedSec < 120
    val dotColor = if (isFresh) LeafGreen else MaterialTheme.colorScheme.onSurfaceVariant
    val label = if (isFresh) "En vivo" else "Sin conexión reciente"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(dotColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "$label · actualizado ${relativeTime(elapsedSec)}",
            style = MaterialTheme.typography.labelMedium,
            color = if (isFresh) LeafGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Convierte segundos transcurridos en un texto tipo "hace 5 s" / "hace 3 min". */
private fun relativeTime(elapsedSec: Long): String = when {
    elapsedSec < 5 -> "ahora"
    elapsedSec < 60 -> "hace ${elapsedSec} s"
    elapsedSec < 3_600 -> "hace ${elapsedSec / 60} min"
    elapsedSec < 86_400 -> "hace ${elapsedSec / 3_600} h"
    else -> "hace ${elapsedSec / 86_400} d"
}
