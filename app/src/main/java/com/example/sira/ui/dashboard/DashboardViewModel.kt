package com.example.sira.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sira.data.model.PlantStatus
import com.example.sira.data.repository.PlantRepository
import com.example.sira.di.ServiceLocator
import com.example.sira.ui.navigation.SiraRoutes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Estado de la pantalla del dashboard. */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val plant: PlantStatus? = null
)

/**
 * ViewModel del dashboard de UNA planta. El `plantId` llega como argumento de
 * navegación a través del [SavedStateHandle].
 */
class DashboardViewModel(
    plantRepository: PlantRepository,
    plantId: String
) : ViewModel() {

    /**
     * Constructor que usa `viewModel()`: la factory de Navigation inyecta el
     * [SavedStateHandle] con los argumentos de la ruta.
     */
    constructor(savedStateHandle: SavedStateHandle) : this(
        ServiceLocator.plantRepository,
        checkNotNull(savedStateHandle.get<String>(SiraRoutes.PLANT_ARG)) { "Falta plantId" }
    )

    val uiState: StateFlow<DashboardUiState> = plantRepository.observePlantStatus(plantId)
        .map { DashboardUiState(isLoading = false, plant = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(isLoading = true, plant = null)
        )
}
