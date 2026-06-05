package com.example.sira.data.model

/**
 * Representa al usuario autenticado de forma independiente del proveedor.
 *
 * Mantenerlo desacoplado de FirebaseUser permite que la UI y los ViewModels
 * no dependan directamente de Firebase, facilitando los datos simulados.
 */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)
