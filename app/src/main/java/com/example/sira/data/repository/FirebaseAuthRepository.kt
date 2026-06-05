package com.example.sira.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.sira.data.model.AuthUser
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

/**
 * Implementación REAL: Google Sign-In (Credential Manager) + Firebase Authentication.
 *
 * ⚠️ Aún NO está en uso. Para activarla:
 *   1. Coloca `google-services.json` en `app/` y descomenta el plugin google-services.
 *   2. Pasa tu Web Client ID (lo da Firebase) en [webClientId].
 *   3. En [com.example.sira.di.ServiceLocator] cambia `MockAuthRepository()`
 *      por `FirebaseAuthRepository(webClientId = "TU_WEB_CLIENT_ID.apps.googleusercontent.com")`.
 */
class FirebaseAuthRepository(
    private val webClientId: String,
    private val auth: FirebaseAuth = Firebase.auth
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(activityContext: Context): Result<AuthUser> = runCatching {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(activityContext)
        val response = credentialManager.getCredential(activityContext, request)

        val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)

        val authResult = auth.signInWithCredential(firebaseCredential).await()
        authResult.user?.toAuthUser()
            ?: error("Firebase no devolvió un usuario tras el inicio de sesión.")
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> = try {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user?.toAuthUser()
            ?: error("Firebase no devolvió un usuario.")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(Exception(mapAuthError(e)))
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> = try {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val firebaseUser = result.user ?: error("Firebase no devolvió un usuario.")
        // Guarda el nombre en el perfil si se proporcionó.
        val name = displayName.trim()
        if (name.isNotEmpty()) {
            val profile = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            firebaseUser.updateProfile(profile).await()
        }
        Result.success(firebaseUser.toAuthUser())
    } catch (e: Exception) {
        Result.failure(Exception(mapAuthError(e)))
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email.trim()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(mapAuthError(e)))
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    /** Traduce los errores más comunes de Firebase Auth a mensajes claros en español. */
    private fun mapAuthError(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException ->
            "La contraseña debe tener al menos 6 caracteres."
        is FirebaseAuthInvalidCredentialsException ->
            "Correo o contraseña incorrectos."
        is FirebaseAuthUserCollisionException ->
            "Ya existe una cuenta con ese correo."
        is FirebaseAuthInvalidUserException ->
            "No existe una cuenta con ese correo."
        else -> e.message ?: "No se pudo completar la operación. Inténtalo de nuevo."
    }

    private fun com.google.firebase.auth.FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString()
    )
}
