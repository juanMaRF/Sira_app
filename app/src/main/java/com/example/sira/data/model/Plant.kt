package com.example.sira.data.model

/**
 * Una planta/maceta registrada por un usuario.
 *
 * @param id        Código único de la maceta (= ID del documento en Firestore).
 * @param plantName Nombre amigable que le puso el usuario.
 * @param ownerUid  UID del usuario dueño (vincula la planta con la cuenta).
 */
data class Plant(
    val id: String,
    val plantName: String,
    val ownerUid: String
)
