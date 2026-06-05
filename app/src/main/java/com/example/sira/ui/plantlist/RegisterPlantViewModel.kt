package com.example.sira.ui.plantlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sira.data.model.Plant
import com.example.sira.data.repository.AuthRepository
import com.example.sira.data.repository.PlantsRepository
import com.example.sira.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado del proceso de registro de una planta. */
sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data class Success(val plant: Plant) : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}

class RegisterPlantViewModel(
    authRepository: AuthRepository,
    private val plantsRepository: PlantsRepository
) : ViewModel() {

    /** Constructor sin argumentos requerido por `viewModel()`. */
    constructor() : this(ServiceLocator.authRepository, ServiceLocator.plantsRepository)

    // Mantiene el usuario actual disponible de forma síncrona al pulsar "registrar".
    private val currentUser = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val state = _state.asStateFlow()

    fun register(code: String, plantName: String) {
        if (_state.value == RegisterUiState.Loading) return
        val uid = currentUser.value?.uid
        if (uid == null) {
            _state.value = RegisterUiState.Error("Tu sesión expiró. Inicia sesión de nuevo.")
            return
        }
        _state.value = RegisterUiState.Loading
        viewModelScope.launch {
            plantsRepository.registerPlant(code, plantName, uid)
                .onSuccess { _state.value = RegisterUiState.Success(it) }
                .onFailure {
                    _state.value = RegisterUiState.Error(
                        it.message ?: "No se pudo registrar la planta."
                    )
                }
        }
    }

    fun consumeError() {
        if (_state.value is RegisterUiState.Error) _state.value = RegisterUiState.Idle
    }
}
