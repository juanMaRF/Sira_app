package com.example.sira.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sira.data.model.AuthUser
import com.example.sira.data.repository.AuthRepository
import com.example.sira.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado del proceso de inicio de sesión que observa la pantalla de Login. */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/** Estado del diálogo de "olvidé mi contraseña" (independiente del login). */
sealed interface ResetUiState {
    data object Idle : ResetUiState
    data object Sending : ResetUiState
    data object Sent : ResetUiState
    data class Error(val message: String) : ResetUiState
}

/**
 * ViewModel de autenticación. Usa un constructor con valor por defecto desde el
 * [ServiceLocator], de modo que `viewModel()` puede crearlo sin un Factory.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Constructor sin argumentos requerido por `viewModel()`. Delega en el
     * [ServiceLocator]; el constructor con parámetro queda disponible para tests.
     */
    constructor() : this(ServiceLocator.authRepository)

    /** Usuario actual; `null` significa que no hay sesión. */
    val currentUser: StateFlow<AuthUser?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _resetState = MutableStateFlow<ResetUiState>(ResetUiState.Idle)
    val resetState: StateFlow<ResetUiState> = _resetState.asStateFlow()

    fun signInWithGoogle(activityContext: Context) = runAuth {
        authRepository.signInWithGoogle(activityContext)
    }

    fun signInWithEmail(email: String, password: String) = runAuth {
        authRepository.signInWithEmail(email, password)
    }

    fun registerWithEmail(email: String, password: String, displayName: String) = runAuth {
        authRepository.registerWithEmail(email, password, displayName)
    }

    /** Ejecuta una operación de autenticación gestionando Loading/Success/Error. */
    private fun runAuth(block: suspend () -> Result<AuthUser>) {
        if (_uiState.value == AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            block()
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure {
                    _uiState.value = AuthUiState.Error(
                        it.message ?: "No se pudo completar. Inténtalo de nuevo."
                    )
                }
        }
    }

    fun sendPasswordReset(email: String) {
        if (_resetState.value == ResetUiState.Sending) return
        if (email.isBlank()) {
            _resetState.value = ResetUiState.Error("Ingresa tu correo electrónico.")
            return
        }
        _resetState.value = ResetUiState.Sending
        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
                .onSuccess { _resetState.value = ResetUiState.Sent }
                .onFailure {
                    _resetState.value = ResetUiState.Error(
                        it.message ?: "No se pudo enviar el correo. Inténtalo de nuevo."
                    )
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState.Idle
        }
    }

    /** Limpia el mensaje de error tras mostrarlo. */
    fun consumeError() {
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.Idle
    }

    /** Reinicia el estado del diálogo de restablecimiento al cerrarlo. */
    fun consumeResetState() {
        _resetState.value = ResetUiState.Idle
    }
}
