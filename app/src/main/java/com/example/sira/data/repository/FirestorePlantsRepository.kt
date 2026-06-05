package com.example.sira.data.repository

import com.example.sira.data.model.Plant
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementación REAL de [PlantsRepository] sobre Cloud Firestore.
 *
 * Estructura: colección `plants`, un documento por maceta (ID = código).
 * Campo clave `ownerUid` para saber de quién es cada planta.
 */
class FirestorePlantsRepository(
    private val collectionName: String = "plants",
    private val firestore: FirebaseFirestore = Firebase.firestore
) : PlantsRepository {

    override fun observeUserPlants(ownerUid: String): Flow<List<Plant>> = callbackFlow {
        val registration = firestore.collection(collectionName)
            .whereEqualTo("ownerUid", ownerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val plants = snapshot.documents.map { doc ->
                        Plant(
                            id = doc.id,
                            plantName = doc.getString("plantName") ?: "Mi planta",
                            ownerUid = doc.getString("ownerUid") ?: ownerUid
                        )
                    }
                    trySend(plants)
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun registerPlant(
        code: String,
        plantName: String,
        ownerUid: String
    ): Result<Plant> = runCatching {
        val normalizedCode = code.trim().uppercase()
        if (normalizedCode.isEmpty()) throw InvalidPlantCodeException()
        val name = plantName.trim().ifEmpty { "Mi planta" }

        val docRef = firestore.collection(collectionName).document(normalizedCode)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            val existingOwner = snapshot.getString("ownerUid")
            when {
                // Sin dueño → reclamar.
                existingOwner.isNullOrEmpty() ->
                    docRef.update(mapOf("ownerUid" to ownerUid, "plantName" to name)).await()
                // Ya es mía → idempotente.
                existingOwner == ownerUid -> Unit
                // De otro → error.
                else -> throw PlantAlreadyClaimedException()
            }
        } else {
            // No existe → crear con valores iniciales (la ESP32 los sobrescribirá).
            docRef.set(newPlantData(name, ownerUid)).await()
        }

        Plant(id = normalizedCode, plantName = name, ownerUid = ownerUid)
    }

    override suspend fun renamePlant(plantId: String, newName: String): Result<Unit> = runCatching {
        val name = newName.trim()
        if (name.isEmpty()) throw InvalidPlantNameException()
        firestore.collection(collectionName).document(plantId)
            .update("plantName", name)
            .await()
    }

    override suspend fun deletePlant(plantId: String): Result<Unit> = runCatching {
        val plantRef = firestore.collection(collectionName).document(plantId)
        // Firestore NO borra subcolecciones en cascada: limpiamos `history` por lotes.
        val historyRef = plantRef.collection("history")
        while (true) {
            val page = historyRef.limit(400).get().await()
            if (page.isEmpty) break
            val batch = firestore.batch()
            page.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (page.size() < 400) break
        }
        plantRef.delete().await()
    }

    private fun newPlantData(name: String, ownerUid: String) = mapOf(
        "ownerUid" to ownerUid,
        "plantName" to name,
        "soilMoisture" to 0L,
        "temperature" to 0.0,
        "lightLevel" to 0L,
        "waterLevel" to 0L,
        "lastUpdated" to System.currentTimeMillis()
    )
}
