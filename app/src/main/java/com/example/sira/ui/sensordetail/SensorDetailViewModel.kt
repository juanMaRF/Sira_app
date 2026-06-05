package com.example.sira.ui.sensordetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sira.data.model.PlantStatus
import com.example.sira.data.repository.PlantRepository
import com.example.sira.di.ServiceLocator
import com.example.sira.ui.navigation.SiraRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado del detalle. Es agnóstico al sensor: expone la lectura actual (en vivo)
 * y el histórico (cargado una vez); la pantalla elige qué sensor mostrar.
 */
data class SensorDetailUiState(
    val isLoading: Boolean = true,
    val current: PlantStatus? = null,
    val history: List<PlantStatus> = emptyList()
)

class SensorDetailViewModel(
    plantRepository: PlantRepository,
    plantId: String
) : ViewModel() {

    /** Constructor usado por `viewModel()` (recibe el plantId vía SavedStateHandle). */
    constructor(savedStateHandle: SavedStateHandle) : this(
        ServiceLocator.plantRepository,
        checkNotNull(savedStateHandle.get<String>(SiraRoutes.PLANT_ARG)) { "Falta plantId" }
    )

    // Histórico para la mini-gráfica: consulta única al abrir la pantalla.
    private val history = MutableStateFlow<List<PlantStatus>>(emptyList())

    init {
        viewModelScope.launch {
            history.value = runCatching { plantRepository.getHistory(plantId, limit = 30) }
                .getOrDefault(emptyList())
        }
    }

    val uiState: StateFlow<SensorDetailUiState> = combine(
        plantRepository.observePlantStatus(plantId),  // estado actual en tiempo real
        history
    ) { current, history ->
        SensorDetailUiState(isLoading = false, current = current, history = history)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SensorDetailUiState(isLoading = true)
    )
}
