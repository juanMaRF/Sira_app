package com.example.sira.data.repository

import com.example.sira.data.model.PlantStatus
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de datos en tiempo real de UNA planta concreta, identificada por su
 * código ([plantId]).
 *
 * La UI solo conoce esta interfaz. La implementa [MockPlantRepository] (datos
 * simulados) y [FirestorePlantRepository] (Cloud Firestore real).
 */
interface PlantRepository {
    /**
     * Flujo en TIEMPO REAL con el estado actual de la planta [plantId].
     * (1 documento que cambia seguido → conviene escucharlo en vivo.)
     */
    fun observePlantStatus(plantId: String): Flow<PlantStatus>

    /**
     * Carga BAJO DEMANDA las últimas [limit] lecturas históricas de [plantId],
     * ordenadas de la más antigua a la más reciente (para graficar izq→der).
     *
     * Es una consulta única (no un listener): el histórico es de solo-anexar y no
     * cambia una vez escrito, así que escucharlo en vivo solo gastaría lecturas.
     * La pantalla decide cuándo refrescar (al abrir y con "deslizar para refrescar").
     */
    suspend fun getHistory(plantId: String, limit: Int = 50): List<PlantStatus>
}
