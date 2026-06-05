package com.example.sira.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sira.data.model.PlantStatus
import com.example.sira.data.repository.PlantRepository
import com.example.sira.di.ServiceLocator
import com.example.sira.ui.navigation.SiraRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de histórico. */
data class HistoryUiState(
    val isLoading: Boolean = true,   // carga inicial (muestra spinner centrado)
    val isRefreshing: Boolean = false, // refresco manual (indicador de pull-to-refresh)
    val readings: List<PlantStatus> = emptyList(),
    val errorMessage: String? = null
)

/**
 * El histórico se carga BAJO DEMANDA: una vez al abrir la pantalla y cada vez que
 * el usuario desliza para refrescar. No usa listener en tiempo real porque las
 * lecturas pasadas no cambian.
 */
class HistoryViewModel(
    private val plantRepository: PlantRepository,
    private val plantId: String
) : ViewModel() {

    /** Constructor usado por `viewModel()` (recibe el plantId vía SavedStateHandle). */
    constructor(savedStateHandle: SavedStateHandle) : this(
        ServiceLocator.plantRepository,
        checkNotNull(savedStateHandle.get<String>(SiraRoutes.PLANT_ARG)) { "Falta plantId" }
    )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load(initial = true)
    }

    /** Refresco manual (pull-to-refresh). */
    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        _uiState.update {
            if (initial) it.copy(isLoading = true, errorMessage = null)
            else it.copy(isRefreshing = true, errorMessage = null)
        }
        viewModelScope.launch {
            runCatching { plantRepository.getHistory(plantId, limit = 50) }
                .onSuccess { readings ->
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, readings = readings)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "No se pudo cargar el histórico."
                        )
                    }
                }
        }
    }
}
