package com.example.sira.data.repository

import com.example.sira.data.model.Plant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Implementación SIMULADA de [PlantsRepository], en memoria.
 *
 * Empieza sin plantas para que puedas probar el flujo de registro. Lo que
 * registres se mantiene mientras la app esté viva (no persiste).
 */
class MockPlantsRepository : PlantsRepository {

    private val plants = MutableStateFlow<List<Plant>>(emptyList())

    override fun observeUserPlants(ownerUid: String): Flow<List<Plant>> =
        plants.map { list -> list.filter { it.ownerUid == ownerUid } }

    override suspend fun registerPlant(
        code: String,
        plantName: String,
        ownerUid: String
    ): Result<Plant> = runCatching {
        delay(600) // Simula latencia de red.
        val normalizedCode = code.trim().uppercase()
        if (normalizedCode.isEmpty()) throw InvalidPlantCodeException()
        val name = plantName.trim().ifEmpty { "Mi planta" }

        val existing = plants.value.firstOrNull { it.id == normalizedCode }
        when {
            existing == null -> {
                val plant = Plant(normalizedCode, name, ownerUid)
                plants.value = plants.value + plant
                plant
            }
            existing.ownerUid == ownerUid -> existing
            else -> throw PlantAlreadyClaimedException()
        }
    }

    override suspend fun renamePlant(plantId: String, newName: String): Result<Unit> = runCatching {
        delay(300)
        val name = newName.trim()
        if (name.isEmpty()) throw InvalidPlantNameException()
        plants.value = plants.value.map {
            if (it.id == plantId) it.copy(plantName = name) else it
        }
    }

    override suspend fun deletePlant(plantId: String): Result<Unit> = runCatching {
        delay(300)
        plants.value = plants.value.filterNot { it.id == plantId }
    }
}
