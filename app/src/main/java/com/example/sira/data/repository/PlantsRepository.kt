package com.example.sira.data.repository

import com.example.sira.data.model.Plant
import kotlinx.coroutines.flow.Flow

/**
 * Gestiona la colección de plantas: listar las del usuario y registrar nuevas.
 *
 * (Distinto de [PlantRepository], que maneja los datos en tiempo real de UNA
 * planta ya registrada.)
 */
interface PlantsRepository {

    /** Flujo con las plantas cuyo `ownerUid` es [ownerUid]. */
    fun observeUserPlants(ownerUid: String): Flow<List<Plant>>

    /**
     * Registra una planta por su [code] (modelo HÍBRIDO):
     *  - si el documento existe y no tiene dueño → lo reclama para [ownerUid];
     *  - si ya es de [ownerUid] → lo deja como está (éxito idempotente);
     *  - si es de otro usuario → falla con [PlantAlreadyClaimedException];
     *  - si no existe → lo crea con [ownerUid] y [plantName].
     */
    suspend fun registerPlant(code: String, plantName: String, ownerUid: String): Result<Plant>

    /** Cambia el nombre visible de la planta [plantId]. */
    suspend fun renamePlant(plantId: String, newName: String): Result<Unit>

    /** Elimina la planta [plantId] junto con su histórico. */
    suspend fun deletePlant(plantId: String): Result<Unit>
}

/** La maceta ya está registrada por otro usuario. */
class PlantAlreadyClaimedException :
    Exception("Esta maceta ya está registrada por otra cuenta.")

/** El código está vacío o es inválido. */
class InvalidPlantCodeException :
    Exception("Ingresa un código de maceta válido.")

/** El nombre está vacío o es inválido. */
class InvalidPlantNameException :
    Exception("Ingresa un nombre válido.")
