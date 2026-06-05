package com.example.sira.data.repository

import android.content.Context
import com.example.sira.data.model.AuthUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación SIMULADA de autenticación.
 *
 * No contacta a Firebase: simplemente espera un instante y devuelve un usuario
 * ficticio, para que puedas recorrer el flujo Login -> Dashboard sin configurar
 * nada en la nube.
 */
class MockAuthRepository : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUser = _currentUser.asStateFlow()

    override suspend fun signInWithGoogle(activityContext: Context): Result<AuthUser> {
        delay(1_200) // Simula la latencia de red del login.
        val user = AuthUser(
            uid = "mock-uid-123",
            displayName = "Juan Manuel",
            email = "juanmarivera12@gmail.com",
            photoUrl = null
        )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        delay(800)
        val user = AuthUser(
            uid = "mock-uid-email",
            displayName = email.substringBefore("@"),
            email = email.trim(),
            photoUrl = null
        )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> {
        delay(800)
        val user = AuthUser(
            uid = "mock-uid-email",
            displayName = displayName.trim().ifEmpty { email.substringBefore("@") },
            email = email.trim(),
            photoUrl = null
        )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(600) // Simula el envío del correo.
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }
}
