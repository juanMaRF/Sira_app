package com.example.sira.ui.plantlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sira.data.model.Plant
import com.example.sira.data.repository.AuthRepository
import com.example.sira.data.repository.PlantsRepository
import com.example.sira.di.ServiceLocator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla "Mis plantas". */
data class PlantListUiState(
    val isLoading: Boolean = true,
    val plants: List<Plant> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlantListViewModel(
    authRepository: AuthRepository,
    private val plantsRepository: PlantsRepository
) : ViewModel() {

    /** Constructor sin argumentos requerido por `viewModel()`. */
    constructor() : this(ServiceLocator.authRepository, ServiceLocator.plantsRepository)

    val uiState: StateFlow<PlantListUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(PlantListUiState(isLoading = false, plants = emptyList()))
            } else {
                plantsRepository.observeUserPlants(user.uid)
                    .map { PlantListUiState(isLoading = false, plants = it) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlantListUiState(isLoading = true)
        )

    // Mensaje transitorio para mostrar en un Snackbar (errores de borrar/renombrar).
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun renamePlant(plantId: String, newName: String) {
        viewModelScope.launch {
            plantsRepository.renamePlant(plantId, newName)
                .onFailure { _userMessage.value = it.message ?: "No se pudo renombrar la planta." }
        }
        // La lista se refresca sola: observeUserPlants emite el cambio.
    }

    fun deletePlant(plantId: String) {
        viewModelScope.launch {
            plantsRepository.deletePlant(plantId)
                .onSuccess { _userMessage.value = "Planta eliminada." }
                .onFailure { _userMessage.value = it.message ?: "No se pudo eliminar la planta." }
        }
    }

    fun consumeMessage() {
        _userMessage.value = null
    }
}
