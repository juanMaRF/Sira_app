package com.example.sira.data.repository

import android.content.Context
import com.example.sira.data.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de autenticación, independiente del proveedor.
 *
 * Implementaciones:
 *  - [MockAuthRepository]: simula el login con Google (para probar la UI ya).
 *  - [FirebaseAuthRepository]: login real con Google + Firebase Authentication.
 */
interface AuthRepository {

    /** Flujo con el usuario actual; `null` si no hay sesión iniciada. */
    val currentUser: Flow<AuthUser?>

    /**
     * Lanza el flujo de inicio de sesión con Google.
     * Recibe un [Context] de Activity porque Credential Manager lo necesita.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<AuthUser>

    /** Inicia sesión con correo y contraseña. */
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>

    /** Crea una cuenta nueva con correo y contraseña (y un nombre opcional). */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser>

    /** Envía un correo con un enlace para restablecer la contraseña. */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Cierra la sesión. */
    suspend fun signOut()
}
