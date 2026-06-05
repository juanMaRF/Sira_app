package com.example.sira.data.repository

import com.example.sira.data.model.PlantStatus
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementación REAL respaldada por Cloud Firestore.
 *
 * Cada planta es un documento en la colección [collectionName] cuyo ID es el
 * código de la maceta. El histórico vive en su subcolección `history`.
 *
 * Espera documentos con los campos:
 *   soilMoisture (Int), temperature (Double/Float), lightLevel (Int),
 *   waterLevel (Int), plantName (String), lastUpdated (Long).
 */
class FirestorePlantRepository(
    private val collectionName: String = "plants",
    private val firestore: FirebaseFirestore = Firebase.firestore
) : PlantRepository {

    override fun observePlantStatus(plantId: String): Flow<PlantStatus> = callbackFlow {
        val docRef = firestore.collection(collectionName).document(plantId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toPlantStatus())
            }
        }
        // Se desuscribe cuando el flujo se cancela (p. ej. al cerrar la pantalla).
        awaitClose { registration.remove() }
    }

    override suspend fun getHistory(plantId: String, limit: Int): List<PlantStatus> {
        // Subcolección de lecturas: plants/<plantId>/history. Consulta única.
        val snapshot = firestore.collection(collectionName).document(plantId)
            .collection("history")
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        // Llegan de más reciente a más antigua; las invertimos para graficar.
        return snapshot.documents
            .map { it.toPlantStatus() }
            .sortedBy { it.lastUpdated }
    }

    private fun DocumentSnapshot.toPlantStatus() = PlantStatus(
        plantName = getString("plantName") ?: "Mi planta",
        soilMoisture = (getLong("soilMoisture") ?: 0L).toInt(),
        temperature = (getDouble("temperature") ?: 0.0).toFloat(),
        lightLevel = (getLong("lightLevel") ?: 0L).toInt(),
        waterLevel = (getLong("waterLevel") ?: 0L).toInt(),
        lastUpdated = getLong("lastUpdated") ?: System.currentTimeMillis()
    )
}
